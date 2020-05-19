package rtk_contest.templating;

public class TemplateMatcherImpl implements TemplateMatcher {

    // Хранит компоненты шаблона
    private final String[] templateComps;

    public TemplateMatcherImpl(String template) {
        // работа со строками - это время и память, работаем с массивом символов шаблона
        this.templateComps = template.split("\\.");
    }

    /**
     * Сопоставляет ключ шаблону
     *
     * @param key
     * @return
     */
    @Override
    public boolean matchTo(String key) {
        int templateCompsPos = 0;
        String[] keyComps = key.split("\\.");
        return internalMatch(templateCompsPos, keyComps, 0);
    }

    /**
     * @param templateCompsPos позиция в шаблоне {@linkplain #templateComps}, 0 - позиция первого символа
     * @param keyComps         компоненты ключа
     * @param keyCompsPos      текущая позиция в компонентах ключа
     * @return
     */
    private boolean internalMatch(int templateCompsPos, String[] keyComps, int keyCompsPos) {
        if (keyCompsPos == keyComps.length) {
            if (templateCompsPos == templateComps.length) {
                return true;
            }
            if ((templateCompsPos == templateComps.length - 1)
                && (templateComps[templateCompsPos].equals("#"))) {
                return true;
            }
            return false;
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

}
