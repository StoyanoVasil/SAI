package loanclient.messageGateway;

import loanclient.model.LoanReply;
import loanclient.model.LoanRequest;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import java.util.HashMap;
import java.util.Map;

public class Gateway {

    private static final String JMS_CLIENT_QUEUE_NAME = "loan-client";
    private static final String JMS_BROKER_QUEUE_NAME = "broker-client";

    private Consumer consumer;
    private Producer producer;
    private LoanSerializer ls;
    private Map<String, LoanRequest> map;

    public Gateway() {
        this.consumer = new Consumer(JMS_CLIENT_QUEUE_NAME);
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.ls = new LoanSerializer();
        this.map = new HashMap<>();

        this.consumer.setMessageListener(message -> {
            try {
                TextMessage msg = (TextMessage) message;
                LoanReply rep = this.ls.deserializeLoanReply(msg.getText());
                LoanRequest req = this.map.get(msg.getJMSCorrelationID());
                onLoanReplyArrived(req, rep);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public void applyForLoan(LoanRequest req) throws JMSException {

        String json = this.ls.serializeLoanRequest(req);
        Message msg = this.producer.createMessage(json);
        this.producer.send(msg);
        this.map.put(msg.getJMSMessageID(), req);
    }

    public void onLoanReplyArrived(LoanRequest req, LoanReply rep) { }
}
