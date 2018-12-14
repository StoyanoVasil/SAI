package brokerclient.messageGateway;

import brokerclient.model.*;

import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;

public class BankGateway {

    private ScatterGather scatterGather;
    private Map<Integer, BankInterestRequest> map;
    // TODO: move to controller

    public BankGateway() {

        this.map = new HashMap<>();
        this.scatterGather = new ScatterGather() {
            public void onReplyReceived(BankInterestReply reply, Integer aggregationId) {
                BankInterestRequest req = map.get(aggregationId);
                onBankInterestRequestArrived(req, reply);
            }
        };
    }

    public void applyForLoan(BankInterestRequest req) throws JMSException {

        int id = this.scatterGather.sendRequest(req);
        this.map.put(id, req);
    }

    public void onBankInterestRequestArrived(BankInterestRequest req, BankInterestReply rep) { }
}
