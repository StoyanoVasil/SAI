package brokerclient.messageGateway;

import brokerclient.model.BankInterestReply;
import brokerclient.model.BankInterestRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class BankGateway {

    private static final String JMS_CONSUMER_QUEUE_NAME = "broker-bank";
    private static final String JMS_PRODUCER_QUEUE_NAME = "abn-bank";

    private Consumer consumer;
    private Producer producer;
    private InterestSerializer serializer;

    public BankGateway() {
        this.consumer = new Consumer(JMS_CONSUMER_QUEUE_NAME);
        this.producer = new Producer(JMS_PRODUCER_QUEUE_NAME);
        this.serializer = new InterestSerializer();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                BankInterestReply rep = this.serializer.deserializeBankInterestReply(msg.getText());
                onBankInterestRequestArrived(rep, message.getJMSCorrelationID());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void applyForLoan(BankInterestRequest req, String correlation) throws JMSException {

        String json = this.serializer.serializeBankInterestRequest(req);
        Message msg = this.producer.createMessage(json);
        msg.setJMSCorrelationID(correlation);
        this.producer.send(msg);
    }

    public void onBankInterestRequestArrived(BankInterestReply rep, String correlation) { }
}
