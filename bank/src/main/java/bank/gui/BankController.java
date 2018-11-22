package bank.gui;

import bank.messageGateway.Consumer;
import bank.messageGateway.Producer;
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
    private static final String JMS_BANK_QUEUE_NAME = "abn-bank";
    private static final String JMS_BROKER_QUEUE_NAME = "broker-bank";

    // (de)serialization object
    private Gson gson;

    // javafx objects
    public ListView<ListViewLine> lvBankRequestReply;
    public TextField tfInterest;

    // Map to store ssn
    private Map<ListViewLine, String> lvlToSsn;

    // consumer and producer
    private Consumer consumer;
    private Producer producer;

    public BankController() {

        // initialize objects
        this.gson = new Gson();
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.consumer = new Consumer(JMS_BANK_QUEUE_NAME);
        this.lvlToSsn = new HashMap<>();

        // set event listener
        this.consumer.setMessageListener(message -> {
            try {
                // get ListViewLine from message and deserialize from JSON
                ListViewLine lvl = deserializeListViewLine(message);
                if (lvl == null) return;

                // add to map
                lvlToSsn.put(lvl, message.getJMSCorrelationID());

                // add ListViewLine to the listview
                addMessageToLv(lvl);
            } catch (JMSException e) {
                e.printStackTrace();
            }
        });
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
            this.producer.send(lvl, ssn);
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
}
