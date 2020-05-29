package rtk_contest.server;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import org.junit.jupiter.api.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.BitSet;

@RunWith(JUnitPlatform.class)
class GlobalSearchContextTest {

    @Test
    public void test() {
        StreamObserver<Mbproto.ConsumeResponse> responseObserver = Mockito.mock(StreamObserver.class);
        ConsumerData consumerData = new ConsumerData(responseObserver);
        GlobalSearchContext.addConsumer(consumerData);
        GlobalSearchContext.addTemplate(consumerData, "one.#");
        GlobalSearchContext.addTemplate(consumerData, "one");
        GlobalSearchContext.addTemplate(consumerData, "one.*");
        GlobalSearchContext.addTemplate(consumerData, "one.two");
        GlobalSearchContext.removeTemplate(consumerData, "one");
        Mbproto.ConsumeResponse response = Mbproto.ConsumeResponse.newBuilder()
                .setKey("test")
                .setPayload(ByteString.copyFromUtf8("test"))
                .build();
        GlobalSearchContext.matchToAndSend(response, "one");
    }

    @Test
    public void testBitSet() {
        BitSet source = new BitSet(4);
        source.set(0, false);
        source.set(1, false);
        source.set(2, true);
        source.set(3, true);

        BitSet param = new BitSet(4);
        param.set(0, false);
        param.set(1, true);
        param.set(2, false);
        param.set(3, true);

        BitSet work = (BitSet) source.clone();
        work.xor(param);
        work.and(param);


        BigInteger sourceBi = new BigInteger(source.toByteArray());

        System.out.println(1 <<  1);
        System.out.println(1 >> 2);
    }

}