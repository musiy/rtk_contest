package rtk_contest.templating;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateMatcherFactory {

    private static final Map<String, TemplateMatcher> CACHE = new ConcurrentHashMap<>();

    public static TemplateMatcher getByTemplate(String template) {
        return new TemplateMatcherImpl(template);
    }

    public static void free(String template) {
        CACHE.remove(template);
    }
}
