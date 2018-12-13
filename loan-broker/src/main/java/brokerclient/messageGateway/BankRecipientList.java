package brokerclient.messageGateway;

import brokerclient.model.BankInterestRequest;
import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

import javax.jms.JMSException;
import javax.jms.Message;

public class BankRecipientList {

    private static final String ingRule = "#{amount} <= 100000 && #{time} <= 10";
    private static final String abnRule = "#{amount} >= 200000 && #{amount} <= 300000 && #{time} <= 20";
    private static final String raboRule = "#{amount} <= 250000 && #{time} <= 15";

    private Producer ingProducer;
    private Producer abnProducer;
    private Producer raboProducer;
    private InterestSerializer serializer;

    public BankRecipientList(String ingQueue, String abnQueue, String raboQueue) {

        this.ingProducer = new Producer(ingQueue);
        this.abnProducer = new Producer(abnQueue);
        this.raboProducer = new Producer(raboQueue);
        this.serializer = new InterestSerializer();
    }

    public int sendRequest(BankInterestRequest req, int aggregationId) throws JMSException {

        Evaluator evaluator = getEvaluator(req);
        int countSent = 0;

        try {
            if (evaluator.evaluate(ingRule).equals("1.0")) {
                send(ingProducer, req, aggregationId);
                countSent++;
            }
            if (evaluator.evaluate(abnRule).equals("1.0")) {
                send(abnProducer, req, aggregationId);
                countSent++;
            }
            if (evaluator.evaluate(raboRule).equals("1.0")) {
                send(raboProducer, req, aggregationId);
                countSent++;
            }
        } catch (EvaluationException e) { e.printStackTrace(); }

        return countSent;
    }

    private void send(Producer producer, BankInterestRequest req, int aggregationId) throws JMSException {

        Message msg = producer.createMessage(this.serializer.serializeBankInterestRequest(req));
        msg.setIntProperty("aggregationID", aggregationId);
        producer.send(msg);
    }

    private Evaluator getEvaluator(BankInterestRequest req) {

        Evaluator evaluator = new Evaluator();
        evaluator.putVariable("amount", Integer.toString(req.getAmount()));
        evaluator.putVariable("time", Integer.toString(req.getTime()));
        return evaluator;
    }
}
