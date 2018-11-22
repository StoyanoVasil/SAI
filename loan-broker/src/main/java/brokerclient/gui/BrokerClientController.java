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
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Iterator;
import java.util.Properties;

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
    public ListView<ClientListViewLine> lvBroker;

    public BrokerClientController() {

        // intialize Gson, consumer, producers
        this.gson = new Gson();
        this.clientProducer = new Producer(JMS_CLIENT_QUEUE_NAME);
        this.bankProducer = new Producer(JMS_BANK_QUEUE_NAME);
        this.clientConsumer = new Consumer(JMS_CLIENT_INBOX_QUEUE_NAME);
        this.bankConsumer = new Consumer(JMS_BANK_INBOX_QUEUE_NAME);

        // set consumer event listeners
        this.clientConsumer.setMessageListener(message -> {

            ClientListViewLine lvl = deserializeClientListViewLine(message);
            ClientListViewLine localLvl = getRequestReply(lvl.getLoanRequest());
            handleClientMessage(lvl);
        });

        this.bankConsumer.setMessageListener(message -> {

            try {
                BankListViewLine blvl = deserializeBankListViewLine(message);
                BankInterestRequest bReq = blvl.getBankInterestRequest();
                BankInterestReply bRep = blvl.getBankInterestReply();

                LoanRequest ln = new LoanRequest(Integer.parseInt(message.getJMSCorrelationID()),
                        bReq.getAmount(), bReq.getTime());
                LoanReply lr = new LoanReply(bRep.getInterest(), bRep.getQuoteId());
                ClientListViewLine lvl = new ClientListViewLine(ln);
                lvl.setLoanReply(lr);
                handleBankMessage(lvl);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
    }

    public ClientListViewLine deserializeClientListViewLine(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), ClientListViewLine.class);
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public BankListViewLine deserializeBankListViewLine(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), BankListViewLine.class);
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addListViewLineToLv(ClientListViewLine lvl) {

        Platform.runLater(() -> {
            Iterator<ClientListViewLine> iterator = lvBroker.getItems().iterator();
            while (iterator.hasNext()) {
                ClientListViewLine temp = iterator.next();
                if (temp.getLoanRequest().getSsn() == lvl.getLoanRequest().getSsn()) {
                    iterator.remove();
                    break;
                }
            }
            lvBroker.getItems().add(lvl);
        });
    }

    private void handleBankMessage(ClientListViewLine lvl) {

        try {
            addListViewLineToLv(lvl);
            this.clientProducer.send(lvl);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private void handleClientMessage(ClientListViewLine lvl) {

        try {
            addListViewLineToLv(lvl);
            LoanRequest lr = lvl.getLoanRequest();
            BankInterestRequest bReq = new BankInterestRequest(lr.getAmount(), lr.getTime());
            BankListViewLine blvl = new BankListViewLine(bReq);
            String ssn = Integer.toString(lvl.getLoanRequest().getSsn());
            this.bankProducer.send(blvl, ssn);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    private ClientListViewLine getRequestReply(LoanRequest request) {

        for (int i = 0; i < lvBroker.getItems().size(); i++) {
            ClientListViewLine rr =  lvBroker.getItems().get(i);
            if (rr.getLoanRequest() != null && rr.getLoanRequest().getSsn() == request.getSsn()) {
                return rr;
            }
        }
        return null;
    }
}
