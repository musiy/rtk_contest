package rtk_contest.templating;

import rtk_contest.server.ConsumerData;

public class TemplateMatcherImpl extends BaseMatcher {

    // Хранит компоненты шаблона
    private final String[] templateComps;

    public TemplateMatcherImpl(ConsumerData consumerData, String template, String[] templateComps) {
        super(consumerData, template);
        // работа со строками - это время и память, работаем с массивом символов шаблона
        this.templateComps = templateComps;
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
            return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos)
                   || internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1)
                   || internalMatch(templateCompsPos, keyComps, keyCompsPos + 1);
        } else if ("*".equals(templateComp)) {
            return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
        } else if (keyComps[keyCompsPos].equals(templateComp)) {
            return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
        }
        return false;
    }
}
