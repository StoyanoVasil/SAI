package bank.gui;

import bank.messageGateway.Consumer;
import bank.messageGateway.Producer;
import bank.model.BankInterestReply;
import bank.model.BankInterestRequest;
import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import javax.jms.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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
    private Map<ListViewLine, String> lvlToCorrelation;

    // consumer and producer
    private Consumer consumer;
    private Producer producer;

    public BankController() {

        // initialize objects
        this.gson = new Gson();
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.consumer = new Consumer(JMS_BANK_QUEUE_NAME);
        this.lvlToCorrelation = new HashMap<>();

        // set event listener
        this.consumer.setMessageListener(message -> {
            try {
                // get BankInterestRequest from message and deserialize from JSON
                BankInterestRequest req = deserializeBankInterestRequest(message);
                if (req == null) return;
                ListViewLine lvl = new ListViewLine(req);

                // add to map
                lvlToCorrelation.put(lvl, message.getJMSCorrelationID());

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
            BankInterestReply reply = new BankInterestReply(interest, BANK_ID);
            String id = this.lvlToCorrelation.get(lvl);

            // create message
            Message msg = this.producer.createMessage(this.gson.toJson(reply));
            msg.setJMSCorrelationID(id);
            // send back reply
            this.producer.send(msg);

            // update ui
            lvl.setBankInterestReply(reply);
            this.lvlToCorrelation.remove(lvl);
            addMessageToLv(lvl);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {

    }

    public BankInterestRequest deserializeBankInterestRequest(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), BankInterestRequest.class);
        } catch (JMSException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void addMessageToLv(ListViewLine lvl) {

        Platform.runLater(() -> {
            Iterator<ListViewLine> iterator = lvBankRequestReply.getItems().iterator();
            while (iterator.hasNext()) {
                ListViewLine temp = iterator.next();
                if (temp.getBankInterestRequest().equals(lvl.getBankInterestRequest())) {
                    iterator.remove();
                    break;
                }
            }
            lvBankRequestReply.getItems().add(lvl);
        });
    }
}
