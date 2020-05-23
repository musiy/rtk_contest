package rtk_contest.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class Processor implements Runnable {

    private final Logger LOGGER = LoggerFactory.getLogger(Processor.class);

    private final BlockingQueue<Handler> bqueue;

    public Processor(BlockingQueue<Handler> bqueue) {
        this.bqueue = bqueue;
    }

    @Override
    public void run() {
        while (true) {
            Handler handler;
            try {
                handler = bqueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }
            try {
                handler.handle();
            } catch (Throwable e) {
                LOGGER.error("Что то пошло не так", e);
            }
        }
    }
}
