package brokerclient.messageGateway;

import brokerclient.model.*;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

public class BankGateway {

    private static final String JMS_CONSUMER_QUEUE_NAME = "broker-bank";
    private static final String JMS_PRODUCER_QUEUE_NAME = "abn-bank";

    private Consumer consumer;
    private Producer producer;
    private InterestSerializer serializer;
    private Map<String, LoanRequest> map;
    private CreditHistoryEnricher enricher;
    private Archiver archiver;

    public BankGateway() {

        this.consumer = new Consumer(JMS_CONSUMER_QUEUE_NAME);
        this.producer = new Producer(JMS_PRODUCER_QUEUE_NAME);
        this.serializer = new InterestSerializer();
        this.map = new HashMap<>();
        this.enricher = new CreditHistoryEnricher();
        this.archiver = new Archiver();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                BankInterestReply rep = this.serializer.deserializeBankInterestReply(msg.getText());
                LoanRequest req = this.map.get(msg.getJMSCorrelationID());
                LoanReply reply = new LoanReply(rep.getInterest(), rep.getQuoteId());
                onBankInterestRequestArrived(req, reply);
                this.archiver.archive(new LoanArchive(req.getSsn(), req.getAmount(),
                        reply.getQuoteID(), reply.getInterest(), req.getTime()));
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void applyForLoan(LoanRequest req) throws JMSException {

        CreditHistory ch = this.enricher.getCreditHistoryForSSN(req.getSsn());
        BankInterestRequest request = new BankInterestRequest(req.getAmount(), req.getTime(), ch.getCredit(), ch.getHistory());
        String json = this.serializer.serializeBankInterestRequest(request);
        Message msg = this.producer.createMessage(json);
        this.producer.send(msg);
        String id = msg.getJMSMessageID();
        this.map.put(id, req);
    }

    public void onBankInterestRequestArrived(LoanRequest req, LoanReply rep) { }
}
