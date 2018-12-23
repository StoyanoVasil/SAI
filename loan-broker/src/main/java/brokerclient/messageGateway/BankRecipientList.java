package brokerclient.messageGateway;

import brokerclient.model.BankInterestRequest;
import net.sourceforge.jeval.EvaluationException;
import net.sourceforge.jeval.Evaluator;

import javax.jms.JMSException;

public class BankRecipientList {

    private static final String ingRule = "#{amount} <= 100000 && #{time} <= 10";
    private static final String abnRule = "#{amount} >= 200000 && #{amount} <= 300000 && #{time} <= 20";
    private static final String raboRule = "#{amount} <= 250000 && #{time} <= 15";

    private BankGateway bankGateway;
    private InterestSerializer serializer;

    public BankRecipientList(BankGateway gateway) {

        this.bankGateway = gateway;
        this.serializer = new InterestSerializer();
    }

    public int sendRequest(BankInterestRequest req, Integer aggregationId) throws JMSException {

        Evaluator evaluator = getEvaluator(req);
        int countSent = 0;

        try {
            if (evaluator.evaluate(ingRule).equals("1.0")) {
                this.bankGateway.applyForLoan(req, aggregationId, "ing-bank");
                countSent++;
            }
            if (evaluator.evaluate(abnRule).equals("1.0")) {
                this.bankGateway.applyForLoan(req, aggregationId, "abn-bank");
                countSent++;
            }
            if (evaluator.evaluate(raboRule).equals("1.0")) {
                this.bankGateway.applyForLoan(req, aggregationId, "rabo-bank");
                countSent++;
            }
        } catch (EvaluationException e) { e.printStackTrace(); }

        return countSent;
    }

    private Evaluator getEvaluator(BankInterestRequest req) {

        Evaluator evaluator = new Evaluator();
        evaluator.putVariable("amount", Integer.toString(req.getAmount()));
        evaluator.putVariable("time", Integer.toString(req.getTime()));
        return evaluator;
    }
}