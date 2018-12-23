package brokerclient.messageGateway;

import brokerclient.model.BankInterestReply;
import brokerclient.model.BankInterestRequest;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;

public class ScatterGather {

    // Declare BankGateway
    private BankGateway bankGateway;

    // Declare RecipientList
    private BankRecipientList bankRecipientList;

    // Declare Aggregator
    private Aggregator aggregator;

    // Declare aggregation counter
    private int aggregationCounter;

    // Declare map for aggregationId to BankInterestRequest mapping
    private Map<Integer, BankInterestRequest> bankInterestRequestMap;

    public ScatterGather() {

        // Initialize aggregation counter
        this.aggregationCounter = 0;

        // Initialize map
        this.bankInterestRequestMap = new HashMap<>();

        // Initialize aggregator
        this.aggregator = new Aggregator() {
            public void onAllRepliesReceived(BankInterestReply reply, Integer aggregationId) {
                onBankInterestReplyArrived(bankInterestRequestMap.get(aggregationId), reply);
            }
        };

        // Initialize BankGateway
        this.bankGateway = new BankGateway() {

            public void onBankInterestRequestArrived(BankInterestRequest req, BankInterestReply rep, Integer aggregationId) {

                if(!bankInterestRequestMap.containsKey(aggregationId)) bankInterestRequestMap.put(aggregationId, req);
                aggregator.newBankInterestReplyArrived(rep, aggregationId);
            }
        };

        // Initialize BankRecipientList
        this.bankRecipientList = new BankRecipientList(this.bankGateway);
    }

    public void applyForLoan(BankInterestRequest req) throws JMSException {

        int sentCount = this.bankRecipientList.sendRequest(req, this.aggregationCounter);
        this.aggregator.addAggregation(this.aggregationCounter, sentCount);
        this.aggregationCounter++;
    }

    public void onBankInterestReplyArrived(BankInterestRequest req, BankInterestReply rep) { }
}
