package rtk_contest.templating;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateMatcherImplTest {

    @Test
    void test() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("*.*.*");
        assertFalse(templateMatcher.matchTo("1.2.3.4"));
    }

    @Test
    void test_repeated() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("#.3.*");
        assertTrue(templateMatcher.matchTo("3.3.3.3"));
        assertTrue(templateMatcher.matchTo("3.3"));
        assertFalse(templateMatcher.matchTo("4.3"));
        assertFalse(templateMatcher.matchTo("3.4.4"));
    }

    @Test
    void test_h() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("#");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.two.three"));
    }

    @Test
    void test_s() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("*");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("two"));
        assertFalse(templateMatcher.matchTo("one.two"));
    }

    @Test
    void test_one_s() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.*");
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.three"));
        assertTrue(templateMatcher.matchTo("one.four"));
        assertFalse(templateMatcher.matchTo("one"));
        assertFalse(templateMatcher.matchTo("two"));
        assertFalse(templateMatcher.matchTo("two.one"));
    }

    @Test
    void test_one_h() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.#");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.two.three"));
        assertFalse(templateMatcher.matchTo("two"));
        assertFalse(templateMatcher.matchTo("two.one"));
    }

    @Test
    void test_s_one() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("*.one");
        assertTrue(templateMatcher.matchTo("one.one"));
        assertTrue(templateMatcher.matchTo("two.one"));
        assertTrue(templateMatcher.matchTo("two.one"));
        assertTrue(templateMatcher.matchTo("four.one"));
        assertFalse(templateMatcher.matchTo("one"));
        assertFalse(templateMatcher.matchTo("one.two"));
        assertFalse(templateMatcher.matchTo("one.one.one"));
    }

    @Test
    void test_h_one() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("#.one");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("two.one"));
        assertTrue(templateMatcher.matchTo("three.two.one"));
        assertFalse(templateMatcher.matchTo("one.two"));
        assertFalse(templateMatcher.matchTo("one.one.two"));
    }

    @Test
    void test_one_s_two() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.*.two");
        assertTrue(templateMatcher.matchTo("one.three.two"));
        assertTrue(templateMatcher.matchTo("one.four.two"));
        assertTrue(templateMatcher.matchTo("one.five.two"));
        assertFalse(templateMatcher.matchTo("one.two"));
        assertFalse(templateMatcher.matchTo("two.one.two"));
    }

    @Test
    void test_one_h_two() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.#.two");
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.three.two"));
        assertTrue(templateMatcher.matchTo("one.three.four.two"));
        assertFalse(templateMatcher.matchTo("two.two.two"));
    }

    @Test
    void test_one_s_s_two() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.*.*.two");
        assertTrue(templateMatcher.matchTo("one.three.four.two"));
        assertTrue(templateMatcher.matchTo("one.five.six.two"));
        assertFalse(templateMatcher.matchTo("one.three.two"));
        assertFalse(templateMatcher.matchTo("two.one.three.two"));
    }

    @Test
    void test_one_s_two_s() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.*.two.*");
        assertTrue(templateMatcher.matchTo("one.three.two.four"));
        assertTrue(templateMatcher.matchTo("one.four.two.five"));
        assertFalse(templateMatcher.matchTo("one.three.four"));
        assertFalse(templateMatcher.matchTo("one.three.four"));
        assertFalse(templateMatcher.matchTo("three.three.two.four"));
    }

    @Test
    void test_one_h_two_s() {
        TemplateMatcherImpl templateMatcher = new TemplateMatcherImpl("one.#.two.*");
        assertTrue(templateMatcher.matchTo("one.two.three"));
        assertTrue(templateMatcher.matchTo("one.three.two.four"));
        assertTrue(templateMatcher.matchTo("one.three.four.two.five"));
        assertTrue(templateMatcher.matchTo("one.one.two.three"));
        assertFalse(templateMatcher.matchTo("one.one.three"));
    }

}