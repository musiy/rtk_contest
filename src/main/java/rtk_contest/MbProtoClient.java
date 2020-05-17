package rtk_contest;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import mbproto.MessageBrokerGrpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MbProtoClient {

    private static final Logger logger = Logger.getLogger(MbProtoClient.class.getName());

    MessageBrokerGrpc.MessageBrokerBlockingStub blockingStub;
    MessageBrokerGrpc.MessageBrokerStub asyncStub;

    public MbProtoClient(ManagedChannel channel) {
        blockingStub = MessageBrokerGrpc.newBlockingStub(channel);
        asyncStub = MessageBrokerGrpc.newStub(channel);
    }

    /**
     * Issues several different requests and then exits.
     */
    public static void main(String[] args) throws InterruptedException {
        // некрасиво, но для теста сойдёт
        String target = args[0];
        String mode = args[1];
        String filename = args[2];
        logger.log(Level.INFO, String.format("Подключение к серверу [%s] в режиме [%s]. Файл данных: ", target, mode, filename));

        ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            MbProtoClient client = new MbProtoClient(channel);
            if ("consumer".equals(mode)) {
                Thread t = new Thread(new ConsumerThread(client, filename, countDownLatch));
                t.start();
            } else if ("producer".equals(mode)) {
                Thread t = new Thread(new Producer(client, filename, countDownLatch));
                t.start();
            } else {
                throw new RuntimeException("Unknown mode: " + mode);
            }
        } finally {
            countDownLatch.await();
        }
    }

    /**
     * Консьюмер сообщений
     */
    static class ConsumerThread implements Runnable {

        private final MbProtoClient client;
        private final String filename;
        private final CountDownLatch countDownLatch;

        public ConsumerThread(MbProtoClient client, String filename, CountDownLatch countDownLatch) {
            this.client = client;
            this.filename = filename;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {

            StreamObserver<Mbproto.ConsumeResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(Mbproto.ConsumeResponse summary) {
                    System.out.println(String.format("\n ===========================\nПришло сообщение: [%s:%s]", summary.getKey(),
                            summary.getPayload().toStringUtf8()));
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.SEVERE, "Ошибка коммуникации", t);
                    countDownLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    logger.log(Level.FINE, "Завершение.");
                    countDownLatch.countDown();
                }
            };

            StreamObserver<Mbproto.ConsumeRequest> consumer = client.asyncStub.consume(responseObserver);

            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("Введите S/U и через пробел группу шаблонов разделенные запятыми > ");
                String line;
                try {
                    line = reader.readLine();
                } catch (IOException e) {
                    countDownLatch.countDown();
                    break;
                }
                int pos = line.indexOf(' ');
                if (pos == -1) {
                    logger.log(Level.WARNING, "Ошибка в формате ввода: " + line);
                    continue;
                }
                String cmd = line.substring(0, pos);
                String templates = line.substring(pos + 1);
                Mbproto.ConsumeRequest.Builder builder = Mbproto.ConsumeRequest.newBuilder()
                        .setActionValue("S".equals(cmd) ? 0 : 1);
                Arrays.stream(templates.split(",")).forEach(builder::addKeys);
                consumer.onNext(builder.build());
            }

        }
    }

    /**
     * Поставщик сообщений (продюсер)
     */
    static class Producer implements Runnable {

        private final MbProtoClient client;
        private final String filename;
        private final CountDownLatch countDownLatch;

        public Producer(MbProtoClient client, String filename, CountDownLatch countDownLatch) {
            this.client = client;
            this.filename = filename;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {

            StreamObserver<Mbproto.ProduceResponse> responseObserver = new StreamObserver<>() {
                @Override
                public void onNext(Mbproto.ProduceResponse summary) {
                    System.out.println("Прекращение коммуникации.");
                }

                @Override
                public void onError(Throwable t) {
                    logger.log(Level.SEVERE, "Ошибка коммуникации", t);
                }

                @Override
                public void onCompleted() {
                    logger.log(Level.FINE, "Завершение.");
                }
            };

            StreamObserver<Mbproto.ProduceRequest> produce = client.asyncStub.produce(responseObserver);

            try {
                runThrow(produce);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Ошибка", e);
            }
            produce.onCompleted();
            countDownLatch.countDown();
        }


        void runThrow(StreamObserver<Mbproto.ProduceRequest> produce) throws Exception {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            String line;
            while (true) {
                System.out.print("Введите ключ и значение через пробел > ");
                line = reader.readLine();
                if (line == null) {
                    break;
                }
                int pos = line.indexOf(' ');
                if (pos == -1) {
                    logger.log(Level.WARNING, "Ошибка в формате ввода: " + line);
                    continue;
                }
                String key = line.substring(0, pos);
                String payload = line.substring(pos + 1);
                Mbproto.ProduceRequest message = Mbproto.ProduceRequest.newBuilder()
                        .setKey(key)
                        .setPayload(ByteString.copyFromUtf8(payload))
                        .build();
                produce.onNext(message);
                logger.log(Level.INFO, String.format("Отправлено сообщение [%s:%s]", key, payload));
            }
        }
    }
}


