package rtk_contest.templating;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;

public class TemplateMatcherImproved implements TemplateMatcher {

    private final String[] comps;

    public TemplateMatcherImproved(String[] comps) {
        this.comps = comps;
    }

    public TemplateMatcherImproved(String template) {
        this.comps = StringHelper.split(template);
    }

    @Override
    public boolean matchTo(String[] keyComps) {
        return internalMatch(keyComps);
    }

    /**
     * Реализация логики матчинга ключа по шаблону
     *
     * @param keyComps компоненты ключа
     * @return ключ подходит шаблону
     */
    private boolean internalMatch(String[] keyComps) {
        // todo оптимизация - вместо new Integer[]{pos, keyPos, 2}
        //      можно хранить в разрядах int: по байту на число - 3 разряда

        Queue<Integer[]> stack = new PriorityQueue<>(Comparator.comparingInt(o -> o[2]));

        int pos = 0;
        int keyPos = 0;

        // 1 - пропуск (skip) '#'
        // 2 - сопоставление - как одна зведа '*'
        // 3 - расширение до '#.*'
        // начинаем со SKIP, как наиболее дешёвого
        int mode = 1;

        do {
            // выбрали все компоненты из ключа и шаблона
            if (keyPos == keyComps.length && pos == comps.length) {
                return true;
            }
            // если дошёл только один - достаём из очереди и возвращаемся на обработку
            if (keyPos == keyComps.length || pos == comps.length) {
                if (pos == comps.length - 1 && comps[pos].charAt(0) == '#') {
                    return true;
                }
                Integer[] data = stack.poll();
                if (data == null) {
                    return false;
                }
                pos = data[0];
                keyPos = data[1];
                continue;
            }
            if (comps[pos].charAt(0) == '#') {
                // режим пропуска - указатель в шаблоне увеличиваем, указатель в ключе оставляем без изменений
                // съедаем 1-к-1, т.е. рассматриваем '#' как '*'
                stack.add(new Integer[]{pos + 1, keyPos + 1, 1});
                // расширение до '#.*' (самое дорогое вычисление - отлкадываем)
                stack.add(new Integer[]{pos, keyPos + 1, 2});
                pos++;
            } else if (comps[pos].charAt(0) == '*') {
                // звезда подходит любому слову
                pos++;
                keyPos++;
            } else {
                // здесь в компоненте шаблона - слово, просто сравниваем
                if (!comps[pos].equals(keyComps[keyPos])) {
                    Integer[] data = stack.poll();
                    if (data == null) {
                        return false;
                    }
                    pos = data[0];
                    keyPos = data[1];
                    continue;
                }
                pos++;
                keyPos++;
            }
        } while (true);
    }

}
