package brokerclient.gui;

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
    private static final String JMS_CONNECTION = "tcp://localhost:61616";
    private static final String JMS_CLIENT_INBOX_QUEUE_NAME = "broker-client";
    private static final String JMS_BANK_INBOX_QUEUE_NAME = "broker-bank";
    private static final String JMS_CLIENT_QUEUE_NAME = "loan-client";
    private static final String JMS_BANK_QUEUE_NAME = "abn-bank";

    // client producer connection objects
    private Connection clientConnection;
    private Session clientSession;
    private Destination clientDestination;
    private MessageProducer clientProducer;

    // broker-bank connection objects
    private Connection brokerBankConnection;
    private Session brokerBankSession;
    private Destination brokerBankDestination;
    private MessageConsumer brokerBankConsumer;

    // broker-client connection objects
    private Connection brokerClientConnection;
    private Session brokerClientSession;
    private Destination brokerClientDestination;
    private MessageConsumer brokerClientConsumer;

    // bank connection objects
    private Connection bankConnection;
    private Session bankSession;
    private Destination bankDestination;
    private MessageProducer bankProducer;

    // (de)serialization object
    private Gson gson;

    // javafx objects
    public ListView<ClientListViewLine> lvBroker;

    public BrokerClientController() {

        try {
            // intialize Gson, consumer, producers
            this.gson = new Gson();
            this.clientProducer = initClientProducer();
            this.bankProducer = initBankProducer();
            this.brokerClientConsumer = initBrokerClientConsumer();
            this.brokerBankConsumer = initBrokerBankConsumer();

            // set consumer event listeners
            this.brokerClientConsumer.setMessageListener(message -> {

                ClientListViewLine lvl = deserializeClientListViewLine(message);
                ClientListViewLine localLvl = getRequestReply(lvl.getLoanRequest());
                handleClientMessage(lvl);
            });

            this.brokerBankConsumer.setMessageListener(message -> {

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

            //start connections
            this.brokerBankConnection.start();
            this.brokerClientConnection.start();
        } catch (JMSException e) {
            e.printStackTrace();
        }
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

    private MessageProducer initClientProducer() {

        try {
            // set properties
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, JMS_CONNECTION);
            props.put(("queue." + JMS_CLIENT_QUEUE_NAME), JMS_CLIENT_QUEUE_NAME);

            // create connection and session
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            this.clientConnection = connectionFactory.createConnection();
            this.clientSession = clientConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create clientDestination and consumer
            this.clientDestination = (Destination) jndiContext.lookup(JMS_CLIENT_QUEUE_NAME);
            return clientSession.createProducer(this.clientDestination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MessageProducer initBankProducer() {

        try {
            // set properties
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, JMS_CONNECTION);
            props.put(("queue." + JMS_BANK_QUEUE_NAME), JMS_BANK_QUEUE_NAME);

            // create connection and session
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            this.bankConnection = connectionFactory.createConnection();
            this.bankSession = bankConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create bankDestination and consumer
            this.bankDestination = (Destination) jndiContext.lookup(JMS_BANK_QUEUE_NAME);
            return bankSession.createProducer(this.bankDestination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MessageConsumer initBrokerClientConsumer() {

        try {
            // set properties
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, JMS_CONNECTION);
            props.put(("queue." + JMS_CLIENT_INBOX_QUEUE_NAME), JMS_CLIENT_INBOX_QUEUE_NAME);

            // create connection and session
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            this.brokerClientConnection = connectionFactory.createConnection();
            this.brokerClientSession = brokerClientConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create brokerDestination and producer
            this.brokerClientDestination = (Destination) jndiContext.lookup(JMS_CLIENT_INBOX_QUEUE_NAME);
            return brokerClientSession.createConsumer(this.brokerClientDestination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MessageConsumer initBrokerBankConsumer() {

        try {
            // set properties
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, JMS_CONNECTION);
            props.put(("queue." + JMS_BANK_INBOX_QUEUE_NAME), JMS_BANK_INBOX_QUEUE_NAME);

            // create connection and session
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            this.brokerBankConnection = connectionFactory.createConnection();
            this.brokerBankSession = brokerBankConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create brokerDestination and producer
            this.brokerBankDestination = (Destination) jndiContext.lookup(JMS_BANK_INBOX_QUEUE_NAME);
            return brokerBankSession.createConsumer(this.brokerBankDestination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void handleBankMessage(ClientListViewLine lvl) {

        try {
            addListViewLineToLv(lvl);
            Message msg = this.clientSession.createTextMessage(this.gson.toJson(lvl));
            this.clientProducer.send(msg);
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
            Message msg = this.bankSession.createTextMessage(this.gson.toJson(blvl));
            msg.setJMSCorrelationID(Integer.toString(lvl.getLoanRequest().getSsn()));
            this.bankProducer.send(msg);
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
