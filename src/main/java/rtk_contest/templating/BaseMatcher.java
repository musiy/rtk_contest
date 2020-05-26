package rtk_contest.templating;

import rtk_contest.server.ConsumerData;

import java.util.Objects;

abstract class BaseMatcher implements TemplateMatcher {
    private final String template;
    private final ConsumerData consumerData;

    public BaseMatcher(ConsumerData consumerData, String template) {
        this.consumerData = consumerData;
        this.template = template;
    }

    @Override
    public ConsumerData getConsumerData() {
        return consumerData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseMatcher that = (BaseMatcher) o;
        return Objects.equals(template, that.template) &&
               Objects.equals(consumerData, that.consumerData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template, consumerData);
    }
}
