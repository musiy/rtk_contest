package rtk_contest.templating;

import rtk_contest.server.ConsumerData;

public interface TemplateMatcher {

    boolean matchTo(String[] keyComps);

    ConsumerData getConsumerData();
}
