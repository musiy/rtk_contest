package rtk_contest.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import mbproto.MessageBrokerGrpc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MbProtoServiceImpl extends MessageBrokerGrpc.MessageBrokerImplBase {

    private final Logger LOGGER = LoggerFactory.getLogger(MbProtoServiceImpl.class);

    /**
     * Пул потоков для обработки входящих сообщений
     */
    @SuppressWarnings("FieldCanBeLocal")
    private final ExecutorService executorService;
    private final BlockingQueue<Handler> queue = new ArrayBlockingQueue<>(30_000);

    public MbProtoServiceImpl() {
        executorService = Executors.newFixedThreadPool(1,
                new ThreadFactoryBuilder().setNameFormat("consumers-event-thread--%d").build());
        executorService.submit(new Processor(queue));
    }

    public StreamObserver<Mbproto.ProduceRequest> produce(
            StreamObserver<Mbproto.ProduceResponse> responseObserver) {

        return new StreamObserver<Mbproto.ProduceRequest>() {

            @Override
            public void onNext(Mbproto.ProduceRequest request) {
                long t0 = System.currentTimeMillis();
                Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                        .setKey(request.getKey())
                        .setPayload(request.getPayload())
                        .build();
                GlobalSearchContext.matchToAndSend(response, request.getKey());
                long delta = System.currentTimeMillis() - t0;
                if (delta > 2) {
                    //LOGGER.info(String.format("%d - %s", delta, request.getKey()));
                }
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
        GlobalSearchContext.addConsumer(thisConsumer);

        return new StreamObserver<Mbproto.ConsumeRequest>() {

            @Override
            public void onNext(Mbproto.ConsumeRequest consumeRequest) {
                queue.add(new ChangeSubscriptionHandler(thisConsumer, consumeRequest));
            }

            @Override
            public void onError(Throwable t) {
                //GlobalSearchContext.removeConsumer(thisConsumer);
                LOGGER.error("Консьюмер закрылся с ошибкой", t);
            }

            @Override
            public void onCompleted() {
                try {
                    responseObserver.onCompleted();
                } catch (Exception e) {
                    LOGGER.error("Консьюмер закрылся с ошибкой (onCompleted)", e);
                } finally {
                    //GlobalSearchContext.removeConsumer(thisConsumer);
                }
            }
        };
    }

}
