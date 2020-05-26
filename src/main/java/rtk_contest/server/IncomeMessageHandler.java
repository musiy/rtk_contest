package rtk_contest.server;

import com.google.protobuf.ByteString;
import mbproto.Mbproto;
import rtk_contest.templating.StringHelper;

import java.util.Set;

public class IncomeMessageHandler implements Handler {

    private final Set<ConsumerData> consumers;
    private final String key;
    private final ByteString payload;

    public IncomeMessageHandler(Set<ConsumerData> consumers,
                                String key, ByteString payload) {
        this.consumers = consumers;
        this.key = key;
        this.payload = payload;
    }

    @Override
    public void handle() {
        String[] keyComps = StringHelper.split(key);

        Mbproto.ConsumeResponse response = null;
        for (ConsumerData consumer : consumers) {
            if (consumer.getTemplateManager().matchToKey(key, keyComps)) {
                //logger.info(String.format("Отправлено [%d]: %s", consumer.num, key));
                if (response == null) {
                    response = Mbproto.ConsumeResponse.newBuilder()
                            .setKey(key)
                            .setPayload(payload)
                            .build();
                }
                consumer.send(response);
            }
        }
    }
}
