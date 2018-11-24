package brokerclient.messageGateway;

import brokerclient.model.LoanReply;
import brokerclient.model.LoanRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

public class ClientGateway {

    // queue names
    private static final String JMS_CONSUMER_QUEUE_NAME = "broker-client";
    private static final String JMS_PRODUCER_QUEUE_NAME = "loan-client";

    // Declare producer, consumer and serializer
    private Producer producer;
    private Consumer consumer;
    private LoanSerializer serializer;

    public ClientGateway() {

        this.consumer = new Consumer(JMS_CONSUMER_QUEUE_NAME);
        this.producer = new Producer(JMS_PRODUCER_QUEUE_NAME);
        this.serializer = new LoanSerializer();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                LoanRequest req = this.serializer.deserializeLoanRequest(msg.getText());
                onLoanReplyArrived(req, message.getJMSMessageID());
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void replyOnRequest(LoanReply rep, String correlation) throws JMSException {

        String json = this.serializer.serializeLoanReply(rep);
        Message msg = this.producer.createMessage(json);
        msg.setJMSCorrelationID(correlation);
        this.producer.send(msg);
    }

    public void onLoanReplyArrived(LoanRequest req, String correlation) {}
}
