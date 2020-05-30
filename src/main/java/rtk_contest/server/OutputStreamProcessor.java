package rtk_contest.server;

import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class OutputStreamProcessor implements Runnable {

    static Logger logger = LoggerFactory.getLogger(OutputStreamProcessor.class);

    private BlockingQueue<Addressing> outputStreamQueue;

    public OutputStreamProcessor(BlockingQueue<Addressing> outputStreamQueue) {
        this.outputStreamQueue = outputStreamQueue;
    }

    @Override
    public void run() {
        Addressing addressing;
        while (true) {
            try {
                addressing = outputStreamQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            try {
                addressing.consumerData.responseObserver.onNext(addressing.response);
            } catch (Throwable t) {
                logger.error("Какая то ошибка при отправке", t);
            }
        }
    }

    public static class Addressing {
        private final ConsumerData consumerData;
        private final Mbproto.ConsumeResponse response;

        public Addressing(ConsumerData consumerData, Mbproto.ConsumeResponse response) {
            this.consumerData = consumerData;
            this.response = response;
        }
    }
}
