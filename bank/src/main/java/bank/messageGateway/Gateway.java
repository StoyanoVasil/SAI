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
    private static final String JMS_BANK_QUEUE_NAME = "abn-bank";
    private static final String JMS_BROKER_QUEUE_NAME = "broker-bank";

    // Declare producer, consumer and serializer
    private Producer producer;
    private Consumer consumer;
    private InterestSerializer serializer;
    private Map<BankInterestRequest, String> map;

    public Gateway() {

        this.consumer = new Consumer(JMS_BANK_QUEUE_NAME);
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.serializer = new InterestSerializer();
        this.map = new HashMap<>();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                BankInterestRequest req = this.serializer.deserializeBankInterestRequest(msg.getText());
                this.map.put(req, message.getJMSCorrelationID());
                onBankInterestRequestArrived(req);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void replyOnRequest(BankInterestRequest req, BankInterestReply rep) throws JMSException {

        String json = this.serializer.serializeBankInterestReply(rep);
        Message msg = this.producer.createMessage(json);
        msg.setJMSCorrelationID(this.map.get(req));
        this.producer.send(msg);
    }

    public void onBankInterestRequestArrived(BankInterestRequest req) {}
}
