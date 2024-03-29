package rtk_contest.templating;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import rtk_contest.server.ConsumerData;
import rtk_contest.server.GlobalSearchContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

public class TemplateMatcherImplTests {

    private static void shouldTryMatch(String template, String key) {
        String[] templateComps = StringHelper.split(template);
        String[] keyComps = StringHelper.split(key);
        TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(template, templateComps);
        assertTrue(templateMatcher.matchTo(keyComps));

        GlobalSearchContext.testInit();
        StreamObserver<Mbproto.ConsumeResponse> responseObserver = Mockito.mock(StreamObserver.class);
        ConsumerData consumerData = new ConsumerData(responseObserver);
        consumerData = Mockito.spy(consumerData);
        GlobalSearchContext.addConsumer(consumerData);
        GlobalSearchContext.addTemplate(consumerData, template);
        Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                .setKey(key)
                .setPayload(ByteString.copyFromUtf8(key))
                .build();
        GlobalSearchContext.matchToAndSend(key, response.getPayload());
        ArgumentCaptor<Mbproto.ConsumeResponse> argumentCaptor = ArgumentCaptor.forClass(Mbproto.ConsumeResponse.class);
        Mockito.verify(consumerData).send(argumentCaptor.capture());
        assertSame(response.getKey(), argumentCaptor.getValue().getKey());
        assertSame(response.getPayload(), argumentCaptor.getValue().getPayload());
    }

    private static void shouldNotMatch(String template, String key) {
        String[] templateComps = StringHelper.split(template);
        String[] keyComps = StringHelper.split(key);
        TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate(template, templateComps);
        assertFalse(templateMatcher.matchTo(keyComps));

        GlobalSearchContext.testInit();
        StreamObserver<Mbproto.ConsumeResponse> responseObserver = Mockito.mock(StreamObserver.class);
        ConsumerData consumerData = new ConsumerData(responseObserver);
        consumerData = Mockito.spy(consumerData);
        GlobalSearchContext.addConsumer(consumerData);
        GlobalSearchContext.addTemplate(consumerData, template);
        Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                .setKey("test")
                .setPayload(ByteString.copyFromUtf8("test"))
                .build();
        GlobalSearchContext.matchToAndSend(key, response.getPayload());
        Mockito.verify(consumerData, Mockito.never()).send(any());
    }

    // Один компонент

    @Test
    void test_h() {
        shouldTryMatch("#", "one");
        shouldTryMatch("#", "one.two");
        shouldTryMatch("#", "one.two.three");
    }

    @Test
    void test_s() {
        shouldTryMatch("*", "one");
        shouldNotMatch("*", "one.two");
        shouldNotMatch("*", "one.two.three");
    }

    @Test
    void test_w() {
        shouldTryMatch("one", "one");
        shouldNotMatch("one", "two");
        shouldNotMatch("one", "one.two");
    }

    // Два компонента

    @Test
    void test_w_w() {
        shouldTryMatch("one.two", "one.two");
        shouldNotMatch("one.two", "one");
        shouldNotMatch("one.two", "two");
        shouldNotMatch("one.two", "one.two.two");
        shouldNotMatch("one.two", "one.one");
    }

    @Test
    void test_w_s() {
        shouldTryMatch("one.*", "one.two");
        shouldNotMatch("one.*", "one");
        shouldNotMatch("one.*", "two");
        shouldNotMatch("one.*", "two.one");
        shouldNotMatch("one.*", "one.two.three");
    }

    @Test
    void test_w_h() {
        shouldTryMatch("one.#", "one");
        shouldTryMatch("one.#", "one.two");
        shouldTryMatch("one.#", "one.two.three");
        shouldNotMatch("one.#", "two");
        shouldNotMatch("one.#", "two.one");
        shouldNotMatch("one.#", "two.one.three");
    }

    @Test
    void test_s_w() {
        shouldTryMatch("*.one", "two.one");
        shouldNotMatch("*.one", "one");
        shouldNotMatch("*.one", "one.two");
        shouldNotMatch("*.one", "one.two.one");
    }

