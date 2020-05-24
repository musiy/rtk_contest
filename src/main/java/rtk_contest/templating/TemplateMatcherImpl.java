package rtk_contest.templating;

import java.util.Objects;

public class TemplateMatcherImpl implements TemplateMatcher {

    // Хранит компоненты шаблона
    private final String[] templateComps;

    private final String template;

    public TemplateMatcherImpl(String template, String[] templateComps) {
        // работа со строками - это время и память, работаем с массивом символов шаблона
        this.template = template;
        this.templateComps = templateComps;
    }

    // todo только для теста
    TemplateMatcherImpl(String template) {
        // работа со строками - это время и память, работаем с массивом символов шаблона
        this.template = template;
        this.templateComps = StringHelper.split(template);
    }

    // todo только для теста
    public boolean matchTo(String key) {
        return matchTo(StringHelper.split(key));
    }

    @Override
    public boolean matchTo(String[] keyComps) {
        int templateCompsPos = 0;
        return internalMatch(templateCompsPos, keyComps, 0);
    }

    /**
     * @param templateCompsPos позиция в шаблоне {@linkplain #templateComps}, 0 - позиция первого символа
     * @param keyComps         компоненты ключа
     * @param keyCompsPos      текущая позиция в компонентах ключа
     * @return
     */
    private boolean internalMatch(int templateCompsPos, String[] keyComps, int keyCompsPos) {
        if ((templateCompsPos == templateComps.length - 1)
            && (templateComps[templateCompsPos].equals("#"))) {
            return true;
        }
        if (keyCompsPos == keyComps.length) {
            return templateCompsPos == templateComps.length;
        }
        if (templateCompsPos == templateComps.length) {
            return false;
        }
        String templateComp = templateComps[templateCompsPos];
        if ("#".equals(templateComp)) {
            return internalMatch(templateCompsPos, keyComps, keyCompsPos + 1)
                   || internalMatch(templateCompsPos + 1, keyComps, keyCompsPos)
                   || internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
        } else if ("*".equals(templateComp)) {
            return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
        } else if (keyComps[keyCompsPos].equals(templateComp)) {
            return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TemplateMatcherImpl that = (TemplateMatcherImpl) o;
        return Objects.equals(template, that.template);
    }

    @Override
    public int hashCode() {
        return Objects.hash(template);
    }
}
