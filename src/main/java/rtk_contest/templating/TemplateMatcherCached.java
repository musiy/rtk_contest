package rtk_contest.templating;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateMatcherCached extends TemplateMatcherImpl {

    private static final Map<String, Boolean> CACHE = new ConcurrentHashMap<>();

    public TemplateMatcherCached(String template) {
        super(template);
    }

    @Override
    public boolean matchTo(String key) {
        return CACHE.computeIfAbsent(key, super::matchTo);
    }
}
