package rtk_contest.server;

import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import mbproto.MessageBrokerGrpc;
import rtk_contest.templating.TemplateMatcher;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicLong;

public class MbProtoServiceImpl extends MessageBrokerGrpc.MessageBrokerImplBase {

    private static final int THREADS_NUM_TO_PROCEED_INBOX = 4;

    private AtomicLong threadPointer = new AtomicLong();

    /**
     * Каждому ключу соответствует набор консьюмеров - для быстрого поиска при отправке.
     */
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<ConsumerData>> addressing = new ConcurrentHashMap<>();

    /**
     * Список всех потребителей.
     */
    private final ConcurrentSkipListSet<ConsumerData> consumers = new ConcurrentSkipListSet<>();

    /**
     * Пул потоков для обработки входящих сообщений
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final IncomeMessageProcessor[] threads;

    public MbProtoServiceImpl() {
        threads = new IncomeMessageProcessor[THREADS_NUM_TO_PROCEED_INBOX];
        for (int i = 0; i < THREADS_NUM_TO_PROCEED_INBOX; i++) {
            IncomeMessageProcessor thread = new IncomeMessageProcessor(addressing, consumers);
            thread.start();
            threads[i] = thread;
        }
    }

    public StreamObserver<Mbproto.ProduceRequest> produce(
            StreamObserver<Mbproto.ProduceResponse> responseObserver) {

        return new StreamObserver<>() {

            @Override
            public void onNext(Mbproto.ProduceRequest request) {
                // равномерно распределяем нагрузку по тредам
                int l = (int) (threadPointer.incrementAndGet() % THREADS_NUM_TO_PROCEED_INBOX);
                threads[l].addMessage(new InboxRequestInfo(request.getKey(), request.getPayload()));
            }

            @Override
            public void onError(Throwable t) {
                // todo норм логгер logger.log(Level.WARNING, "Encountered error: produce ", t);
            }

            @Override
            public void onCompleted() {
                // пустой ответ
                responseObserver.onNext(Mbproto.ProduceResponse.newBuilder().build());
                responseObserver.onCompleted();
            }
        };
    }

    public StreamObserver<Mbproto.ConsumeRequest> consume(
            StreamObserver<Mbproto.ConsumeResponse> responseObserver) {

        // добавить консьюмера
        ConsumerData thisConsumer = new ConsumerData(responseObserver);
        // добавим консьюмера в список
        consumers.add(thisConsumer);

        return new StreamObserver<>() {

            /**
             * Попробуем сделать синхронно, надеемся что вызывают не очень часто
             */
            @Override
            public void onNext(Mbproto.ConsumeRequest consumeRequest) {
                if (consumeRequest.getActionValue() == 0) { // SUBSCRIBE
                    int keysCount = consumeRequest.getKeysCount();

                    for (int i = 0; i < keysCount; i++) {
                        // 1. добавляем шаблоны консьюмеру
                        String template = consumeRequest.getKeys(i);
                        thisConsumer.addTemplate(template);
                        // 2. проверяем все ключи по шаблону,
                        // если ключ удовлетворяет шаблону то добавляем консьюмера в список отправки по этому ключу
                        TemplateMatcher templateMatcher = new TemplateMatcher(template);
                        for (Map.Entry<String, ConcurrentSkipListSet<ConsumerData>> entry : addressing.entrySet()) {
                            if (templateMatcher.matchTo(entry.getKey())) {
                                entry.getValue().add(thisConsumer);
                            }
                        }
                    }
                } else if (consumeRequest.getActionValue() == 1) { // UNSUBSCRIBE

                    // С удалением всё сложнее - нужно исключить консьюмера из некоторых ключей.
                    // Для этого составляем полный список ключей "до" удаления шаблонов
                    // и полный список ключей "после" удаления шаблонов.
                    // Разница между ними - это ключи по которым больше не следует отправлять сообщения.
                    // Из этих ключей следует исключить наш консьюмер в структуре addressing.

                    ConcurrentHashMap.KeySetView<String, ConcurrentSkipListSet<ConsumerData>> keySet = addressing.keySet();
                    Set<String> matchedBefore = getMatchedKeys(keySet, thisConsumer.getTemplates());
                    int keysCount = consumeRequest.getKeysCount();
                    for (int i = 0; i < keysCount; i++) {
                        // 1. удаляем шаблоны из консьюмера
                        String template = consumeRequest.getKeys(i);
                        thisConsumer.removeTemplate(template);
                    }
                    Set<String> matchedAfter = getMatchedKeys(keySet, thisConsumer.getTemplates());
                    matchedBefore.removeAll(matchedAfter);
                    for (Map.Entry<String, ConcurrentSkipListSet<ConsumerData>> entry : addressing.entrySet()) {
                        if (matchedBefore.contains(entry.getKey())) {
                            entry.getValue().remove(thisConsumer);
                        }
                    }
                }
            }

            private Set<String> getMatchedKeys(ConcurrentHashMap.KeySetView<String, ConcurrentSkipListSet<ConsumerData>> keySet,
                                               Set<String> templates) {
                Set<String> matched = new HashSet<>();
                for (String template : templates) {
                    TemplateMatcher templateMatcher = new TemplateMatcher(template);
                    for (String key : keySet) {
                        if (templateMatcher.matchTo(key)) {
                            matched.add(key);
                        }
                    }
                }
                return matched;
            }

            @Override
            public void onError(Throwable t) {
                //logger.log(Level.WARNING, "Encountered error: consume ", t);
//                consumers.remove(thisConsumer);
//                for (Map.Entry<String, ConcurrentSkipListSet<ConsumerData>> entry : addressing.entrySet()) {
//                    entry.getValue().remove(thisConsumer);
//                }
            }

            @Override
            public void onCompleted() {
                consumers.remove(thisConsumer);
                for (Map.Entry<String, ConcurrentSkipListSet<ConsumerData>> entry : addressing.entrySet()) {
                    entry.getValue().remove(thisConsumer);
                }
                responseObserver.onCompleted();
            }
        };
    }

}
