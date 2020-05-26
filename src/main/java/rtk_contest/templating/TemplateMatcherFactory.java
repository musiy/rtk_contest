package rtk_contest.templating;

import rtk_contest.server.ConsumerData;

public class TemplateMatcherFactory {

    public static TemplateMatcher getByTemplate(ConsumerData consumerData, String template, String[] comps) {
        TemplateMatcher templateMatcher = internalGetByTemplate(consumerData, template, comps);
        if (templateMatcher == null) {
            templateMatcher = new TemplateMatcherImproved(consumerData, template, comps);
        }
        return templateMatcher;
    }

    static TemplateMatcher internalGetByTemplate(ConsumerData consumerData, String template, String[] comps) {

        boolean hasWord = false;
        boolean hasHash = false;
        int starsCount = 0;
        for (String comp : comps) {
            if ('#' == comp.charAt(0)) {
                hasHash = true;
            } else if ('*' == comp.charAt(0)) {
                starsCount++;
            } else {
                hasWord = true;
            }
        }

        if (!hasWord) {
            return new SpecTemplateMatcher(consumerData, template, hasHash, starsCount);
        }

        if (comps.length == 1) {
            if (comps[0].charAt(0) == '#') {
                return new SingleHashMatcher(consumerData);
            } else if (comps[0].charAt(0) == '*') {
                return new SingleStarMatcher(consumerData);
            } else {
                return new SingleWordMatcher(consumerData, comps[0]);
            }
        } else if (comps.length == 2) {
            boolean pos1IsWord = false;
            boolean pos2IsWord = false;
            if (!(comps[0].charAt(0) == '#' || comps[0].charAt(0) == '*')) {
                pos1IsWord = true;
            }
            if (!(comps[1].charAt(0) == '#' || comps[1].charAt(0) == '*')) {
                pos2IsWord = true;
            }
            // W.W
            if (pos1IsWord && pos2IsWord) {
                return new DoubleWordMatcher(consumerData, template, comps[0], comps[1]);
            }
            // W.*
            // W.#
            if (pos1IsWord) {
                return new TwoCompsMatcher_First_W(consumerData, template, comps[0], comps[1].charAt(0));
            }
            // *.W
            // #.W
            if (pos2IsWord) {
                return new TwoCompsMatcher_Second_W(consumerData, template, comps[0].charAt(0), comps[1]);
            }
            // варианты ниже здесь не рассматриваем - это особые случаи
            // #.#
            // *.*
            // #.*
            // *.#
        } else if (comps.length == 3) {
            boolean pos1IsWord = false;
            boolean pos2IsWord = false;
            boolean pos3IsWord = false;
            if (!(comps[0].charAt(0) == '#' || comps[0].charAt(0) == '*')) {
                pos1IsWord = true;
            }
            if (!(comps[1].charAt(0) == '#' || comps[1].charAt(0) == '*')) {
                pos2IsWord = true;
            }
            if (!(comps[2].charAt(0) == '#' || comps[2].charAt(0) == '*')) {
                pos3IsWord = true;
            }
            // W.W.W
            if (pos1IsWord && pos2IsWord && pos3IsWord) {
                return new TrippleCompMatcherAllWords(consumerData, template, comps[0], comps[1], comps[2]);
            }
            // W.W.?
            // W.?.W
            // ?.W.W
            if ((pos1IsWord && pos2IsWord) || (pos1IsWord && pos3IsWord) || (pos2IsWord && pos3IsWord)) {
                return new TrippleCompMatcherTwoWords(consumerData, template, comps, pos1IsWord, pos2IsWord, pos3IsWord);
            }

            // W.?.?
            // ?.W.?
            // ?.?.W
            if (pos1IsWord || pos2IsWord || pos3IsWord) {
                return new TrippleCompMatcherOneWord(consumerData, template, comps, pos1IsWord, pos2IsWord, pos3IsWord);
            }
        }
        return null;
    }

    static class SingleHashMatcher extends BaseMatcher {

        public SingleHashMatcher(ConsumerData consumerData) {
            super(consumerData, "#");
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            return true;
        }
    }

    static class SingleStarMatcher extends BaseMatcher {

        public SingleStarMatcher(ConsumerData consumerData) {
            super(consumerData, "*");
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            return keyComps.length == 1;
        }
    }

    static class SingleWordMatcher extends BaseMatcher {

        private final String word;

        SingleWordMatcher(ConsumerData consumerData, String word) {
            super(consumerData, word);
            this.word = word;
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            return keyComps.length == 1 && keyComps[0].equals(word);
        }
    }

    /**
     * Два компонента
     */

    static class DoubleWordMatcher extends BaseMatcher {

        private final String word1;
        private final String word2;

        DoubleWordMatcher(ConsumerData consumerData, String template, String word1, String word2) {
            super(consumerData, template);
            this.word1 = word1;
            this.word2 = word2;
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            return keyComps.length == 2
                   && keyComps[0].equals(word1) && keyComps[1].equals(word2);
        }
    }

    static class TwoCompsMatcher_First_W extends BaseMatcher {
        private final String word;
        private final char sym;

        public TwoCompsMatcher_First_W(ConsumerData consumerData, String template, String word, char sym) {
            super(consumerData, template);
            this.word = word;
            this.sym = sym;
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            if (sym == '#') {
                return keyComps[0].equals(word);
            } else {
                return keyComps.length == 2 && keyComps[0].equals(word);
            }
        }
    }

    static class TwoCompsMatcher_Second_W extends BaseMatcher {
        private final String word;
        private final char sym;

