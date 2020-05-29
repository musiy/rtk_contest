package rtk_contest.templating;

import mbproto.Mbproto;
import rtk_contest.server.ConsumerData;

import java.util.BitSet;

public interface TemplateMatcher {

    boolean matchTo(String[] keyComps);

    /**
     * Добавляет консьюмера к шаблону
     */
    void addConsumer(ConsumerData consumerData);

    /**
     * Удаляет консьюмера у шаблона
     *
     * @return true - если это был последний консьюмер по шаблону
     */
    boolean removeConsumerData(ConsumerData consumerData);

    void sendIfMatchAndUpdateBitSet(BitSet bitSet, String[] comps, Mbproto.ConsumeResponse response);
}
