package rtk_contest.server;

import com.google.common.collect.EvictingQueue;
import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.TemplateMatcher;
import rtk_contest.templating.TemplateMatcherFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class IncomeMessageProcessor extends Thread {

    private final Logger LOGGER = LoggerFactory.getLogger(IncomeMessageProcessor.class);
    private final Logger LOGGER_MESSAGE_PROCESSING = LoggerFactory.getLogger("message.processing");

    static final int MAX_MESSAGE_QUEUE = 10;

    static final AtomicInteger WORKER_ENUMERATOR = new AtomicInteger();

    private final int workerId = WORKER_ENUMERATOR.incrementAndGet();
    private final ConcurrentSkipListSet<ConsumerData> consumers;

    private final EvictingQueue<InboxRequestInfo> evictingQueue = EvictingQueue.create(MAX_MESSAGE_QUEUE);

    public IncomeMessageProcessor(ConcurrentSkipListSet<ConsumerData> consumers) {
        this.consumers = consumers;
    }

    public void addMessage(InboxRequestInfo requestInfo) {
        evictingQueue.add(requestInfo);
    }

    public void anotify() {
        synchronized (evictingQueue) {
            evictingQueue.notify();
        }
    }

    private void await() {
        synchronized (evictingQueue) {
            try {
                evictingQueue.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {

        while (true) {
            long t0 = System.currentTimeMillis();
            InboxRequestInfo requestInfo;
            while ((requestInfo = evictingQueue.poll()) == null) {
                await();
            }
            long t1 = System.currentTimeMillis();
            Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                    .setKey(requestInfo.key)
                    .setPayload(requestInfo.value)
                    .build();
            int cnt = 0;

            List<ConsumerData> toDelete = new LinkedList<>();
            for (ConsumerData consumer : consumers) {
                boolean matchFound = false;
                for (String template : consumer.getTemplates()) {
                    TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(template);
                    if (templateMatcher.matchTo(requestInfo.key)) {
                        matchFound = true;
                        cnt++;
                        break;
                    }
                }
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
                for (String template : consumer.getTemplates()) {
                    TemplateMatcherFactory.free(template);
                }
            }

            long t2 = System.currentTimeMillis();
            // ######################################################################################3
            if (t2 - t1 > 200) {
                LOGGER_MESSAGE_PROCESSING.info(String.format("wait: %d, proc: %d, mf: %d", t1 - t0, t2 - t1, cnt));
            }
            // ######################################################################################3
        }
    }
}
