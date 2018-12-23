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
    private Map<String, BankInterestRequest> map;

    public BankGateway() {

        this.consumer = new Consumer(JMS_CONSUMER_QUEUE_NAME);
        this.producer = new Producer(JMS_PRODUCER_QUEUE_NAME);
//        this.producer = new Producer();
        this.serializer = new InterestSerializer();
        this.map = new HashMap<>();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                BankInterestReply rep = this.serializer.deserializeBankInterestReply(msg.getText());
                BankInterestRequest req = this.map.get(msg.getJMSCorrelationID());
                onBankInterestRequestArrived(req, rep);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void applyForLoan(BankInterestRequest req) throws JMSException {

        String json = this.serializer.serializeBankInterestRequest(req);
        Message msg = this.producer.createMessage(json);
        this.producer.send(msg);
//        this.producer.send(msg, "abn-bank");
        String id = msg.getJMSMessageID();
        this.map.put(id, req);
    }

    public void onBankInterestRequestArrived(BankInterestRequest req, BankInterestReply rep) { }
}
