package rtk_contest.server;

import com.google.common.collect.Sets;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import rtk_contest.templating.StringHelper;
import rtk_contest.templating.TemplateMatcher;
import rtk_contest.templating.TemplateMatcherImpl;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс описывает потребителя - его стрим и шаблоны на которые он подписан.
 */
class ConsumerData implements Comparable<ConsumerData> {

    static final AtomicInteger CONSUMER_ENUMERATOR = new AtomicInteger(1);
    /**
     * порядковый номер потребителя
     */
    private final int num = CONSUMER_ENUMERATOR.incrementAndGet();
    /**
     * Стрим для отправки данных консюмеру
     */
    private final StreamObserver<Mbproto.ConsumeResponse> responseObserver;
    /**
     * Набор шаблонов консюмера в которых есть слова (можно быстро отфильтровать)
     */
    private final Map<String, Set<TemplateMatcher>> templatesHasWords = new ConcurrentHashMap<>();
    /**
     * Набор шаблонов консюмера в которых нет слов (например, #.*) - такие придется перебирать всегда
     */
    private final Set<TemplateMatcher> templatesNotHasWords = Sets.newConcurrentHashSet();

    public ConsumerData(StreamObserver<Mbproto.ConsumeResponse> responseObserver) {
        this.responseObserver = responseObserver;
    }

    void addTemplate(String template) {
        String[] comps = StringHelper.split(template);
        TemplateMatcher templateMatcher = new TemplateMatcherImpl(template, comps);

        boolean hasWorlds = false;
        for (String comp : comps) {
            if (!"#".equals(comp) && !"*".equals(comp)) {
                hasWorlds = true;
                templatesHasWords.computeIfAbsent(comp, key -> Sets.newConcurrentHashSet())
                        .add(templateMatcher);
            }
        }
        if (!hasWorlds) {
            templatesNotHasWords.add(templateMatcher);
        }
    }

    void removeTemplate(String template) {
        String[] comps = StringHelper.split(template);
        TemplateMatcher templateMatcher = new TemplateMatcherImpl(template, comps);

        for (String comp : comps) {
            if (!"#".equals(comp) && !"*".equals(comp)) {
                Set<TemplateMatcher> templateMatchers = templatesHasWords.get(comp);
                if (templateMatchers != null) {
                    templateMatchers.remove(templateMatcher);
                }
            }
        }
        templatesNotHasWords.remove(templateMatcher);
    }

    public boolean matchToKey(String[] keyComps) {
        for (String comps : keyComps) {
            Set<TemplateMatcher> templateMatchers = templatesHasWords.get(comps);
            if (templateMatchers != null) {
                for (TemplateMatcher matcher : templateMatchers) {
                    if (matcher.matchTo(keyComps)) {
                        return true;
                    }
                }
            }
        }
        for (TemplateMatcher matcher : templatesNotHasWords) {
            if (matcher.matchTo(keyComps)) {
                return true;
            }
        }
        return false;
    }

    public void onNext(Mbproto.ConsumeResponse response) {
        responseObserver.onNext(response);
    }

    @Override
    public boolean equals(Object o) {
        ConsumerData that = (ConsumerData) o;
        return num == that.num;
    }

    @Override
    public int hashCode() {
        return num;
    }

    @Override
    public int compareTo(ConsumerData that) {
        return that.num - num;
    }
}
