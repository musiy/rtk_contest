package rtk_contest.server;

import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final static Map<String, Node> storage_l = new ConcurrentHashMap<>(3_000_000);
    final static Map<String, Node> storage_r = new ConcurrentHashMap<>(100_000);

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

    static AtomicInteger stat_cnt = new AtomicInteger(0);

    public static void printStat() {
        LOGGER.info(String.format("size of storage_l: %d", storage_l.size()));
        LOGGER.info(String.format("size of storage_r: %d", storage_r.size()));
        LOGGER.info(String.format("count of templates: %d", stat_cnt.get()));
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

        String[] getCompsR(String[] compsL) {
            String[] compsR = getCompByCnt(compsL.length, false);
            for (int i = 0; i < compsL.length; i++) {
                compsR[compsL.length - 1 - i] = compsL[i];
            }
            return compsR;
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
        stat_cnt.incrementAndGet();
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
            String[] compsR = splitterDataThreadLocal.get().getCompsR(compsL);
            for (int i = 0; i < compsL.length; i++) {
                compsR[compsL.length - 1 - i] = compsL[i];
            }
            fill(consumerData, storage_r, compsR);
        }
    }

    private static void fill(ConsumerData consumerData, Map<String, Node> map, String[] comps) {

        Object from = map;
        Node node = null;

        for (int i = 0; i < comps.length; i++) {
            String comp = comps[i];
            if (from instanceof Map) {
                node = ((Map<String, Node>) from).computeIfAbsent(comp, GlobalSearchContext::getNode);
            } else {
                node = getNode(comp);
                ((Node) from).next = node;
            }
            if (i < comps.length - 1) {
                if (node.next == null) {
                    from = node; // заполним на следующем шаге
                } else if (node.next instanceof Node) {
                    Node tmpNode = (Node) node.next;
                    Map<String, Node> tmpMap = GlobalSearchContext.getMap(2); // 2 - capacity
                    tmpMap.put(tmpNode.templateComponent, tmpNode);
                    node.next = tmpMap;
                    from = tmpMap;
                } else if (node.next instanceof Map) {
                    from = node.next;
                } else {
                    LOGGER.error("Ошибка заполнения ноды");
                }
            }
        }
        assert node != null;
        if (node.endHere == null) {
            node.endHere = consumerData.getNum();
        } else if (node.endHere instanceof Integer) {
            int prevVal = (int) node.endHere;
            BitSet newVal = new BitSet();
            newVal.set(prevVal, true);
            newVal.set(consumerData.getNum(), true);
            node.endHere = newVal;
        } else {
            BitSet val = (BitSet) node.endHere;
            val.set(consumerData.getNum(), true);
        }
        //node.endHere.set(consumerData.getNum(), true);
    }

    static Node getNode(String comp) {
        // todo сделать пул нод
        return new Node(comp);
    }

    static Map<String, Node> getMap(int capacity) {
        // todo сделать пул карт
        return new ConcurrentHashMap<>(capacity, 1.0f, 4);
    }

    public static void removeTemplate(ConsumerData consumerData, String template) {
        stat_cnt.decrementAndGet();

        String[] compsL = splitterDataThreadLocal.get().getCompsL(template);

        String firstComp = compsL[0];
        boolean firstIsSpec = firstComp.charAt(0) == '#' || firstComp.charAt(0) == '*';
        if (!firstIsSpec) {
            delete(consumerData, storage_l, compsL);
        } else {
            String[] compsR = splitterDataThreadLocal.get().getCompsR(compsL);
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
            if (node.next instanceof Node) {
                map.remove(comp);
                return;
            }
            map = (Map<String, Node>) node.next;
        }
        assert node != null;
        if (node.endHere != null) {
            if (node.endHere instanceof Integer) {
                node.endHere = null;
            } else if (node.endHere instanceof BitSet) {
                BitSet endHere = (BitSet) node.endHere;
                endHere.set(consumerData.getNum(), false);
            }
        }
        //node.endHere.set(consumerData.getNum(), false);

//        Node node = null;
//        for (int i = 0; i < comps.length; i++) {
//            String comp = comps[i];
//            node = map.get(comp);
//            if (node == null) {
//                return;
//            }
//            node.count--;
//            if (node.count == 0) {
//                // если ссылок не осталось - прссто обрубаем веточку и можно не уменьшать счётчик
//                map.remove(comp);
//                return;
//            }
//        }
//        assert node != null;
//        node.endHere.set(consumerData.getNum(), false);
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
        String[] compsR = splitterDataThreadLocal.get().getCompsR(compsL);
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

            if (node.next != null) {
                Node nodeHash = getNodeComp(node, "#");
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
            if (node.next == null) {
                return;
            }
            Node nodeComp2 = getNodeComp(node, comp2);
            if (nodeComp2 != null) {
                iterateAndSend(bs, responseKeeper, nodeComp2.endHere);
            }
            Node nodeStar = getNodeComp(node, "*");
            if (nodeStar != null) {
                iterateAndSend(bs, responseKeeper, nodeStar.endHere);
            }
            Node nodeHash = getNodeComp(node, "#");
            if (nodeHash != null) {
                iterateAndSend(bs, responseKeeper, nodeHash.endHere);
                if (nodeHash.next != null) {
                    Node nodeWord3 = getNodeComp(nodeHash, comp2);
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
            if (node.next == null) {
                return;
            }
            Node nodeComp2 = getNodeComp(node, comp2);
            if (nodeComp2 != null) {
                if (nodeComp2.next != null) {
                    Node nodeComp3 = getNodeComp(nodeComp2, comp3);
                    if (nodeComp3 != null) {
                        iterateAndSend(bs, responseKeeper, nodeComp3.endHere);
                    }
                }
            }
            Node nodeStar2 = getNodeComp(node, "*");
            if (nodeStar2 != null && nodeStar2.next != null) {
                Node nodeStar3 = getNodeComp(nodeStar2, "*");
                if (nodeStar3 != null) {
                    iterateAndSend(bs, responseKeeper, nodeStar3.endHere);
                }
                Node nodeWord3 = getNodeComp(nodeStar2, comp3);
                if (nodeWord3 != null) {
                    iterateAndSend(bs, responseKeeper, nodeWord3.endHere);
                }
            }
            Node nodeHash2 = getNodeComp(node, "#");
            if (nodeHash2 != null) {
                iterateAndSend(bs, responseKeeper, nodeHash2.endHere);
                if (nodeHash2.next != null) {
                    Node nodeWord3 = getNodeComp(nodeHash2, comp3);
                    if (nodeWord3 != null) {
                        iterateAndSend(bs, responseKeeper, nodeWord3.endHere);
                    }
                }
            }
        }
    }

    static Node getNodeComp(Node node, String comp) {
        if (node.next instanceof Map) {
            Map<String, Node> tmpMap = (Map<String, Node>) node.next;
            return tmpMap.get(comp);
        } else if (node.next instanceof Node) {
            Node tmpNode = (Node) node.next;
            if (comp.equals(tmpNode.templateComponent)) {
                return tmpNode;
            }
            return null;
        } else {
            LOGGER.error("Странная нода");
            return null;
        }
    }

    private static void iterateAndSend(BitSet all, ResponseKeeper responseKeeper, Object endHereObj) {
        if (endHereObj == null) {
            return;
        }
        if (endHereObj instanceof BitSet) {
            BitSet endHere = (BitSet) endHereObj;
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
        } else if (endHereObj instanceof Integer) {
            int num = (int) endHereObj;
            if (!all.get(num)) {
                ConsumerData consumerData = consumersByNum[num];
                consumerData.send(responseKeeper.obtaint());
                all.set(num);
            }
        }
    }

    static class Node {
        volatile String templateComponent;
        volatile Object endHere;
        volatile Object next; // Map<String, Node>

        public Node(String templateComponent) {
            this.templateComponent = templateComponent;
        }

        // число консьюмеров, проходящих через ноду
        public boolean noMoreConsumers() {
            if (next == null) {
                return true;
            }
            if (next instanceof Map) {
                return ((Map) next).isEmpty();
            } else if (next instanceof Node) {
                return true;
            }
            return true;
        }
    }
}
