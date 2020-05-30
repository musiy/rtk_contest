package rtk_contest.server;

import com.google.common.collect.Sets;
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
    static Map<Integer, ConsumerData> consumersByNum = new ConcurrentHashMap<>();
    static AtomicInteger consumersCount = new AtomicInteger();

    // todo посчитать сколько элементов на максимуме и этим значением инициализировать
    final static Map<String, Node> storage_l = new ConcurrentHashMap<>(50_000);
    final static Map<String, Node> storage_r = new ConcurrentHashMap<>(50_000);

    public static void testInit() {
        consumers.clear();
        consumersCount = new AtomicInteger(0);
        consumersByNum.clear();
        storage_l.clear();
        storage_r.clear();
    }

    public static void addConsumer(ConsumerData consumerData) {
        consumers.add(consumerData);
        consumersCount.incrementAndGet();
        consumersByNum.put(consumerData.getNum(), consumerData);
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

        String[] comps = StringHelper.split(template);

        boolean firstIsSpec = template.charAt(0) == '#' || template.charAt(0) == '*';

        if (!firstIsSpec) {
            fill(consumerData, storage_l, comps);
        } else {
            String[] compsR = new String[comps.length];
            for (int i = 0; i < comps.length; i++) {
                compsR[comps.length - 1 - i] = comps[i];
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
                    node.toNextMap = new ConcurrentHashMap<>();
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

    static ThreadLocal<BitSet> bitSetThreadLocal = ThreadLocal.withInitial(() -> new BitSet(MAX_CONSUMERS));

    public static void matchToAndSend(Mbproto.ConsumeResponse response, String key) {
        String[] compsL = StringHelper.split(key);
        String[] compsR = new String[compsL.length];
        for (int i = 0; i < compsL.length; i++) {
            compsR[compsL.length - 1 - i] = compsL[i];
        }
        BitSet bs = new BitSet(MAX_CONSUMERS);
        search(storage_l, bs, compsL, response, true);
        search(storage_r, bs, compsR, response, false);
    }

    private static void search(Map<String, Node> storage, BitSet bs, String[] comps,
                               Mbproto.ConsumeResponse response, boolean isForward) {
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
            iterateAndSend(bs, response, node.endHere);

            if (node.toNextMap != null) {
                Node nodeHash = node.toNextMap.get("#");
                if (nodeHash != null) {
                    iterateAndSend(bs, response, nodeHash.endHere);
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
                iterateAndSend(bs, response, nodeComp2.endHere);
            }
            Node nodeStar = node.toNextMap.get("*");
            if (nodeStar != null) {
                iterateAndSend(bs, response, nodeStar.endHere);
            }
            Node nodeHash = node.toNextMap.get("#");
            if (nodeHash != null) {
                iterateAndSend(bs, response, nodeHash.endHere);
                if (nodeHash.toNextMap != null) {
                    Node nodeWord3 = nodeHash.toNextMap.get(comp2);
                    if (nodeWord3 != null) {
                        iterateAndSend(bs, response, nodeWord3.endHere);
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
                        iterateAndSend(bs, response, nodeComp3.endHere);
                    }
                }
            }
            Node nodeStar2 = node.toNextMap.get("*");
            if (nodeStar2 != null && nodeStar2.toNextMap != null) {
                Node nodeStar3 = nodeStar2.toNextMap.get("*");
                if (nodeStar3 != null) {
                    iterateAndSend(bs, response, nodeStar3.endHere);
                }
                Node nodeWord3 = nodeStar2.toNextMap.get(comp3);
                if (nodeWord3 != null) {
                    iterateAndSend(bs, response, nodeWord3.endHere);
                }
            }
            Node nodeHash2 = node.toNextMap.get("#");
            if (nodeHash2 != null) {
                iterateAndSend(bs, response, nodeHash2.endHere);
                if (nodeHash2.toNextMap != null) {
                    Node nodeWord3 = nodeHash2.toNextMap.get(comp3);
                    if (nodeWord3 != null) {
                        iterateAndSend(bs, response, nodeWord3.endHere);
                    }
                }
            }
        }
    }

    private static void iterateAndSend(BitSet all, Mbproto.ConsumeResponse response, BitSet endHere) {
        if (endHere == null) {
            return;
        }
        BitSet work = bitSetThreadLocal.get();
        work.clear();
        work.or(all); // init
        work.xor(endHere);
        work.and(endHere);
        if (work.isEmpty()) {
            return;
        }
        for (int i = work.nextSetBit(0); i != -1; i = work.nextSetBit(i + 1)) {
            ConsumerData consumerData = consumersByNum.get(i);
            consumerData.send(response);
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
