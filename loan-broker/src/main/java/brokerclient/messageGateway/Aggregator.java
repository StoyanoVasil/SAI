package brokerclient.messageGateway;

import brokerclient.model.BankInterestReply;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Aggregator {

    private Consumer consumer;
    private Map<Integer, Integer> mapAggregationToCount;
    private Map<Integer, ArrayList<BankInterestReply>> mapAggregationToReplies;
    private InterestSerializer serializer;

    public Aggregator(String brokerQueue) {

        this.mapAggregationToCount = new HashMap<>();
        this.mapAggregationToReplies = new HashMap<>();
        this.serializer = new InterestSerializer();
        this.consumer = new Consumer(brokerQueue);
        this.consumer.setMessageListener(message -> {
            try {
                handleMessage(message);
            } catch (JMSException e) { e.printStackTrace(); }
        });
    }

    public void addAggregation(int aggregationId, int numberOfReqSent) {

        this.mapAggregationToCount.put(aggregationId, numberOfReqSent);
        this.mapAggregationToReplies.put(aggregationId, new ArrayList<>());
    }

    private void handleMessage(Message message) throws JMSException {

        TextMessage msg = (TextMessage) message;
        BankInterestReply rep = this.serializer.deserializeBankInterestReply(msg.getText());
        int aggregationId = msg.getIntProperty("aggregationID");
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