        public TwoCompsMatcher_Second_W(ConsumerData consumerData, String template, char sym, String word) {
            super(consumerData, template);
            this.word = word;
            this.sym = sym;

        }

        @Override
        public boolean matchTo(String[] keyComps) {
            if (sym == '#') {
                return keyComps[keyComps.length - 1].equals(word);
            } else {
                return keyComps.length == 2 && keyComps[1].equals(word);
            }
        }
    }

    /**
     * Три компонента
     */
    static class TrippleCompMatcherAllWords extends BaseMatcher {

        private final String w1;
        private final String w2;
        private final String w3;

        public TrippleCompMatcherAllWords(ConsumerData consumerData, String template, String w1, String w2, String w3) {
            super(consumerData, template);
            this.w1 = w1;
            this.w2 = w2;
            this.w3 = w3;
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            return keyComps.length == 3
                   && keyComps[0].equals(w1) && keyComps[1].equals(w2) && keyComps[2].equals(w3);
        }
    }

    // W.W.?
    // W.?.W
    // ?.W.W
    static class TrippleCompMatcherTwoWords extends BaseMatcher {
        private final int posW1;
        private final int posW2;
        private final int posSym;

        private final String wA;
        private final String wB;
        private final char sym;

        public TrippleCompMatcherTwoWords(ConsumerData consumerData, String template,
                                          String[] comps, boolean pos1IsWord, boolean pos2IsWord, boolean pos3IsWord) {
            super(consumerData, template);
            if (!pos1IsWord) {
                posSym = 0;
                posW1 = 1;
                posW2 = 2;
            } else if (!pos2IsWord) {
                posW1 = 0;
                posSym = 1;
                posW2 = 2;
            } else {
                posW1 = 0;
                posW2 = 1;
                posSym = 2;
            }
            wA = comps[posW1];
            wB = comps[posW2];
            sym = comps[posSym].charAt(0);
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            if (keyComps.length < 2) {
                return false;
            }
            if (posSym == 0) {
                if (!(keyComps[keyComps.length - 2].equals(wA) && keyComps[keyComps.length - 1].equals(wB))) {
                    return false;
                }
            } else if (posSym == 1) {
                if (!(keyComps[0].equals(wA) && keyComps[keyComps.length - 1].equals(wB))) {
                    return false;
                }
            } else {
                if (!(keyComps[0].equals(wA) && keyComps[1].equals(wB))) {
                    return false;
                }
            }
            if (sym == '#') {
                return true;
            }
            return keyComps.length == 3;
        }
    }

    static class TrippleCompMatcherOneWord extends BaseMatcher {

        private final int posSym1;
        private final char sym1;
        private final int posSym2;
        private final char sym2;
        private final int posWord;
        private final String word;


        public TrippleCompMatcherOneWord(ConsumerData consumerData, String template, String[] comps,
                                         boolean pos1IsWord, boolean pos2IsWord, boolean pos3IsWord) {
            super(consumerData, template);
            if (pos1IsWord) {
                posWord = 0;
                posSym1 = 1;
                posSym2 = 2;
            } else if (pos2IsWord) {
                posSym1 = 0;
                posWord = 1;
                posSym2 = 2;
            } else {
                posSym1 = 0;
                posSym2 = 1;
                posWord = 2;
            }
            sym1 = comps[posSym1].charAt(0);
            sym2 = comps[posSym2].charAt(0);
            word = comps[posWord];
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            if (posWord == 0) {
                if (keyComps[0].equals(word)) {
                    if (sym1 == '#' && sym2 == '#') {
                        return true;
                    } else if ((sym1 == '#' && sym2 == '*') || (sym1 == '*' && sym2 == '#')) {
                        return keyComps.length >= 2;
                    } else {
                        return keyComps.length == 3;
                    }
                }
            } else if (posWord == 1) {
                if (sym1 == '#' && sym2 == '#') {
                    for (String keyComp : keyComps) {
                        if (keyComp.equals(word)) {
                            return true;
                        }
                    }
                } else if (sym1 == '*' && sym2 == '#') {
                    if (keyComps.length < 2) {
                        return false;
                    }
                    return keyComps[1].equals(word);
                } else if (sym1 == '#' && sym2 == '*') {
                    if (keyComps.length < 2) {
                        return false;
                    }
                    return keyComps[keyComps.length - 2].equals(word);
                } else if (sym1 == '*' && sym2 == '*') {
                    return keyComps.length == 3 && keyComps[1].equals(word);
                } else {
                    throw new RuntimeException("Ошибка");
                }
            } else {
                if (keyComps[keyComps.length - 1].equals(word)) {
                    if (sym1 == '#' && sym2 == '#') {
                        return true;
                    } else if ((sym1 == '#' && sym2 == '*') || (sym1 == '*' && sym2 == '#')) {
                        return keyComps.length >= 2;
                    } else {
                        return keyComps.length == 3;
                    }
                }
            }
            return false;
        }
    }

    static class SpecTemplateMatcher extends BaseMatcher {

        private final boolean hasHash;
        private final int starsCount;

        public SpecTemplateMatcher(ConsumerData consumerData, String template, boolean hasHash, int starsCount) {
            super(consumerData, template);
            this.hasHash = hasHash;
            this.starsCount = starsCount;
        }

        @Override
        public boolean matchTo(String[] keyComps) {
            if (hasHash) {
                return keyComps.length >= starsCount;
            } else {
                return keyComps.length == starsCount;
            }
        }
    }

}
