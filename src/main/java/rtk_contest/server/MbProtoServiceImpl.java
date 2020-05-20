package rtk_contest.server;

import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import mbproto.MessageBrokerGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MbProtoServiceImpl extends MessageBrokerGrpc.MessageBrokerImplBase {

    private final Logger LOGGER = LoggerFactory.getLogger(MbProtoServiceImpl.class);

    private static final int THREADS_NUM_TO_PROCEED_INBOX = 4;

    /**
     * Список всех потребителей.
     */
    private final ConcurrentSkipListSet<ConsumerData> consumers = new ConcurrentSkipListSet<>();

    /**
     * Пул потоков для обработки входящих сообщений
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ExecutorService executorService;

    private final BlockingQueue<InboxRequestInfo> queue = new ArrayBlockingQueue<>(100_000);

    public MbProtoServiceImpl() {
        executorService = Executors.newFixedThreadPool(THREADS_NUM_TO_PROCEED_INBOX);
        for (int i = 0; i < THREADS_NUM_TO_PROCEED_INBOX; i++) {
            executorService.submit(new IncomeMessageProcessor(consumers, queue));
        }
    }

    public StreamObserver<Mbproto.ProduceRequest> produce(
            StreamObserver<Mbproto.ProduceResponse> responseObserver) {

        return new StreamObserver<>() {

            @Override
            public void onNext(Mbproto.ProduceRequest request) {
                // равномерно распределяем нагрузку по тредам
                queue.add(new InboxRequestInfo(request.getKey(), request.getPayload()));
            }

            @Override
            public void onError(Throwable t) {
                // LOGGER.error("Продюсер закрылся с ошибкой", t);
            }

            @Override
            public void onCompleted() {
                // пустой ответ
                try {
                    responseObserver.onNext(Mbproto.ProduceResponse.newBuilder().build());
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOGGER.error("Продюсер закрылся с ошибкой (onCompleted)", e);
                }
            }
        };
    }

    public StreamObserver<Mbproto.ConsumeRequest> consume(StreamObserver<Mbproto.ConsumeResponse> responseObserver) {

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
                for (int i = 0; i < consumeRequest.getKeysCount(); i++) {
                    // 1. добавляем шаблоны консьюмеру
                    String template = consumeRequest.getKeys(i);

                    if (consumeRequest.getActionValue() == 0) { // SUBSCRIBE
                        thisConsumer.addTemplate(template);
                    } else {
                        thisConsumer.removeTemplate(template);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                consumers.remove(thisConsumer);
                //LOGGER.error("Консьюмер закрылся с ошибкой", t);
            }

            @Override
            public void onCompleted() {
                try {
                    consumers.remove(thisConsumer);
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOGGER.error("Консьюмер закрылся с ошибкой (onCompleted)", e);
                }
            }
        };
    }

}
