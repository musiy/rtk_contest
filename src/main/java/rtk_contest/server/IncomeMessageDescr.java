package rtk_contest.server;

import mbproto.Mbproto;

public class IncomeMessageDescr {

    public ConsumerData[] consumers;
    public String key;
    public String[] keyComps;
    public Mbproto.ConsumeResponse response;
    public int start;
    public int end;

    public IncomeMessageDescr(ConsumerData[] consumers, String key, String[] keyComps, Mbproto.ConsumeResponse response, int start, int end) {
        this.consumers = consumers;
        this.key = key;
        this.keyComps = keyComps;
        this.response = response;
        this.start = start;
        this.end = end;
    }
}
