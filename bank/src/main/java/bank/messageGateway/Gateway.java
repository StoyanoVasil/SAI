package bank.messageGateway;

import bank.model.BankInterestReply;
import bank.model.BankInterestRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

public class Gateway {

    // queue names
    private static final String JMS_BROKER_QUEUE_NAME = "broker-bank";
    private String JMS_BANK_QUEUE_NAME;

    // Declare producer, consumer and serializer
    private Producer producer;
    private Consumer consumer;
    private InterestSerializer serializer;
    private Map<BankInterestRequest, String> mapReqToCorrelation;
    private Map<String, Integer> mapCorrelationToAggregation;

    public Gateway(String bankQueue) {

        this.JMS_BANK_QUEUE_NAME = bankQueue;
        this.consumer = new Consumer(JMS_BANK_QUEUE_NAME);
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.serializer = new InterestSerializer();
        this.mapReqToCorrelation = new HashMap<>();
        this.mapCorrelationToAggregation = new HashMap<>();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                BankInterestRequest req = this.serializer.deserializeBankInterestRequest(msg.getText());
                this.mapReqToCorrelation.put(req, message.getJMSMessageID());
                this.mapCorrelationToAggregation.put(message.getJMSMessageID(),
                        message.getIntProperty("aggregationID"));
                onBankInterestRequestArrived(req);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void replyOnRequest(BankInterestRequest req, BankInterestReply rep) throws JMSException {

        String json = this.serializer.serializeBankInterestReply(rep);
        Message msg = this.producer.createMessage(json);
        String correlation = this.mapReqToCorrelation.get(req);
        msg.setJMSCorrelationID(correlation);
        msg.setIntProperty("aggregationID", this.mapCorrelationToAggregation.get(correlation));
        this.producer.send(msg);
    }

    public void onBankInterestRequestArrived(BankInterestRequest req) {}
}
