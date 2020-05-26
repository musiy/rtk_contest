package rtk_contest.server;

import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import mbproto.MessageBrokerGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MbProtoServiceImpl extends MessageBrokerGrpc.MessageBrokerImplBase {

    private final Logger LOGGER = LoggerFactory.getLogger(MbProtoServiceImpl.class);

    private static final int THREADS_NUM_TO_PROCEED_INBOX = 4;

    /**
     * Список всех потребителей.
     */
    private final Set<ConsumerData> consumers = Sets.newConcurrentHashSet();

    /**
     * Пул потоков для обработки входящих сообщений
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ExecutorService executorService;

    private final BlockingQueue<Handler> queue = new ArrayBlockingQueue<>(30_000);

    public MbProtoServiceImpl() {
        executorService = Executors.newFixedThreadPool(THREADS_NUM_TO_PROCEED_INBOX);
        for (int i = 0; i < THREADS_NUM_TO_PROCEED_INBOX; i++) {
            executorService.submit(new Processor(queue));
        }
    }

    public StreamObserver<Mbproto.ProduceRequest> produce(
            StreamObserver<Mbproto.ProduceResponse> responseObserver) {

        return new StreamObserver<Mbproto.ProduceRequest>() {

            @Override
            public void onNext(Mbproto.ProduceRequest request) {
                queue.add(new IncomeMessageHandler(consumers, request.getKey(), request.getPayload()));
            }

            @Override
            public void onError(Throwable t) {
                LOGGER.error("Продюсер закрылся с ошибкой", t);
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

        return new StreamObserver<Mbproto.ConsumeRequest>() {

            @Override
            public void onNext(Mbproto.ConsumeRequest consumeRequest) {
                String[] templates = new String[consumeRequest.getKeysCount()];
                for (int i = 0; i < consumeRequest.getKeysCount(); i++) {
                    templates[i] = consumeRequest.getKeys(i);
                    if (consumeRequest.getActionValue() == 0) {
                        //logger.info(String.format("Подписка [%d]: ", consumer.getNum()) + template);
                        thisConsumer.getTemplateManager().addTemplate(thisConsumer, templates[i]);
                    } else {
                        //logger.info(String.format("Отписка [%d]: ", consumer.getNum()) + template);
                        thisConsumer.getTemplateManager().removeTemplate(thisConsumer, templates[i]);
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                consumers.remove(thisConsumer);
                thisConsumer.onDelete();
                LOGGER.error("Консьюмер закрылся с ошибкой", t);
            }

            @Override
            public void onCompleted() {
                try {
                    consumers.remove(thisConsumer);
                    thisConsumer.onDelete();
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOGGER.error("Консьюмер закрылся с ошибкой (onCompleted)", e);
                }
            }
        };
    }

}
