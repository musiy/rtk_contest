package rtk_contest.server;

import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.TemplateMatcher;
import rtk_contest.templating.TemplateMatcherFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;

public class IncomeMessageProcessor extends Thread {

    private final Logger LOGGER = LoggerFactory.getLogger(IncomeMessageProcessor.class);
    private final Logger LOGGER_MESSAGE_PROCESSING = LoggerFactory.getLogger("message.processing");

    static final int MAX_MESSAGE_QUEUE = 700;

    // попрог в МС, при первышении которого сообщение не отправляем
    static final int THRESHOLD = 500;

    static final AtomicInteger WORKER_ENUMERATOR = new AtomicInteger();
    private final int workerId = WORKER_ENUMERATOR.incrementAndGet();

    private final ConcurrentSkipListSet<ConsumerData> consumers;

    private final Object[][] queue = new Object[MAX_MESSAGE_QUEUE][2];
    private final AtomicIntegerArray q_flag = new AtomicIntegerArray(MAX_MESSAGE_QUEUE);
    // Состояние доступно для записи (т.е. элемент ещё пустой или был ранее обработан)
    static final int CAN_WRITE = 0;
    // Состояние, устанавливаемое потоком при захвате элемента
    static final int BUSY_WRITE = 1;
    // После захвата элемента (состояние BUSY_CUR) следует "освободить" элемент, установим ему состояние CAN_READ
    // В состоянии CAN_READ элемент может быть выбран из очереди.
    static final int CAN_READ = 2;
    //  Что бы один и тот же элемент не был выбран дважды, при получении элемента для чтения маркаем его как busy
    static final int BUSY_READ = 2;

    private final AtomicLong w_position = new AtomicLong();
    private final AtomicLong r_position = new AtomicLong();

    // CAN_WRITE -> BUSY_WRITE -> CAN_READ -> BUSY_READ -> CAN_WRITE

    public IncomeMessageProcessor(ConcurrentSkipListSet<ConsumerData> consumers) {
        this.consumers = consumers;
    }

    public void addMessage(InboxRequestInfo requestInfo) {
        // цикл по всем элементам, ищем свободный слот для записи
        long millis = System.currentTimeMillis();
        boolean found = false;
        int count = 0;
        while (count++ < MAX_MESSAGE_QUEUE) {
            int pos = (int) (w_position.incrementAndGet() % MAX_MESSAGE_QUEUE);
            if (q_flag.compareAndSet(pos, CAN_WRITE, BUSY_WRITE)) {
                found = true;
                queue[pos][0] = requestInfo;
                queue[pos][1] = millis;
                q_flag.set(pos, CAN_READ);
                break;
            }
        }
        if (!found) {
            LOGGER.info("throttling: " + requestInfo.key);
        }
    }

    @Override
    public void run() {
        int r_pos = 0;
        while (true) {
            long t0 = System.currentTimeMillis();
            InboxRequestInfo requestInfo;

            while (true) {
                if (q_flag.compareAndSet(r_pos, CAN_READ, BUSY_READ)) {
                    if (System.currentTimeMillis() - (long) queue[r_pos][1] < THRESHOLD) {
                        requestInfo = (InboxRequestInfo) queue[r_pos][0];
                        q_flag.set(r_pos, CAN_WRITE);
                        break;
                    }
                    q_flag.set(r_pos, CAN_WRITE);
                }
                r_pos++;
                if (r_pos == MAX_MESSAGE_QUEUE) {
                    r_pos = 0;
                }
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
            if (t2 - t1 > 50) {
                LOGGER_MESSAGE_PROCESSING.info(String.format("wait: %d, proc: %d, mf: %d", t1 - t0, t2 - t1, cnt));
            }
            // ######################################################################################3
        }
    }
}
