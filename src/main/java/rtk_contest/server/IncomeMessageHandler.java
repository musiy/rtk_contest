package rtk_contest.server;

import com.google.protobuf.ByteString;
import mbproto.Mbproto;
import rtk_contest.templating.StringHelper;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class IncomeMessageHandler implements Handler {

    // Logger logger = LoggerFactory.getLogger(IncomeMessageHandler.class);

    private final Set<ConsumerData> consumers;
    private final String key;
    private final ByteString payload;

    public IncomeMessageHandler(Set<ConsumerData> consumers, String key, ByteString payload) {
        this.consumers = consumers;
        this.key = key;
        this.payload = payload;
    }

    @Override
    public void handle() {
        String[] keyComps = StringHelper.split(key);

        List<ConsumerData> toDelete = new LinkedList<>();
        boolean oneMatched = false;
        for (ConsumerData consumer : consumers) {
            if (consumer.matchToKey(keyComps)) {
                oneMatched = true;
                try {
                    //logger.info(String.format("Отправлено [%d]: %s", consumer.num, key));
                    Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                            .setKey(key)
                            .setPayload(payload)
                            .build();
                    consumer.onNext(response);
                } catch (Exception e) {
                    toDelete.add(consumer);
                    //logger.error(String.format("closed consumer? [%d]", consumer.hashCode()));
                }
            }
        }
        if (!oneMatched) {
            //logger.info(String.format("Не сматчено %s", key));
        }

        for (ConsumerData consumer : toDelete) {
            consumers.remove(consumer);
        }
    }
}
