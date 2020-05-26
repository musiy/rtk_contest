package rtk_contest.templating;

import rtk_contest.server.ConsumerData;

import java.util.ArrayDeque;
import java.util.Deque;

public class TemplateMatcherImproved extends BaseMatcher {

    private final String[] comps;

    public TemplateMatcherImproved(ConsumerData consumerData, String template, String[] comps) {
        super(consumerData, template);
        this.comps = comps;
    }

    @Override
    public boolean matchTo(String[] keyComps) {
        return internalMatch(keyComps);
    }

    @Override
    public ConsumerData getConsumerData() {
        return null;
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

        Deque<Integer[]> stack = new ArrayDeque<>();

        int pos = 0;
        int keyPos = 0;

        boolean takeFromStackOrFail = false;

        while (true) {
            if (takeFromStackOrFail) {
                Integer[] elem = stack.poll();
                if (elem == null) {
                    return false;
                }
                pos = elem[0];
                keyPos = elem[1];
                takeFromStackOrFail = false;
            }

            // выбрали все компоненты из ключа и шаблона
            if (keyPos == keyComps.length && pos == comps.length) {
                return true;
            }
            // если дошёл указатель по ключу - для матчинга оставшиейся в шаблоне могут быть только решётками
            if (keyPos == keyComps.length) {
                boolean allHash = true;
                for (int i = pos; i < comps.length; i++) {
                    if (comps[i].charAt(0) != '#') {
                        allHash = false;
                        break;
                    }
                }
                return allHash;
            }
            if (pos == comps.length) {
                takeFromStackOrFail = true;
                continue;
            }

            if (comps[pos].charAt(0) == '#') {
                // приоритет #1: режим пропуска - указатель в шаблоне увеличиваем,
                //                                указатель в ключе оставляем без изменений
                // съедаем 1-к-1, т.е. рассматриваем '#' как '*'
                stack.addFirst(new Integer[]{pos + 1, keyPos + 1});
                // расширение до '#.*' (самое дорогое вычисление - отлкадываем)
                stack.addLast(new Integer[]{pos, keyPos + 1});
                pos++;
            } else if (comps[pos].charAt(0) == '*') {
                // звезда подходит любому слову
                pos++;
                keyPos++;
            } else {
                // здесь в компоненте шаблона - слово, просто сравниваем
                if (comps[pos].equals(keyComps[keyPos])) {
                    pos++;
                    keyPos++;
                } else {
                    takeFromStackOrFail = true;
                }
            }
        }
    }
}
