package rtk_contest.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeSubscriptionHandler implements Handler {

    private final Logger logger = LoggerFactory.getLogger(ChangeSubscriptionHandler.class);

    private final ConsumerData consumer;
    private final int actionValue;
    private final String[] templates;

    public ChangeSubscriptionHandler(ConsumerData consumer, int actionValue, String[] templates) {
        this.consumer = consumer;
        this.actionValue = actionValue;
        this.templates = templates;
    }

    @Override
    public void handle() {
        for (int i = 0; i < templates.length; i++) {
            if (actionValue == 0) {
                //logger.info(String.format("Подписка [%d]: ", consumer.getNum()) + template);
                consumer.getTemplateManager().addTemplate(templates[i]);
            } else {
                //logger.info(String.format("Отписка [%d]: ", consumer.getNum()) + template);
                consumer.getTemplateManager().removeTemplate(templates[i]);
            }
        }
    }
}
