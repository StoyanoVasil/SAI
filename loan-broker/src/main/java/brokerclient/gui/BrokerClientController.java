package brokerclient.gui;

import brokerclient.messageGateway.Consumer;
import brokerclient.messageGateway.Producer;
import brokerclient.model.BankInterestReply;
import brokerclient.model.BankInterestRequest;
import brokerclient.model.LoanReply;
import brokerclient.model.LoanRequest;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import javax.jms.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BrokerClientController {

    // connection details and queue names
    private static final String JMS_CLIENT_INBOX_QUEUE_NAME = "broker-client";
    private static final String JMS_BANK_INBOX_QUEUE_NAME = "broker-bank";
    private static final String JMS_CLIENT_QUEUE_NAME = "loan-client";
    private static final String JMS_BANK_QUEUE_NAME = "abn-bank";

    // (de)serialization object
    private Gson gson;

    // consumers and producers
    private Consumer clientConsumer;
    private Consumer bankConsumer;
    private Producer clientProducer;
    private Producer bankProducer;

    // javafx objects
    public ListView<ListViewLine> lvBroker;

    // map correlationId to LoanRequest
    private Map<String, LoanRequest> idToLoanRequest;

    public BrokerClientController() {

        // intialize Gson, consumer, producers
        this.gson = new Gson();
        this.clientProducer = new Producer(JMS_CLIENT_QUEUE_NAME);
        this.bankProducer = new Producer(JMS_BANK_QUEUE_NAME);
        this.clientConsumer = new Consumer(JMS_CLIENT_INBOX_QUEUE_NAME);
        this.bankConsumer = new Consumer(JMS_BANK_INBOX_QUEUE_NAME);
        this.idToLoanRequest = new HashMap<>();

        // set consumer event listeners
        this.clientConsumer.setMessageListener(message -> {

            try {
                handleClientMessage(message);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });

        this.bankConsumer.setMessageListener(message -> {

            try {
                handleBankMessage(message);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public LoanRequest deserializeLoanRequest(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), LoanRequest.class);
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BankInterestReply deserializeBankInterestReply(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), BankInterestReply.class);
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addListViewLineToLv(ListViewLine lvl) {

        Platform.runLater(() -> {
            Iterator<ListViewLine> iterator = lvBroker.getItems().iterator();
            while (iterator.hasNext()) {
                ListViewLine temp = iterator.next();
                if (temp.getLoanRequest().getSsn() == lvl.getLoanRequest().getSsn()) {
                    iterator.remove();
                    break;
                }
            }
            lvBroker.getItems().add(lvl);
        });
    }

    private void handleBankMessage(Message message)  throws JMSException {

        // get necessary information
        BankInterestReply reply = deserializeBankInterestReply(message);
        LoanReply rep = new LoanReply(reply.getInterest(), reply.getQuoteId());

        // create message
        Message msg = this.clientProducer.createMessage(this.gson.toJson(rep));
        String id = message.getJMSCorrelationID();
        msg.setJMSCorrelationID(id);
        this.clientProducer.send(msg);

        // update ui
        ListViewLine lvl = getRequestReply(this.idToLoanRequest.get(id));
        lvl.setLoanReply(rep);
        addListViewLineToLv(lvl);
    }

    private void handleClientMessage(Message message) throws JMSException {

        // get necessary information
        LoanRequest req = deserializeLoanRequest(message);
        ListViewLine lvl = new ListViewLine(req);
        BankInterestRequest bReq = new BankInterestRequest(req.getAmount(), req.getTime());
        String id = message.getJMSMessageID();
        this.idToLoanRequest.put(id, req);

        // create message
        Message msg = this.bankProducer.createMessage(this.gson.toJson(req));
        msg.setJMSCorrelationID(id);
        this.bankProducer.send(msg);

        // update ui
        addListViewLineToLv(lvl);
    }

    private ListViewLine getRequestReply(LoanRequest request) {

        for (ListViewLine lvl : this.lvBroker.getItems()) {
            if(request.equals(lvl.getLoanRequest())) {
                return lvl;
            }
        }
        return null;
    }
}
