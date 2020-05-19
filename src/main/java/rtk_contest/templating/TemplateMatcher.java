package rtk_contest.templating;

public class TemplateMatcher {

    // Хранит компоненты шаблона
    private final String[] templateComps;

    public TemplateMatcher(String template) {
        // работа со строками - это время и память, работаем с массивом символов шаблона
        this.templateComps = template.split("\\.");
    }

    /**
     * Сопоставляет ключ шаблону
     *
     * @param key
     * @return
     */
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
        if (templateCompsPos == templateComps.length && keyCompsPos == keyComps.length) {
            return true;
        }
        if (templateCompsPos == templateComps.length && keyCompsPos < keyComps.length) {
            return false;
        }
        String templComp = templateComps[templateCompsPos];
        // если это последний оставшийся компонент шаблона - значит оставшиеся компоненты должны подходить ему
        if (templateCompsPos + 1 == templateComps.length) {
            return fitTo(templComp, keyComps, keyCompsPos);
        }
        // в шаблоне ещё остались компоненты

        // # подходит ко всем ключам - поэтому вызываем проверку с оставшимися ключами
        // Например: для 3.3.3.3, метод будет вызван три раза:
        // internalMatch(1, keyComps, 1)
        // internalMatch(1, keyComps, 2)
        // internalMatch(1, keyComps, 3)
        if ("#".equals(templComp)) {
            for (int i = keyCompsPos; i < keyComps.length; i++) {
                if (internalMatch(templateCompsPos + 1, keyComps, i)) {
                    // если хотя бы один подходит - выходим
                    return true;
                }
            }
            return false;
        }
        // * это любой ключ который должен быть в позиции keyCompsPos
        else if ("*".equals(templComp)) {
            return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
        }
        // указан точный ключ
        else {
            if (templComp.equals(keyComps[keyCompsPos])) {
                return internalMatch(templateCompsPos + 1, keyComps, keyCompsPos + 1);
            }
            return false;
        }
    }

    private boolean fitTo(String comp, String[] keyComps, int keyCompsPos) {
        // если текущий компонент это '#', то любые оставшиеся ключи (в т.ч. отсутствие ключей) - удовлетворяет
        if ("#".equals(comp)) {
            return true;
        }
        // если текущий компонент это '*', то ему может удовлетворять только один оставшийся ключ (любой)
        if ("*".equals(comp)) {
            return keyCompsPos + 1 == keyComps.length;
        }
        // если задан конкретный ключ, то он должен точно совпадать с оставшимся
        return (keyCompsPos + 1 == keyComps.length) && keyComps[keyCompsPos].equals(comp);
    }

}
