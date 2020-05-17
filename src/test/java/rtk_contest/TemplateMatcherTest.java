package rtk_contest;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateMatcherTest {

    @Test
    void test_repeated() {
        TemplateMatcher templateMatcher = new TemplateMatcher("#.3.*");
        assertTrue(templateMatcher.matchTo("3.3.3.3"));
        assertTrue(templateMatcher.matchTo("3.3"));
        assertFalse(templateMatcher.matchTo("4.3"));
        assertFalse(templateMatcher.matchTo("3.4.4"));
    }

    @Test
    void test_h() {
        TemplateMatcher templateMatcher = new TemplateMatcher("#");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.two.three"));
    }

    @Test
    void test_s() {
        TemplateMatcher templateMatcher = new TemplateMatcher("*");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("two"));
        assertFalse(templateMatcher.matchTo("one.two"));
    }

    @Test
    void test_one_s() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.*");
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.three"));
        assertTrue(templateMatcher.matchTo("one.four"));
        assertFalse(templateMatcher.matchTo("one"));
        assertFalse(templateMatcher.matchTo("two"));
        assertFalse(templateMatcher.matchTo("two.one"));
    }

    @Test
    void test_one_h() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.#");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.two.three"));
        assertFalse(templateMatcher.matchTo("two"));
        assertFalse(templateMatcher.matchTo("two.one"));
    }

    @Test
    void test_s_one() {
        TemplateMatcher templateMatcher = new TemplateMatcher("*.one");
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
        TemplateMatcher templateMatcher = new TemplateMatcher("#.one");
        assertTrue(templateMatcher.matchTo("one"));
        assertTrue(templateMatcher.matchTo("two.one"));
        assertTrue(templateMatcher.matchTo("three.two.one"));
        assertFalse(templateMatcher.matchTo("one.two"));
        assertFalse(templateMatcher.matchTo("one.one.two"));
    }

    @Test
    void test_one_s_two() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.*.two");
        assertTrue(templateMatcher.matchTo("one.three.two"));
        assertTrue(templateMatcher.matchTo("one.four.two"));
        assertTrue(templateMatcher.matchTo("one.five.two"));
        assertFalse(templateMatcher.matchTo("one.two"));
        assertFalse(templateMatcher.matchTo("two.one.two"));
    }

    @Test
    void test_one_h_two() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.#.two");
        assertTrue(templateMatcher.matchTo("one.two"));
        assertTrue(templateMatcher.matchTo("one.three.two"));
        assertTrue(templateMatcher.matchTo("one.three.four.two"));
        assertFalse(templateMatcher.matchTo("two.two.two"));
    }

    @Test
    void test_one_s_s_two() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.*.*.two");
        assertTrue(templateMatcher.matchTo("one.three.four.two"));
        assertTrue(templateMatcher.matchTo("one.five.six.two"));
        assertFalse(templateMatcher.matchTo("one.three.two"));
        assertFalse(templateMatcher.matchTo("two.one.three.two"));
    }

    @Test
    void test_one_s_two_s() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.*.two.*");
        assertTrue(templateMatcher.matchTo("one.three.two.four"));
        assertTrue(templateMatcher.matchTo("one.four.two.five"));
        assertFalse(templateMatcher.matchTo("one.three.four"));
        assertFalse(templateMatcher.matchTo("one.three.four"));
        assertFalse(templateMatcher.matchTo("three.three.two.four"));
    }

    @Test
    void test_one_h_two_s() {
        TemplateMatcher templateMatcher = new TemplateMatcher("one.#.two.*");
        assertTrue(templateMatcher.matchTo("one.two.three"));
        assertTrue(templateMatcher.matchTo("one.three.two.four"));
        assertTrue(templateMatcher.matchTo("one.three.four.two.five"));
        assertTrue(templateMatcher.matchTo("one.one.two.three"));
        assertFalse(templateMatcher.matchTo("one.one.three"));
    }

}