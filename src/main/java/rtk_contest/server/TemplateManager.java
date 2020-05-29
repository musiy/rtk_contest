package rtk_contest.server;

public class TemplateManager {

//    private final Logger LOGGER = LoggerFactory.getLogger(ConsumerData.class);
//
//    /**
//     * Набор шаблонов консюмера в которых слова (могу быть и спец символы)
//     */
//    private final Map<String, Set<TemplateMatcher>> templatesCommon = new ConcurrentHashMap<>();
//
//    private final Set<String> onlyWords = Sets.newConcurrentHashSet();
//    /**
//     * Набор шаблонов консюмера _без слов_.
//     */
//    private final Set<TemplateMatcher> templatesWithoutWords = Sets.newConcurrentHashSet();
//    private volatile boolean hasTemplatesWithoutWords = false;
//
//    public void addTemplate(ConsumerData consumerData, String template) {
//        String[] comps = StringHelper.split(template);
//
//        boolean hasWords = false;
//        boolean hasSpec = false;
//        for (int i = 0; i < comps.length; i++) {
//            if ('#' == comps[i].charAt(0) || '*' == comps[i].charAt(0)) {
//                hasSpec = true;
//            } else {
//                hasWords = true;
//            }
//        }
//
//        if (hasWords && !hasSpec) {
//            onlyWords.add(template);
//        } else if (!hasWords && hasSpec) {
//            TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(consumerData, template, comps);
//            templatesWithoutWords.add(templateMatcher);
//            hasTemplatesWithoutWords = true;
//        } else {
//            TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(consumerData, template, comps);
//            for (int i = 0; i < comps.length; i++) {
//                if ('#' == comps[i].charAt(0) || '*' == comps[i].charAt(0)) {
//                    // ничего не делаем, это спец. символ
//                } else {
//                    templatesCommon.computeIfAbsent(comps[i], key -> Sets.newConcurrentHashSet())
//                            .add(templateMatcher);
//                }
//            }
//        }
//
//    }
//
//    public void removeTemplate(ConsumerData consumer, String template) {
//        String[] comps = StringHelper.split(template);
//
//        boolean hasWords = false;
//        boolean hasSpec = false;
//        for (int i = 0; i < comps.length; i++) {
//            if ('#' == comps[i].charAt(0) || '*' == comps[i].charAt(0)) {
//                hasSpec = true;
//            } else {
//                hasWords = true;
//            }
//        }
//
//        if (hasWords && !hasSpec) {
//            onlyWords.remove(template);
//        } else if (!hasWords && hasSpec) {
//            TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(consumer, template, comps);
//            templatesWithoutWords.remove(templateMatcher);
//            if (templatesWithoutWords.isEmpty()) {
//                hasTemplatesWithoutWords = false;
//            }
//        } else {
//            TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(consumer, template, comps);
//            for (int i = 0; i < comps.length; i++) {
//                String comp = comps[i];
//                if ('#' == comp.charAt(0) || '*' == comp.charAt(0)) {
//                    // ничего не делаем, это спец. символ
//                } else {
//                    Set<TemplateMatcher> templateMatchers = templatesCommon.get(comp);
//                    if (templateMatchers != null) {
//                        templateMatchers.remove(templateMatcher);
//                        if (templateMatchers.isEmpty()) {
//                            Set<TemplateMatcher> deleted = templatesCommon.remove(comp);
//                            if (!deleted.isEmpty()) {
//                                LOGGER.error("Удалили не пустой набор матчеров!" + template);
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    public boolean matchToKey(String key, String[] comps) {
//
//        if (onlyWords.contains(key)) {
//            return true;
//        }
//
//        for (int i = 0; i < comps.length; i++) {
//            Set<TemplateMatcher> templateMatchers = templatesCommon.get(comps[i]);
//            if (templateMatchers != null) {
//                for (TemplateMatcher matcher : templateMatchers) {
//                    if (matcher.matchTo(comps)) {
//                        return true;
//                    }
//                }
//            }
//        }
//
//        if (hasTemplatesWithoutWords) {
//            for (TemplateMatcher templateMatcher : templatesWithoutWords) {
//                if (templateMatcher.matchTo(comps)) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    public void onDelete() {
//        for (Map.Entry<String, Set<TemplateMatcher>> entry : templatesCommon.entrySet()) {
//            entry.getValue().clear();
//        }
//        templatesCommon.clear();
//        onlyWords.clear();
//        templatesWithoutWords.clear();
//    }
}
