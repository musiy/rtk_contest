package rtk_contest.server;

import io.grpc.stub.StreamObserver;
import mbproto.Mbproto;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Класс описывает потребителя - его стрим и шаблоны на которые он подписан.
 */
class ConsumerData implements Comparable<ConsumerData> {

    static final AtomicInteger CONSUMER_ENUMERATOR = new AtomicInteger(1);
    /**
     * порядковый номер потребителя
     */
    private final int num = CONSUMER_ENUMERATOR.incrementAndGet();
    /**
     * Стрим для отправки данных консюмеру
     */
    private final StreamObserver<Mbproto.ConsumeResponse> responseObserver;
    /**
     * Набор шаблонов консюмера
     */
    private final ConcurrentSkipListSet<String> templates = new ConcurrentSkipListSet<>();

    public ConsumerData(StreamObserver<Mbproto.ConsumeResponse> responseObserver) {
        this.responseObserver = responseObserver;
    }

    void addTemplate(String template) {
        templates.add(template);
    }

    void removeTemplate(String template) {
        templates.remove(template);
    }

    public ConcurrentSkipListSet<String> getTemplates() {
        return templates;
    }

    public void onNext(Mbproto.ConsumeResponse response) {
        responseObserver.onNext(response);
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
