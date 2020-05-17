package rtk_contest;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import mbproto.MessageBrokerGrpc;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server that manages startup/shutdown of a {@code Greeter} server.
 */
public class MbProtoServer {
    private static final Logger logger = Logger.getLogger(MbProtoServer.class.getName());

    private Server server;

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 53401;
        server = ServerBuilder.forPort(port)
                .addService(new MbProtoServiceImpl())
                .build()
                .start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    MbProtoServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    /**
     * Main launches the server from the command line.
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        final MbProtoServer server = new MbProtoServer();
        server.start();
        server.blockUntilShutdown();
    }

    static class MbProtoServiceImpl extends MessageBrokerGrpc.MessageBrokerImplBase {

        private static final int THREADS_NUM_TO_PROCEED_INBOX = 7;

        /**
         * Массив входящих сообщений
         */
        private final LinkedBlockingQueue<InboxRequestInfo> inbox = new LinkedBlockingQueue<>();

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
        private final ExecutorService inboxProcessingThreadPool;

        public MbProtoServiceImpl() {
            // Создаём тредпул и сразу инициализируем все потоки.
            // Потоки будут исполняться бесконечно и жать сообщения из очереди inbox.
            inboxProcessingThreadPool = Executors.newFixedThreadPool(THREADS_NUM_TO_PROCEED_INBOX);
            for (int i = 0; i < THREADS_NUM_TO_PROCEED_INBOX; i++) {
                inboxProcessingThreadPool.submit(new InboxProcessor(inbox, addressing, consumers));
            }
        }

        public StreamObserver<Mbproto.ProduceRequest> produce(
                StreamObserver<Mbproto.ProduceResponse> responseObserver) {

            return new StreamObserver<>() {

                @Override
                public void onNext(Mbproto.ProduceRequest request) {
                    inbox.add(new InboxRequestInfo(request.getKey(), request.getPayload()));
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.WARNING, "Encountered error: produce ", t);
                }

                @Override
                public void onCompleted() {
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
                    logger.log(Level.WARNING, "Encountered error: consume ", t);
                    consumers.remove(thisConsumer);
                    for (Map.Entry<String, ConcurrentSkipListSet<ConsumerData>> entry : addressing.entrySet()) {
                        entry.getValue().remove(thisConsumer);
                    }
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

        /**
         * Содержит входящее сообщение с ключём и значением
         */
        static class InboxRequestInfo {
            String key;
            ByteString value;

            public InboxRequestInfo(String key, ByteString value) {
                this.key = key;
                this.value = value;
            }
        }

        /**
         * Класс описывает потребителя - его стрим и шаблоны на которые он подписан.
         */
        static class ConsumerData implements Comparable<ConsumerData> {

            static final AtomicInteger CONSUMER_ENUMERATOR = new AtomicInteger();
            /**
             * порядковый номер потребителя
             */
            private final int num = CONSUMER_ENUMERATOR.incrementAndGet();
            /**
             * Стрим для отправки данных консюмеру
             */
            private final StreamObserver<Mbproto.ConsumeResponse> responseObserver;
            /**
             * Набор шаблонов консюмера
             */
            private final ConcurrentSkipListSet<String> templates = new ConcurrentSkipListSet<>();

            public ConsumerData(StreamObserver<Mbproto.ConsumeResponse> responseObserver) {
                this.responseObserver = responseObserver;
            }

            void addTemplate(String template) {
                templates.add(template);
            }

            void removeTemplate(String template) {
                templates.remove(template);
            }

            public ConcurrentSkipListSet<String> getTemplates() {
                return templates;
            }

            @Override
            public boolean equals(Object o) {
                ConsumerData that = (ConsumerData) o;
                return num == that.num;
            }

            @Override
            public int hashCode() {
                return num;
            }

            @Override
            public int compareTo(ConsumerData that) {
                return that.num - num;
            }
        }

        static class InboxProcessor implements Runnable {

            static final AtomicInteger WORKER_ENUMERATOR = new AtomicInteger();

            private final LinkedBlockingQueue<InboxRequestInfo> inbox;
            private final int worker_id = WORKER_ENUMERATOR.incrementAndGet();
            private final ConcurrentHashMap<String, ConcurrentSkipListSet<ConsumerData>> addressing;
            private final ConcurrentSkipListSet<ConsumerData> consumers;

            public InboxProcessor(LinkedBlockingQueue<InboxRequestInfo> inbox,
                                  ConcurrentHashMap<String, ConcurrentSkipListSet<ConsumerData>> addressing,
                                  ConcurrentSkipListSet<ConsumerData> consumers) {
                this.inbox = inbox;
                this.addressing = addressing;
                this.consumers = consumers;
            }

            @Override
            public void run() {
                InboxRequestInfo requestInfo;
                while (true) {
                    try {
                        requestInfo = inbox.take();
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "worker shutdown: " + worker_id, e);
                        break;
                    }
                    ConcurrentSkipListSet<ConsumerData> consumersToSend =
                            addressing.computeIfAbsent(requestInfo.key, key -> {
                                ConcurrentSkipListSet<ConsumerData> set = new ConcurrentSkipListSet<>();
                                // если такого ключа ещё не было - следует добавить в него консьюмеров
                                for (ConsumerData consumer : consumers) {
                                    for (String template : consumer.getTemplates()) {
                                        TemplateMatcher templateMatcher = new TemplateMatcher(template);
                                        if (templateMatcher.matchTo(key)) {
                                            set.add(consumer);
                                            // одного вхождения достаточно
                                            break;
                                        }
                                    }
                                }
                                return set;
                            });
                    Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                            .setKey(requestInfo.key)
                            .setPayload(requestInfo.value)
                            .build();
                    for (ConsumerData consumer : consumersToSend) {
                        consumer.responseObserver.onNext(response);
                    }
                }
            }
        }

    }
}
