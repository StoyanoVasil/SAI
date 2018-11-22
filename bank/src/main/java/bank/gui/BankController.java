package bank.gui;

import bank.model.BankInterestReply;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;

public class BankController implements Initializable {

    // connection details and queue names
    private final String BANK_ID = "ABN";
    private static final String JMS_CONNECTION = "tcp://localhost:61616";
    private static final String JMS_BANK_QUEUE_NAME = "abn-bank";
    private static final String JMS_BROKER_QUEUE_NAME = "broker-bank";

    // bank connection objects
    private Connection bankConnection;
    private Session bankSession;
    private Destination bankDestination;
    private MessageConsumer bankConsumer;

    // broker connection objects
    private Connection brokerConnection;
    private Session brokerSession;
    private Destination brokerDestination;
    private MessageProducer brokerProducer;

    // (de)serialization object
    private Gson gson;

    // javafx objects
    public ListView<ListViewLine> lvBankRequestReply;
    public TextField tfInterest;

    // Map to store ssn
    public Map<ListViewLine, String> lvlToSsn;

    public BankController() {

        try {
            // initialize Gson, consumer, producer
            this.gson = new Gson();
            this.bankConsumer = initMessageConsumer();
            this.brokerProducer = initMessageProducer();

            // initialize map
            lvlToSsn = new HashMap<>();

            if (this.bankConsumer == null || this.brokerProducer == null) return;

            // set event listener
            this.bankConsumer.setMessageListener(message -> {

                try {
                    // get ListViewLine from message and deserialize from JSON
                    ListViewLine lvl = deserializeListViewLine(message);
                    if (lvl == null) return;

                    // add to map
                    this.lvlToSsn.put(lvl, message.getJMSCorrelationID());

                    // add ListViewLine to the listview
                    addMessageToLv(lvl);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            });

            // start connneciton
            this.bankConnection.start();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void btnSendBankInterestReplyClicked(){

        try {
            // set BankInterestReply
            double interest = Double.parseDouble(tfInterest.getText());
            ListViewLine lvl = lvBankRequestReply.getFocusModel().getFocusedItem();
            String ssn = this.lvlToSsn.get(lvl);
            lvl.setBankInterestReply(new BankInterestReply(interest, BANK_ID));

            // send back reply
            Message msg = this.brokerSession.createTextMessage(this.gson.toJson(lvl));
            msg.setJMSCorrelationID(ssn);
            this.brokerProducer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public ListViewLine deserializeListViewLine(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), ListViewLine.class);
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addMessageToLv(ListViewLine lvl) {

        Platform.runLater(() -> {
            lvBankRequestReply.getItems().add(lvl);
        });
    }

    private MessageConsumer initMessageConsumer() {

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
            return bankSession.createConsumer(this.bankDestination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private MessageProducer initMessageProducer() {

        try {
            // set properties
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, JMS_CONNECTION);
            props.put(("queue." + JMS_BROKER_QUEUE_NAME), JMS_BROKER_QUEUE_NAME);

            // create connection and session
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            this.brokerConnection = connectionFactory.createConnection();
            this.brokerSession = brokerConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create brokerDestination and producer
            this.brokerDestination = (Destination) jndiContext.lookup(JMS_BROKER_QUEUE_NAME);
            return brokerSession.createProducer(this.brokerDestination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
            return null;
        }
    }
}
