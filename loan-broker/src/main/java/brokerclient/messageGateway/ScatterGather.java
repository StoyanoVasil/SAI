package brokerclient.messageGateway;

import brokerclient.model.BankInterestReply;
import brokerclient.model.BankInterestRequest;

import javax.jms.JMSException;

public class ScatterGather {

    private static final String JMS_BROKER_QUEUE_NAME = "broker-bank";
    private static final String JMS_ING_BANK_QUEUE_NAME = "ing-bank";
    private static final String JMS_ABN_BANK_QUEUE_NAME = "abn-bank";
    private static final String JMS_RABO_BANK_QUEUE_NAME = "rabo-bank";

    private BankRecipientList bankRecipientList;
    private Aggregator aggregator;

    private Integer aggregationCount;

    public ScatterGather() {

        this.aggregationCount = 0;
        this.bankRecipientList = new BankRecipientList(JMS_ING_BANK_QUEUE_NAME,
                JMS_ABN_BANK_QUEUE_NAME, JMS_RABO_BANK_QUEUE_NAME);
        this.aggregator = new Aggregator(JMS_BROKER_QUEUE_NAME) {
            public void onAllRepliesReceived(BankInterestReply reply, Integer aggregationId) {
                onReplyReceived(reply, aggregationId);
            }
        };
    }

    public int sendRequest(BankInterestRequest request) throws JMSException {
        int repliesCount = this.bankRecipientList.sendRequest(request, this.aggregationCount);
        this.aggregator.addAggregation(this.aggregationCount, repliesCount);
        this.aggregationCount++;
        return this.aggregationCount - 1;
    }

    public void onReplyReceived(BankInterestReply reply, Integer aggregationId) {}
}