    @Test
    void test_h_w() {
        shouldTryMatch("#.one", "one");
        shouldTryMatch("#.one", "two.one");
        shouldTryMatch("#.one", "one.two.one");
        shouldNotMatch("#.one", "two");
        shouldNotMatch("#.one", "one.two");
        shouldNotMatch("#.one", "one.two.three");
    }

    // три компонента

    @Test
    void test_w_w_w() {
        shouldTryMatch("one.two.three", "one.two.three");
        shouldNotMatch("one.two.three", "one");
        shouldNotMatch("one.two.three", "one.two");
        shouldNotMatch("one.two.three", "one.three");
        shouldNotMatch("one.two.three", "one.two.three.four");
        shouldNotMatch("one.two.three", "one.two.two.three");
    }

    @Test
    void test_w_w_s() {
        shouldTryMatch("one.two.*", "one.two.three");
        shouldNotMatch("one.two.*", "one.two");
        shouldNotMatch("one.two.*", "one.one.two");
        shouldNotMatch("one.two.*", "one.two.three.four");
    }

    @Test
    void test_w_w_h() {
        shouldTryMatch("one.two.#", "one.two");
        shouldTryMatch("one.two.#", "one.two.three");
        //shouldTryMatch("one.two.#", "one.two.three.four");
        shouldNotMatch("one.two.#", "one");
        shouldNotMatch("one.two.#", "one.one");
    }

    @Test
    void test_w_s_w() {
        shouldTryMatch("one.*.two", "one.three.two");
        shouldTryMatch("one.*.two", "one.four.two");
        shouldNotMatch("one.*.two", "one");
        shouldNotMatch("one.*.two", "one.two");
        shouldNotMatch("one.*.two", "two.one.two");
    }

    @Test
    void test_w_h_w() {
        shouldTryMatch("one.#.two", "one.two");
        shouldTryMatch("one.#.two", "one.three.two");
        //shouldTryMatch("one.#.two", "one.three.four.two");
        shouldNotMatch("one.#.two", "two.two.two");
        shouldNotMatch("one.#.two", "two.two");
    }

    @Test
    void test_s_w_w() {
        shouldTryMatch("*.one.two", "two.one.two");
        shouldNotMatch("*.one.two", "one.two");
        shouldNotMatch("*.one.two", "one.two.three");
        //shouldNotMatch("*.one.two", "two.one.two.three");
    }

    @Test
    void test_h_w_w() {
        shouldTryMatch("#.one.two", "three.one.two");
        shouldTryMatch("#.one.two", "one.two");
//        shouldTryMatch("#.one.two", "three.four.one.two");
//        shouldNotMatch("#.one.two", "three.four.two.two");
        shouldNotMatch("#.one.two", "one");
    }

    @Test
    void test_w_h_h() {
        shouldTryMatch("one.#.#", "one");
        shouldTryMatch("one.#.#", "one.two.three.four");
        shouldNotMatch("one.#.#", "two");
        shouldNotMatch("one.#.#", "two.one");
    }

    @Test
    void test_w_s_s() {
        shouldTryMatch("one.*.*", "one.two.three");
        shouldNotMatch("one.*.*", "two.one.three");
        //shouldNotMatch("one.*.*", "one.two.three.four");
        shouldNotMatch("one.*.*", "one");
        shouldNotMatch("one.*.*", "one.two");
    }

    @Test
    void test_w_s_h() {
        shouldTryMatch("one.*.#", "one.two");
        shouldTryMatch("one.*.#", "one.two.three.four");
        shouldNotMatch("one.*.#", "one");
        shouldNotMatch("one.*.#", "two.one.three.four");
    }

    @Test
    void test_w_h_s() {
        shouldTryMatch("one.#.*", "one.two");
        shouldTryMatch("one.#.*", "one.two.three.four.one");
        shouldNotMatch("one.#.*", "two.three.four.one");
    }

    @Test
    void test_h_w_h() {
        shouldTryMatch("#.one.#", "one");
        shouldTryMatch("#.one.#", "one.one");
        shouldTryMatch("#.one.#", "one.one.one.one");
        shouldTryMatch("#.one.#", "two.one.one.four");
        shouldTryMatch("#.one.#", "one.one.four");
        shouldTryMatch("#.one.#", "two.four.one");
        shouldTryMatch("#.one.#", "four.one");
        shouldTryMatch("#.one.#", "one.four.four");
        shouldNotMatch("#.one.#", "four.four");
    }

