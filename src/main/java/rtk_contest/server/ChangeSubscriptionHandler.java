package rtk_contest.server;

import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.StringHelper;

import java.util.concurrent.atomic.LongAdder;

public class ChangeSubscriptionHandler implements Handler {

    private final Logger logger = LoggerFactory.getLogger(ChangeSubscriptionHandler.class);

    static LongAdder time = new LongAdder();

    private final ConsumerData consumer;
    private final Mbproto.ConsumeRequest consumeRequest;

    public ChangeSubscriptionHandler(ConsumerData consumer, Mbproto.ConsumeRequest consumeRequest) {
        this.consumer = consumer;
        this.consumeRequest = consumeRequest;
    }

    @Override
    public void handle() {
//        long t0 = System.nanoTime();
        for (int i = 0; i < consumeRequest.getKeysCount(); i++) {

            String template = consumeRequest.getKeys(i);
            String[] comps = StringHelper.split(template);
            if (comps.length > 2) {
                if (consumeRequest.getActionValue() == 0) {
                    //logger.info("Подписка: " + template);
                } else {
                    //logger.info("Отписка: " + template);
                }
            }

            if (consumeRequest.getActionValue() == 0) {
                GlobalSearchContext.addTemplate(consumer, consumeRequest.getKeys(i));
            } else {
                GlobalSearchContext.removeTemplate(consumer, consumeRequest.getKeys(i));
            }
        }
//        time.add(System.nanoTime() - t0);
//        logger.info(String.format("template change: %d", time.longValue())
    }
}
