package brokerclient.messageGateway;

import brokerclient.model.*;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

public class BankGateway {

    private ScatterGather scatterGather;
    private Map<Integer, LoanRequest> map;
    private CreditHistoryEnricher enricher;
    private Archiver archiver;

    public BankGateway() {

        this.map = new HashMap<>();
        this.enricher = new CreditHistoryEnricher();
        this.archiver = new Archiver();
        this.scatterGather = new ScatterGather() {
            public void onReplyReceived(BankInterestReply reply, Integer aggregationId) {
                LoanRequest req = map.get(aggregationId);
                LoanReply rep = new LoanReply(reply.getInterest(), reply.getQuoteId());
                onBankInterestRequestArrived(req, rep);
                archiver.archive(new LoanArchive(req.getSsn(), req.getAmount(),
                        rep.getQuoteID(), reply.getInterest(), req.getTime()));
            }
        };
    }

    public void applyForLoan(LoanRequest req) throws JMSException {

        CreditHistory ch = this.enricher.getCreditHistoryForSSN(req.getSsn());
        BankInterestRequest request = new BankInterestRequest(req.getAmount(), req.getTime(), ch.getCredit(), ch.getHistory());
        int id = this.scatterGather.sendRequest(request);
        this.map.put(id, req);
    }

    public void onBankInterestRequestArrived(LoanRequest req, LoanReply rep) { }
}
