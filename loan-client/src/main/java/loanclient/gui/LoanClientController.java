package loanclient.gui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import loanclient.model.LoanRequest;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.net.URL;
import java.util.Iterator;
import java.util.Properties;
import java.util.ResourceBundle;

public class LoanClientController implements Initializable {

    // connection details and queue names
    private static final String JMS_CONNECTION = "tcp://localhost:61616";
    private static final String JMS_CLIENT_QUEUE_NAME = "loan-client";
    private static final String JMS_BROKER_QUEUE_NAME = "broker-client";

    // client connection objects
    private Connection clientConnection;
    private Session clientSession;
    private Destination clientDestination;
    private MessageConsumer clientConsumer;

    // broker connection objects
    private Connection brokerConnection;
    private Session brokerSession;
    private Destination brokerDestination;
    private MessageProducer brokerProducer;

    // (de)serialization object
    private Gson gson;

    // javafx objects
    public TextField tfSsn;
    public TextField tfAmount;
    public TextField tfTime;
    public ListView<ListViewLine> lvLoanRequestReply;

    public LoanClientController() {

        try {
            // initialize Gson, consumer, producer
            this.gson = new Gson();
            this.clientConsumer = initMessageConsumer();
            this.brokerProducer = initMessageProducer();

            if (this.clientConsumer == null || this.brokerProducer == null) return;

            // set event listener
            this.clientConsumer.setMessageListener(message -> {

                // get ListViewLine from message and deserialize from JSON
                ListViewLine lvl = deserializeListViewLine(message);
                if (lvl == null) return;

                // add ListViewLine to the listview
                addMessageToLv(lvl);
            });

            // start connneciton
            this.clientConnection.start();
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void btnSendLoanRequestClicked(){

        try {
            // create the BankInterestRequest
            int ssn = Integer.parseInt(tfSsn.getText());
            int amount = Integer.parseInt(tfAmount.getText());
            int time = Integer.parseInt(tfTime.getText());
            LoanRequest loanRequest = new LoanRequest(ssn, amount, time);

            //create the ListView line with the request and add it to lvLoanRequestReply
            ListViewLine listViewLine = new ListViewLine(loanRequest);
            addMessageToLv(listViewLine);

            //send lvl
            Message msg = this.brokerSession.createTextMessage(this.gson.toJson(listViewLine));
            this.brokerProducer.send(msg);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tfSsn.setText("123456");
        tfAmount.setText("80000");
        tfTime.setText("30");
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
            Iterator<ListViewLine> iterator = lvLoanRequestReply.getItems().iterator();
            while (iterator.hasNext()) {
                ListViewLine temp = iterator.next();
                if (temp.getLoanRequest().getSsn() == lvl.getLoanRequest().getSsn()) {
                    iterator.remove();
                    break;
                }
            }
            lvLoanRequestReply.getItems().add(lvl);
        });
    }

    private MessageConsumer initMessageConsumer() {

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
            return clientSession.createConsumer(this.clientDestination);
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
