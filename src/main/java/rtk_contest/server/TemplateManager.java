package rtk_contest.server;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.StringHelper;
import rtk_contest.templating.TemplateMatcher;
import rtk_contest.templating.TemplateMatcherFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateManager {

    private final Logger LOGGER = LoggerFactory.getLogger(ConsumerData.class);

    /**
     * Набор шаблонов где есть только слова
     */
    private final Set<String> templatesOnlyWords = Sets.newConcurrentHashSet();
    /**
     * Набор шаблонов консюмера в которых есть и слова и спец символы (придется запускать поиск по шаблону)
     */
    private final Map<String, Set<TemplateMatcher>> templatesCommon = new ConcurrentHashMap<>();
    /**
     * Набор шаблонов консюмера _без слов_, в которых есть хотя бы одна решетка '#'- такие подходят
     * ключам с числом слов >= N, где N - число звёздочек.
     */
    private final Map<Integer, Set<String>> templatesWithoutWordsWithHash = new ConcurrentHashMap<>();
    private volatile boolean hasTemplatesWithoutWordsWithHash = false;
    /**
     * Набор шаблонов в которых есть только звёзды
     */
    private final Map<Integer, Set<String>> templatesWithoutWordsStarsOnly = new ConcurrentHashMap<>();
    private volatile boolean hasTemplatesWithoutWordsStarsOnly = false;

    public void addTemplate(String template) {
        String[] comps = StringHelper.split(template);

        boolean hasWords = false;
        boolean hasHash = false;
        int starsCount = 0;
        for (String comp : comps) {
            if ('#' == comp.charAt(0)) {
                hasHash = true;
            } else if ('*' == comp.charAt(0)) {
                starsCount++;
            } else {
                hasWords = true;
            }
        }

        if (hasWords && !hasHash && starsCount == 0) {
            // содержит только слова
            templatesOnlyWords.add(template);
        } else if (!hasWords && hasHash) {
            // нет слов, но есть хеш (могут быть и звёзды)
            templatesWithoutWordsWithHash.computeIfAbsent(starsCount, cnt -> Sets.newConcurrentHashSet())
                    .add(template);
            hasTemplatesWithoutWordsWithHash = true;
        } else if (!hasWords && starsCount > 0) {
            // нет слов, нет хеша, есть звёзды
            templatesWithoutWordsStarsOnly.computeIfAbsent(comps.length, len -> Sets.newConcurrentHashSet())
                    .add(template);
            hasTemplatesWithoutWordsStarsOnly = true;
        } else {
            // смешанные шаблоны - есть и слова и спец. символы
            TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(template, comps);
            for (String comp : comps) {
                if ('#' == comp.charAt(0) || '*' == comp.charAt(0)) {
                    // ничего не делаем, это спец. символ
                } else {
                    templatesCommon.computeIfAbsent(comp, key -> Sets.newConcurrentHashSet())
                            .add(templateMatcher);
                }
            }
        }

    }

    public void removeTemplate(String template) {
        String[] comps = StringHelper.split(template);

        boolean hasWords = false;
        boolean hasHash = false;
        int starsCount = 0;
        for (String comp : comps) {
            if ('#' == comp.charAt(0)) {
                hasHash = true;
            } else if ('*' == comp.charAt(0)) {
                starsCount++;
            } else {
                hasWords = true;
            }
        }

        if (hasWords && !hasHash && starsCount == 0) {
            // содержит только слова
            templatesOnlyWords.remove(template);
        } else if (!hasWords && hasHash) {
            // нет слов, но есть хеш (могут быть и звёзды)
            Set<String> templatesWithHash = templatesWithoutWordsWithHash.get(starsCount);
            if (templatesWithHash != null) {
                templatesWithHash.remove(template);
                if (templatesWithHash.isEmpty()) {
                    Set<String> deleted = templatesWithoutWordsWithHash.remove(starsCount);
                    if (!deleted.isEmpty()) {
                        LOGGER.error("Удалили не пустой набор матчеров! " + template);
                    }
                }
            }
            hasTemplatesWithoutWordsWithHash = !templatesWithoutWordsWithHash.isEmpty();
        } else if (!hasWords && starsCount > 0) {
            // нет слов, нет хеша, есть звёзды
            Set<String> templatesStarsOnly = templatesWithoutWordsStarsOnly.get(comps.length);
            if (templatesStarsOnly != null) {
                templatesStarsOnly.remove(template);
                if (templatesStarsOnly.isEmpty()) {
                    Set<String> deleted = templatesWithoutWordsStarsOnly.remove(comps.length);
                    if (!deleted.isEmpty()) {
                        LOGGER.error("Удалили не пустой набор матчеров! " + template);
                    }
                }
            }
            hasTemplatesWithoutWordsStarsOnly = !templatesWithoutWordsStarsOnly.isEmpty();
        } else {
            TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(template, comps);
            for (String comp : comps) {
                if ('#' == comp.charAt(0) || '*' == comp.charAt(0)) {
                    // ничего не делаем, это спец. символ
                } else {
                    Set<TemplateMatcher> templateMatchers = templatesCommon.get(comp);
                    if (templateMatchers != null) {
                        templateMatchers.remove(templateMatcher);
                        if (templateMatchers.isEmpty()) {
                            Set<TemplateMatcher> deleted = templatesCommon.remove(comp);
                            if (!deleted.isEmpty()) {
                                LOGGER.error("Удалили не пустой набор матчеров!" + template);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean matchToKey(String key, String[] comps) {

        /**
         * Шаблоны без слов, в которых есть символ решётки.
         * В таких ключём выступает число звёзд - минимальное число слов.
         * Так если передан ключ 1.2.3.4, то ему подходят любые шаблоны,
         * с числом звёзд меньшим или равным 4:
         * #, #.*, #.*.*, #.*.*, #.*.*.*, #.*.*.*.*
         *
         */
        if (hasTemplatesWithoutWordsWithHash) {
            for (int i = 0; i <= comps.length; i++) {
                if (templatesWithoutWordsWithHash.get(i) != null) {
                    return true;
                }
            }
        }

        if (templatesOnlyWords.contains(key)) {
            return true;
        }

        if (hasTemplatesWithoutWordsStarsOnly &&
            templatesWithoutWordsStarsOnly.containsKey(comps.length)) {
            return true;
        }

        for (String comp : comps) {
            Set<TemplateMatcher> templateMatchers = templatesCommon.get(comp);
            if (templateMatchers != null) {
                for (TemplateMatcher matcher : templateMatchers) {
                    if (matcher.matchTo(comps)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