    @Test
    void test_h_w_s() {
        shouldTryMatch("#.one.*", "one.one.one.one");
        shouldTryMatch("#.one.*", "one.one");
        shouldNotMatch("#.one.*", "four.one");
        shouldNotMatch("#.one.*", "one.four.four");
    }

    @Test
    void test_s_w_h() {
        shouldTryMatch("*.one.#", "two.one.three.four");
        shouldTryMatch("*.one.#", "one.one");
        shouldNotMatch("*.one.#", "one.two");
        shouldNotMatch("*.one.#", "one.two.three");
    }

    @Test
    void test_s_w_s() {
        shouldTryMatch("*.one.*", "two.one.three");
        shouldNotMatch("*.one.*", "one");
        shouldNotMatch("*.one.*", "one.one");
        shouldNotMatch("*.one.*", "one.one.one.one");
    }

    @Test
    void test_h_h_w() {
        shouldTryMatch("#.#.one", "one");
        shouldTryMatch("#.#.one", "two.one");
        shouldTryMatch("#.#.one", "three.two.one");
        shouldTryMatch("#.#.one", "four.three.two.one");
        shouldNotMatch("#.#.one", "two");
        shouldNotMatch("#.#.one", "one.two");
    }

    @Test
    void test_s_h_w() {
        shouldTryMatch("*.#.one", "two.one");
        shouldTryMatch("*.#.one", "three.two.one");
        shouldTryMatch("*.#.one", "four.three.two.one");
        shouldNotMatch("*.#.one", "one");
        shouldNotMatch("*.#.one", "two");
        shouldNotMatch("*.#.one", "one.two");
    }

    @Test
    void test_h_s_w() {
        shouldTryMatch("#.*.one", "two.one");
        shouldTryMatch("#.*.one", "three.two.one");
        shouldTryMatch("#.*.one", "four.three.two.one");
        shouldNotMatch("#.*.one", "one");
        shouldNotMatch("#.*.one", "two");
        shouldNotMatch("#.*.one", "one.two");
    }

    @Test
    void test_s_s_w() {
        shouldTryMatch("*.*.one", "two.three.one");
        shouldNotMatch("*.*.one", "one");
        shouldNotMatch("*.*.one", "two.one");
        shouldNotMatch("*.*.one", "two");
        shouldNotMatch("*.*.one", "one.two");
        shouldNotMatch("*.*.one", "four.three.two.one");
    }

    // четыре и более компонентов


    @Test
    void test_one_s_s_two() {
        shouldTryMatch("one.*.*.two", "one.three.four.two");
        shouldTryMatch("one.*.*.two", "one.five.six.two");
        shouldNotMatch("one.*.*.two", "one.three.two");
        shouldNotMatch("one.*.*.two", "two.one.three.two");
        shouldNotMatch("one.*.*.two", "one.one.three.two.match");
    }

//    @Test
//    void test_one_s_two_s() {
//        TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate("one.*.two.*");
//        assertTrue(templateMatcher.matchTo("one.three.two.four"));
//        assertTrue(templateMatcher.matchTo("one.four.two.five"));
//        assertFalse(templateMatcher.matchTo("one.three.four"));
//        assertFalse(templateMatcher.matchTo("one.three.four"));
//        assertFalse(templateMatcher.matchTo("three.three.two.four"));
//    }
//
//    @Test
//    void test_one_h_two_s() {
//        TemplateMatcher templateMatcher = TemplateMatcherFactory.getByTemplate("one.#.two.*");
//        assertTrue(templateMatcher.matchTo("one.two.three"));
//        assertTrue(templateMatcher.matchTo("one.three.two.four"));
//        assertTrue(templateMatcher.matchTo("one.three.four.two.five"));
//        assertTrue(templateMatcher.matchTo("one.one.two.three"));
//        assertFalse(templateMatcher.matchTo("one.one.three"));
//    }

}