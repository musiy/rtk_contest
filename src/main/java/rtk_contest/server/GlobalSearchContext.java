package rtk_contest.server;

import com.google.common.collect.Sets;
import mbproto.Mbproto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtk_contest.templating.StringHelper;

import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalSearchContext {

    static final int MAX_CONSUMERS = 1_000;

    static Logger LOGGER = LoggerFactory.getLogger(GlobalSearchContext.class);

    static Set<ConsumerData> consumers = Sets.newConcurrentHashSet();
    static AtomicInteger consumersCount = new AtomicInteger();

    // todo посчитать сколько элементов на максимуме и этим значением инициализировать
    final static Map<String, Node> storage = new ConcurrentHashMap<>(30_000);

    public static void testInit() {
        consumers.clear();
        consumersCount = new AtomicInteger(0);
        storage.clear();
    }

    public static void addConsumer(ConsumerData consumerData) {
        consumers.add(consumerData);
        consumersCount.incrementAndGet();
    }

    public static void addTemplate(ConsumerData consumerData, String template) {
        String[] comps = StringHelper.split(template);
        int hashCnt = 0;
        for (int i = 0; i < comps.length; i++) {
            if ('#' == comps[i].charAt(0)) {
                hashCnt ++;
            }
        }
        if (comps.length > 3) {
            LOGGER.info(template);
        }


        Map<String, Node> currentNodeMap = storage;

        for (int i = 0; i < comps.length; i++) {
            String comp = comps[i];
            Node node = currentNodeMap.computeIfAbsent(comp, Node::new);
            // в мапе по ключу - компоненту хранится нода этого компонента
            node.templComp = comp;
            // увеличиваем
            node.count++;
            if (comp.charAt(0) == '#') {
                // заполним в hashConsumers консьюмеров, которые проходили по этому пути
                if (node.hashConsumers == null) {
                    node.hashConsumers = new BitSet(MAX_CONSUMERS);
                }
                node.hashConsumers.set(consumerData.getNum(), true);
            }
            if (i == comps.length - 1) {
                // если это последний элемент - заполняем endConsumers
                if (node.endConsumers == null) {
                    node.endConsumers = Sets.newConcurrentHashSet();
                }
                node.endConsumers.add(consumerData);
            } else {
                if (node.mappingToNext == null) {
                    node.mappingToNext = new ConcurrentHashMap<>();
                }
                currentNodeMap = node.mappingToNext;
            }
        }
    }

    public static void removeTemplate(ConsumerData consumerData, String template) {
        String[] comps = StringHelper.split(template);

        Map<String, Node> currentNodeMap = storage;
        for (int i = 0; i < comps.length; i++) {
            String comp = comps[i];
            Node node = currentNodeMap.get(comp);
            if (node == null) {
                currentNodeMap.remove(comp);
                return;
            }
            node.count--;
            if (node.count == 0) {
                currentNodeMap.remove(comp);
                // обрубаем дерево
                break;
            }
            if (comp.charAt(0) == '#') {
                // удалим в hashConsumers консьюмера, который тут больше не живёт
                node.hashConsumers.set(consumerData.getNum(), false);
            }
            if (i == comps.length - 1) {
                // если это последний элемент - удаляем endConsumers
                if (node.count > 0) {
                    node.endConsumers.remove(consumerData);
                }
            }
            currentNodeMap = node.mappingToNext;
        }
    }

    public static void matchToAndSend(Mbproto.ConsumeResponse response, String template) {
        String[] comps = StringHelper.split(template);
        if (comps.length > 3) {
            LOGGER.info(template);
        }
        BitSet bitSet = new BitSet(consumersCount.get());
        makeSwitch(bitSet, storage, comps, 0, response);
    }

    private static void makeSwitch(BitSet bitSet, Map<String, Node> mappingToNext,
                                   String[] comps, int pos, Mbproto.ConsumeResponse response) {
        if (pos >= comps.length) {
            return;
        }
        if (mappingToNext == null) {
            return;
        }
        {
            Node node = mappingToNext.get(comps[pos]);
            if (node != null) {
                matchAndSend(bitSet, node, comps, pos, response);
            }
        }
        {
            Node node = mappingToNext.get("*");
            if (node != null) {
                matchAndSend(bitSet, node, comps, pos, response);
            }
        }
        {
            Node node = mappingToNext.get("#");
            if (node != null) {
                handleHash(bitSet, node, comps, pos, response);
            }
        }
    }

    private static void handleHash(BitSet bitSet, Node node, String[] comps, int pos, Mbproto.ConsumeResponse response) {
        BitSet work = (BitSet) bitSet.clone();
        work.xor(node.hashConsumers);
        work.and(node.hashConsumers);
        if (work.isEmpty()) {
            return;
        }

        // текущая решётка - может подходить любому ключу
        if (node.endConsumers != null) {
            iterateAndSend(bitSet, response, node.endConsumers);
        }
        // 1 - пропускаем, действуем, как будто '#' не было (# может не занимать ни одного слова)
        Map<String, Node> mappingToNext = node.mappingToNext;
        if (mappingToNext != null) {
            for (Node next : mappingToNext.values()) {
                matchAndSend(bitSet, next, comps, pos, response);
            }
        }
        // 2 - работает как '*', т.е. снимает первое слово в comps
        if (mappingToNext != null && pos + 1 < comps.length) {
            for (Node next : mappingToNext.values()) {
                matchAndSend(bitSet, next, comps, pos + 1, response);
            }
        }
        // 3 - расширение # до любой позиции в компонентах ключа
        for (int i = pos + 1; i < comps.length; i++) {
            matchAndSend(bitSet, node, comps, i, response);
        }
    }

    private static void matchAndSend(BitSet bitSet, Node node, String[] keyComps, int posInKeyComps,
                                     Mbproto.ConsumeResponse response) {

        boolean isNodeHash = node.templComp.charAt(0) == '#';
        boolean isNodeStar = node.templComp.charAt(0) == '*';

        // текущий компонент это либо решётка, либо звезда либо слово
        if (posInKeyComps == keyComps.length - 1) {
            boolean isMatched = false;
            if (node.templComp.equals(keyComps[posInKeyComps])
                || isNodeStar) {
                if (node.endConsumers != null) {
                    iterateAndSend(bitSet, response, node.endConsumers);
                }
                isMatched = true;
            } else if (isNodeHash) {
                handleHash(bitSet, node, keyComps, posInKeyComps, response);
                isMatched = true;
            }
            if (isMatched) {
                // если дальше остались только хеши - тоже подходит
                Node curr = node;
                while (curr.mappingToNext != null && (curr = curr.mappingToNext.get("#")) != null) {
                    if (curr.endConsumers != null) {
                        iterateAndSend(bitSet, response, curr.endConsumers);
                    }
                }
            }
            return;
        }
        if (!isNodeHash && !isNodeStar) {
            if (!node.templComp.equals(keyComps[posInKeyComps])) {
                return;
            }
        }
        makeSwitch(bitSet, node.mappingToNext, keyComps, posInKeyComps + 1, response);
    }

    private static void iterateAndSend(BitSet bitSet, Mbproto.ConsumeResponse response, Set<ConsumerData> consumers) {
        if (consumers != null) {
            for (ConsumerData consumer : consumers) {
                if (!bitSet.get(consumer.getNum())) {
                    consumer.send(response);
                    bitSet.set(consumer.getNum(), true);
                }
            }
        }
    }

    static class Node {
        public Node(String templComp) {
            this.templComp = templComp;
        }

        // текущий компонент шаблона
        volatile String templComp;

        volatile BitSet hashConsumers;

        // число шаблонов, которые проходят через эту ноду
        volatile int count;

        // переход к следующим нодам по слову
        volatile Map<String, Node> mappingToNext;

        // консьюмеры которые заканчиваются здесь
        volatile Set<ConsumerData> endConsumers;

        @Override
        public String toString() {
            return "Node{" +
                   "templComp='" + templComp + '\'' +
                   '}';
        }
    }
}
