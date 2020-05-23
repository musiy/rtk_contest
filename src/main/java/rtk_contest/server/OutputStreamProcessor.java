package rtk_contest.server;

import mbproto.Mbproto;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class OutputStreamProcessor implements Runnable {

    private final BlockingQueue<Addressing> streamQueue;
    private final Set<ConsumerData> consumers;

    public OutputStreamProcessor(BlockingQueue<Addressing> streamQueue, Set<ConsumerData> consumers) {
        this.streamQueue = streamQueue;
        this.consumers = consumers;
    }

    @Override
    public void run() {
        Addressing addressing;
        while (true) {
            try {
                addressing = streamQueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            try {
                addressing.consumerData.responseObserver.onNext(addressing.response);
            } catch (Throwable t) {
                consumers.remove(addressing.consumerData);
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
