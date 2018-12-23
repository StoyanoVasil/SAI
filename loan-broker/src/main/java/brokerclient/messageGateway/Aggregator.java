package brokerclient.messageGateway;

import brokerclient.model.BankInterestReply;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Aggregator {

    private Map<Integer, Integer> mapAggregationToCount;
    private Map<Integer, ArrayList<BankInterestReply>> mapAggregationToReplies;

    public Aggregator() {

        this.mapAggregationToCount = new HashMap<>();
        this.mapAggregationToReplies = new HashMap<>();
    }

    public void addAggregation(Integer aggregationId, Integer numberOfReqSent) {

        this.mapAggregationToCount.put(aggregationId, numberOfReqSent);
        this.mapAggregationToReplies.put(aggregationId, new ArrayList<>());
    }

    public void newBankInterestReplyArrived(BankInterestReply rep, Integer aggregationId) {

        this.mapAggregationToReplies.get(aggregationId).add(rep);
        checkAllRepliesReceived(aggregationId);
    }

    private void checkAllRepliesReceived(Integer aggregationId) {
        ArrayList<BankInterestReply> replies = this.mapAggregationToReplies.get(aggregationId);
        if(replies.size() == this.mapAggregationToCount.get(aggregationId)) {
            double min = Integer.MAX_VALUE;
            BankInterestReply rep = null;
            for(BankInterestReply r : replies) {
                if(min > r.getInterest()) rep = r;
            }
            onAllRepliesReceived(rep, aggregationId);
        }
    }

    public void onAllRepliesReceived(BankInterestReply reply, Integer aggregationId) {}
}