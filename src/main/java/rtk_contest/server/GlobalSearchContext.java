package rtk_contest.server;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.StringHelper;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalSearchContext {

    static Logger LOGGER = LoggerFactory.getLogger(GlobalSearchContext.class);

    static final int MAX_CONSUMERS = 1_000;

    public static final BlockingQueue<OutputStreamProcessor.Addressing> outputStreamQueue = new ArrayBlockingQueue<>(30_000);

    static Set<ConsumerData> consumers = Sets.newConcurrentHashSet();
    static ConsumerData[] consumersByNum = new ConsumerData[MAX_CONSUMERS];
    static AtomicInteger consumersCount = new AtomicInteger();

    // todo посчитать сколько элементов на максимуме и этим значением инициализировать
    final static Map<String, Node> storage_l = new ConcurrentHashMap<>(50_000);
    final static Map<String, Node> storage_r = new ConcurrentHashMap<>(50_000);

    public static void testInit() {
        consumers.clear();
        consumersCount = new AtomicInteger(0);
        for (int i = 0; i < MAX_CONSUMERS; i++) {
            consumersByNum[i] = null;
        }
        storage_l.clear();
        storage_r.clear();
    }

    public static void addConsumer(ConsumerData consumerData) {
        consumers.add(consumerData);
        consumersCount.incrementAndGet();
        consumersByNum[consumerData.getNum()] = consumerData;
    }

//    static class Stat {
//        String c1;
//        String c2;
//        String c3;
//
//        @Override
//        public String toString() {
//            return "Stat{" +
//                   "c1='" + c1 + '\'' +
//                   ", c2='" + c2 + '\'' +
//                   ", c3='" + c3 + '\'' +
//                   '}';
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (o == null || getClass() != o.getClass()) return false;
//            Stat stat = (Stat) o;
//            return Objects.equals(c1, stat.c1) &&
//                   Objects.equals(c2, stat.c2) &&
//                   Objects.equals(c3, stat.c3);
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(c1, c2, c3);
//        }
//    }
//
//    static Set<Stat> zed = new HashSet<>();
//
//    public static void printStat() {
//        zed.forEach(System.out::println);
//    }

    public static class SplitterData {
        public SplitterData() {

        }

        String[] compsL1 = new String[1];
        String[] compsL2 = new String[2];
        String[] compsL3 = new String[3];
        String[] compsL4 = new String[4];
        String[] compsL5 = new String[5];

        String[] compsR1 = new String[1];
        String[] compsR2 = new String[2];
        String[] compsR3 = new String[3];
        String[] compsR4 = new String[4];
        String[] compsR5 = new String[5];

        String[] getCompsL(String key) {
            int cnt = 1;
            for (int i = 0; i < key.length(); i++) {
                if (key.charAt(i) == '.') {
                    cnt++;
                }
            }
            String[] comps = getCompByCnt(cnt, true);
            fill(key, comps);
            return comps;
        }

        String[] getCompsR(String key) {
            int cnt = 1;
            for (int i = 0; i < key.length(); i++) {
                if (key.charAt(i) == '.') {
                    cnt++;
                }
            }
            String[] comps = getCompByCnt(cnt, false);
            fill(key, comps);
            return comps;
        }

        private void fill(String key, String[] comps) {
            int startPos = 0;
            int idx = 0;
            int i = 0;
            do {
                if (i == key.length() || key.charAt(i) == '.') {
                    comps[idx++] = key.substring(startPos, i);
                    startPos = i + 1;
                }
            } while (i++ < key.length());
        }

        private String[] getCompByCnt(int cnt, boolean left) {
            switch (cnt) {
                case 1:
                    return left ? compsL1 : compsR1;
                case 2:
                    return left ? compsL2 : compsR2;
                case 3:
                    return left ? compsL3 : compsR3;
                case 4:
                    return left ? compsL4 : compsR4;
                case 5:
                    return left ? compsL5 : compsR5;
                default:
                    return new String[cnt];
            }
        }
    }

    static ThreadLocal<SplitterData> splitterDataThreadLocal = ThreadLocal.withInitial(SplitterData::new);

    public static void addTemplate(ConsumerData consumerData, String template) {
//
//        Stat stat = new Stat();
//        for (int i = 0; i < comps.length; i++) {
//            String c;
//            if (comps[i].charAt(0) == '#') {
//                c = "#";
//            } else if (comps[i].charAt(0) == '*') {
//                c = "*";
//            } else {
//                c = "W";
//            }
//            switch (i) {
//                case 0:
//                    stat.c1 = c;
//                    break;
//                case 1:
//                    stat.c2 = c;
//                    break;
//                case 2:
//                    stat.c3 = c;
//                    break;
//            }
//        }
//        zed.add(stat);

        String[] compsL = splitterDataThreadLocal.get().getCompsL(template);

        boolean firstIsSpec = template.charAt(0) == '#' || template.charAt(0) == '*';

        if (!firstIsSpec) {
            fill(consumerData, storage_l, compsL);
        } else {
            String[] compsR = splitterDataThreadLocal.get().getCompsR(template);
            for (int i = 0; i < compsL.length; i++) {
                compsR[compsL.length - 1 - i] = compsL[i];
            }
            fill(consumerData, storage_r, compsR);
        }
    }

    private static void fill(ConsumerData consumerData, Map<String, Node> map, String[] comps) {
        Node node = null;
        for (int i = 0; i < comps.length; i++) {
            String comp = comps[i];
            node = map.computeIfAbsent(comp, Node::new);
            node.count++;
            if (i < comps.length - 1) {
                if (node.toNextMap == null) {
                    node.toNextMap = new ConcurrentHashMap<>(1);
                }
                map = node.toNextMap;
            }
        }
        assert node != null;
        if (node.endHere == null) {
            node.endHere = new BitSet();
        }
        node.endHere.set(consumerData.getNum(), true);
    }

    public static void removeTemplate(ConsumerData consumerData, String template) {
        String[] comps = StringHelper.split(template);

        String firstComp = comps[0];
        boolean firstIsSpec = firstComp.charAt(0) == '#' || firstComp.charAt(0) == '*';
        if (!firstIsSpec) {
            delete(consumerData, storage_l, comps);
        } else {
            String[] compsR = new String[comps.length];
            for (int i = 0; i < comps.length; i++) {
                compsR[comps.length - 1 - i] = comps[i];
            }
            delete(consumerData, storage_r, compsR);
        }
    }

    private static void delete(ConsumerData consumerData, Map<String, Node> map, String[] comps) {

        Node node = null;
        for (int i = 0; i < comps.length; i++) {
            String comp = comps[i];
            node = map.get(comp);
            if (node == null) {
                return;
            }
            node.count--;
            if (node.count == 0) {
                // если ссылок не осталось - прссто обрубаем веточку и можно не уменьшать счётчик
                map.remove(comp);
                return;
            }
        }
        assert node != null;
        node.endHere.set(consumerData.getNum(), false);
    }

    static ThreadLocal<BitSet> workBitSetThreadLocal = ThreadLocal.withInitial(() -> new BitSet(MAX_CONSUMERS));
    static ThreadLocal<BitSet> allBitSetThreadLocal = ThreadLocal.withInitial(() -> new BitSet(MAX_CONSUMERS));

    static class ResponseKeeper {
        private Mbproto.ConsumeResponse response;
        private String key;
        private ByteString payload;

        void onInit(String key, ByteString payload) {
            response = null;
            this.key = key;
            this.payload = payload;
        }

        Mbproto.ConsumeResponse obtaint() {
            if (response == null) {
                response = Mbproto.ConsumeResponse.newBuilder()
                        .setKey(key)
                        .setPayload(payload)
                        .build();
            }
            return response;
        }

        void after() {
            response = null;
            key = null;
            payload = null;
        }
    }

    static ThreadLocal<ResponseKeeper> keeperThreadLocal = ThreadLocal.withInitial(ResponseKeeper::new);

    public static void matchToAndSend(String key, ByteString payload) {
        String[] compsL = splitterDataThreadLocal.get().getCompsL(key);
        String[] compsR = splitterDataThreadLocal.get().getCompsR(key);
        for (int i = 0; i < compsL.length; i++) {
            compsR[compsL.length - 1 - i] = compsL[i];
        }
        BitSet bs = allBitSetThreadLocal.get();
        bs.clear();
        ResponseKeeper responseKeeper = keeperThreadLocal.get();
        responseKeeper.onInit(key, payload);
        search(storage_l, bs, compsL, responseKeeper);
        search(storage_r, bs, compsR, responseKeeper);
        responseKeeper.after();
    }

    private static void search(Map<String, Node> storage, BitSet bs, String[] comps,
                               ResponseKeeper responseKeeper) {
        String comp1 = comps[0];
        // на первом уровне всегда доступно слово
        Node node = storage.get(comp1);
        if (node == null) {
            return;
        }
        if (comps.length == 1) {
            // возможны следующие варианты:
            // SLOVO
            // SLOVO -> #
            iterateAndSend(bs, responseKeeper, node.endHere);

            if (node.toNextMap != null) {
                Node nodeHash = node.toNextMap.get("#");
                if (nodeHash != null) {
                    iterateAndSend(bs, responseKeeper, nodeHash.endHere);
                }
            }

        } else if (comps.length == 2) { // ------------------------------------------ два слова

            String comp2 = comps[1];
            // возможны следующие варианты:
            // SLOVO2
            // *
            // #
            // (#) -> SLOVO2
            if (node.toNextMap == null) {
                return;
            }
            Node nodeComp2 = node.toNextMap.get(comp2);
            if (nodeComp2 != null) {
                iterateAndSend(bs, responseKeeper, nodeComp2.endHere);
            }
            Node nodeStar = node.toNextMap.get("*");
            if (nodeStar != null) {
                iterateAndSend(bs, responseKeeper, nodeStar.endHere);
            }
            Node nodeHash = node.toNextMap.get("#");
            if (nodeHash != null) {
                iterateAndSend(bs, responseKeeper, nodeHash.endHere);
                if (nodeHash.toNextMap != null) {
                    Node nodeWord3 = nodeHash.toNextMap.get(comp2);
                    if (nodeWord3 != null) {
                        iterateAndSend(bs, responseKeeper, nodeWord3.endHere);
                    }
                }
            }
        } else if (comps.length == 3) { // ------------------------------------------ три слова
            // дальше возможно
            // SLOVO2 -> SLOVO3
            //   *    -> *
            //   *    -> SLOVO3
            //   #
            //   #    -> SLOVO3
            String comp2 = comps[1];
            String comp3 = comps[2];
            if (node.toNextMap == null) {
                return;
            }
            Node nodeComp2 = node.toNextMap.get(comp2);
            if (nodeComp2 != null) {
                if (nodeComp2.toNextMap != null) {
                    Node nodeComp3 = nodeComp2.toNextMap.get(comp3);
                    if (nodeComp3 != null) {
                        iterateAndSend(bs, responseKeeper, nodeComp3.endHere);
                    }
                }
            }
            Node nodeStar2 = node.toNextMap.get("*");
            if (nodeStar2 != null && nodeStar2.toNextMap != null) {
                Node nodeStar3 = nodeStar2.toNextMap.get("*");
                if (nodeStar3 != null) {
                    iterateAndSend(bs, responseKeeper, nodeStar3.endHere);
                }
                Node nodeWord3 = nodeStar2.toNextMap.get(comp3);
                if (nodeWord3 != null) {
                    iterateAndSend(bs, responseKeeper, nodeWord3.endHere);
                }
            }
            Node nodeHash2 = node.toNextMap.get("#");
            if (nodeHash2 != null) {
                iterateAndSend(bs, responseKeeper, nodeHash2.endHere);
                if (nodeHash2.toNextMap != null) {
                    Node nodeWord3 = nodeHash2.toNextMap.get(comp3);
                    if (nodeWord3 != null) {
                        iterateAndSend(bs, responseKeeper, nodeWord3.endHere);
                    }
                }
            }
        }
    }

    private static void iterateAndSend(BitSet all, ResponseKeeper responseKeeper, BitSet endHere) {
        if (endHere == null) {
            return;
        }
        if (endHere.cardinality() == 0) {
            return;
        }
        BitSet work = workBitSetThreadLocal.get();
        work.clear();
        work.or(all); // init
        work.xor(endHere);
        work.and(endHere);
        if (work.isEmpty()) {
            return;
        }
        for (int i = work.nextSetBit(0); i != -1; i = work.nextSetBit(i + 1)) {
            ConsumerData consumerData = consumersByNum[i];
            consumerData.send(responseKeeper.obtaint());
            all.set(i);
        }
    }

    static class Node {
        volatile String templateComponent;
        volatile BitSet endHere;
        volatile Map<String, Node> toNextMap;
        // число консьюмеров, проходящих через ноду
        volatile int count;

        public Node(String templateComponent) {
            this.templateComponent = templateComponent;
        }
    }
}
