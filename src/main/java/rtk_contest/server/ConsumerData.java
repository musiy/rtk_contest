package rtk_contest.server;

import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс описывает потребителя - его стрим и шаблоны на которые он подписан.
 */
public class ConsumerData implements Comparable<ConsumerData> {

    private static final AtomicInteger CONSUMER_ENUMERATOR = new AtomicInteger(1);

    /**
     * порядковый номер потребителя
     */
    private final int num = CONSUMER_ENUMERATOR.getAndIncrement();
    /**
     * Стрим для отправки данных консюмеру
     */
    final StreamObserver<Mbproto.ConsumeResponse> responseObserver;

    public ConsumerData(StreamObserver<Mbproto.ConsumeResponse> responseObserver) {
        this.responseObserver = responseObserver;
    }

    public int getNum() {
        return num;
    }

    @Override
    public boolean equals(Object o) {
        ConsumerData that = (ConsumerData) o;
        return num == that.num;
    }

    @Override
    public int hashCode() {
        return num;
    }

    @Override
    public int compareTo(ConsumerData that) {
        return that.num - num;
    }

    public void send(Mbproto.ConsumeResponse response) {
        GlobalSearchContext.outputStreamQueue.add(new OutputStreamProcessor.Addressing(this, response));
    }
}
