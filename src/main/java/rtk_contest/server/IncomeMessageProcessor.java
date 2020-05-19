package rtk_contest.server;

import mbproto.Mbproto;
import rtk_contest.templating.TemplateMatcher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

class IncomeMessageProcessor extends Thread {

    static final int MAX_MESSAGE_QUEUE = 10;

    static final AtomicInteger WORKER_ENUMERATOR = new AtomicInteger();

    private final int workerId = WORKER_ENUMERATOR.incrementAndGet();
    private final ConcurrentHashMap<String, ConcurrentSkipListSet<ConsumerData>> addressing;
    private final ConcurrentSkipListSet<ConsumerData> consumers;

    private final InboxRequestInfo[] queue = new InboxRequestInfo[MAX_MESSAGE_QUEUE];
    private final AtomicLong w_pos = new AtomicLong(0);
    private int r_pos = 0;

    public IncomeMessageProcessor(ConcurrentHashMap<String, ConcurrentSkipListSet<ConsumerData>> addressing,
                                  ConcurrentSkipListSet<ConsumerData> consumers) {
        this.addressing = addressing;
        this.consumers = consumers;
    }

    public void addMessage(InboxRequestInfo requestInfo) {
        int idx = (int) (w_pos.getAndIncrement() % MAX_MESSAGE_QUEUE);
        queue[idx] = requestInfo;
    }

    @Override
    public void run() {
        InboxRequestInfo requestInfo;
        while (true) {
            while ((requestInfo = queue[r_pos]) == null) {
                r_pos++;
                if (r_pos == MAX_MESSAGE_QUEUE) {
                    r_pos = 0;
                }
            }
            queue[r_pos] = null;
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
                try {
                    consumer.onNext(response);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }
}
