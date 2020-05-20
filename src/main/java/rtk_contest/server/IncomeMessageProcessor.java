package rtk_contest.server;

import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.StringHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;

public class IncomeMessageProcessor extends Thread {

    private final Logger LOGGER = LoggerFactory.getLogger(IncomeMessageProcessor.class);

    private final ConcurrentSkipListSet<ConsumerData> consumers;
    private final BlockingQueue<InboxRequestInfo> bqueue;

    public IncomeMessageProcessor(ConcurrentSkipListSet<ConsumerData> consumers, BlockingQueue<InboxRequestInfo> bqueue) {
        this.consumers = consumers;
        this.bqueue = bqueue;
    }

    @Override
    public void run() {
        while (true) {
            InboxRequestInfo requestInfo;
            try {
                requestInfo = bqueue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
                continue;
            }

            Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                    .setKey(requestInfo.key)
                    .setPayload(requestInfo.value)
                    .build();
            String[] keyComps = StringHelper.split(requestInfo.key);

            List<ConsumerData> toDelete = new LinkedList<>();
            for (ConsumerData consumer : consumers) {
                boolean matchFound = consumer.matchToKey(keyComps);
                if (matchFound) {
                    try {
                        consumer.onNext(response);
                    } catch (Exception e) {
                        toDelete.add(consumer);
                        LOGGER.error(String.format("closed consumer? [%d]", consumer.hashCode()));
                    }
                }
            }

            for (ConsumerData consumer : toDelete) {
                consumers.remove(consumer);
            }
        }
    }
}
