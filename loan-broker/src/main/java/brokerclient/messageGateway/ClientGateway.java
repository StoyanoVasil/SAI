package brokerclient.messageGateway;

import brokerclient.model.LoanReply;
import brokerclient.model.LoanRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

public class ClientGateway {

    // queue names
    private static final String JMS_CONSUMER_QUEUE_NAME = "broker-client";
    private static final String JMS_PRODUCER_QUEUE_NAME = "loan-client";

    // Declare producer, consumer and serializer
    private Producer producer;
    private Consumer consumer;
    private LoanSerializer serializer;
    private Map<LoanRequest, String> map;

    public ClientGateway() {

        this.consumer = new Consumer(JMS_CONSUMER_QUEUE_NAME);
        this.producer = new Producer(JMS_PRODUCER_QUEUE_NAME);
        this.serializer = new LoanSerializer();
        this.map = new HashMap<>();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                LoanRequest req = this.serializer.deserializeLoanRequest(msg.getText());
                this.map.put(req, message.getJMSMessageID());
                onLoanRequestArrived(req);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void replyOnRequest(LoanRequest req, LoanReply rep) throws JMSException {

        String json = this.serializer.serializeLoanReply(rep);
        Message msg = this.producer.createMessage(json);
        msg.setJMSCorrelationID(this.map.get(req));
        this.producer.send(msg);
    }

    public void onLoanRequestArrived(LoanRequest req) {}
}
