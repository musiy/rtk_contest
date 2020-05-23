package rtk_contest.server;

import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс описывает потребителя - его стрим и шаблоны на которые он подписан.
 */
class ConsumerData implements Comparable<ConsumerData> {

    private static final AtomicInteger CONSUMER_ENUMERATOR = new AtomicInteger(1);

    /**
     * порядковый номер потребителя
     */
    private final int num = CONSUMER_ENUMERATOR.incrementAndGet();
    /**
     * Стрим для отправки данных консюмеру
     */
    final StreamObserver<Mbproto.ConsumeResponse> responseObserver;

    private final TemplateManager templateManager = new TemplateManager();

    public ConsumerData(StreamObserver<Mbproto.ConsumeResponse> responseObserver) {
        this.responseObserver = responseObserver;
    }

    public TemplateManager getTemplateManager() {
        return templateManager;
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
}
