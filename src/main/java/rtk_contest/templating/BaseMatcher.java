package rtk_contest.templating;

import com.google.common.collect.Sets;
import mbproto.Mbproto;
import rtk_contest.server.ConsumerData;

import java.util.BitSet;
import java.util.Objects;
import java.util.Set;

abstract class BaseMatcher implements TemplateMatcher {
    final String template;
    volatile Set<ConsumerData> consumers;
    volatile ConsumerData consumer;

    public BaseMatcher(String template) {
        this.template = template;
    }

    /**
     * Пессимистичная блокировка - вроде ок
     */
    @Override
    public synchronized void addConsumer(ConsumerData consumerData) {
        if (consumers == null) {
            if (this.consumer == null) {
                this.consumer = consumerData;
            } else {
                consumers = Sets.newConcurrentHashSet();
                consumers.add(consumer);
                consumers.add(consumerData);
            }
        } else {
            consumers.add(consumer);
        }
    }

    @Override
    public synchronized boolean removeConsumerData(ConsumerData consumerData) {
        if (consumers == null) {
            consumer = null;
            return true;
        } else {
            consumers.remove(consumerData);
            return consumers.isEmpty();
        }
    }

    @Override
    public void sendIfMatchAndUpdateBitSet(BitSet bitSet, String[] comps, Mbproto.ConsumeResponse response) {
        if (!matchTo(comps)) {
            return;
        }
        if (consumer != null) {
            if (!bitSet.get(consumer.getNum())) {
                bitSet.set(consumer.getNum(), 1);
                consumer.send(response);
            }
        } else {
            for (ConsumerData consumerData : consumers) {
                if (!bitSet.get(consumerData.getNum())) {
                    bitSet.set(consumerData.getNum(), 1);
                    consumerData.send(response);
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseMatcher that = (BaseMatcher) o;
        return Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template);
    }
}
