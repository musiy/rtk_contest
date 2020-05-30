package rtk_contest.server;

import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeSubscriptionHandler implements Handler {

    private final Logger logger = LoggerFactory.getLogger(ChangeSubscriptionHandler.class);

    private final ConsumerData consumer;
    private final Mbproto.ConsumeRequest consumeRequest;

    public ChangeSubscriptionHandler(ConsumerData consumer, Mbproto.ConsumeRequest consumeRequest) {
        this.consumer = consumer;
        this.consumeRequest = consumeRequest;
    }

    @Override
    public void handle() {
        for (int i = 0; i < consumeRequest.getKeysCount(); i++) {
//            if (consumeRequest.getKeys(i).equals("print_stat")) {
//                GlobalSearchContext.printStat();
//            }
            if (consumeRequest.getActionValue() == 0) {
                GlobalSearchContext.addTemplate(consumer, consumeRequest.getKeys(i));
            } else {
                GlobalSearchContext.removeTemplate(consumer, consumeRequest.getKeys(i));
            }
        }
    }
}
