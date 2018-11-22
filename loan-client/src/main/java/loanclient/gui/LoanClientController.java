package loanclient.gui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import loanclient.messageGateway.Consumer;
import loanclient.messageGateway.Producer;
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
    private static final String JMS_CLIENT_QUEUE_NAME = "loan-client";
    private static final String JMS_BROKER_QUEUE_NAME = "broker-client";

    // javafx objects
    public TextField tfSsn;
    public TextField tfAmount;
    public TextField tfTime;
    public ListView<ListViewLine> lvLoanRequestReply;

    // consumer and producer
    private Producer producer;
    private Consumer consumer;

    // (de)serialize object
    private Gson gson;

    public LoanClientController() {

        this.gson = new Gson();

        // initialize consumer and producer
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.consumer = new Consumer(JMS_CLIENT_QUEUE_NAME);

        // set event listener
        this.consumer.setMessageListener(message -> {
            ListViewLine lvl = deserializeListViewLine(message);
            if(lvl == null) return;
            addMessageToLv(lvl);
        });
    }

    @FXML
    public void btnSendLoanRequestClicked(){

        try {
            // create the BankInterestRequest
            int ssn = Integer.parseInt(tfSsn.getText());
            int amount = Integer.parseInt(tfAmount.getText());
            int time = Integer.parseInt(tfTime.getText());
            LoanRequest loanRequest = new LoanRequest(ssn, amount, time);

            // create the ListView line with the request and add it to lvLoanRequestReply
            ListViewLine lvl = new ListViewLine(loanRequest);
            addMessageToLv(lvl);

            // send lvl
             producer.send(lvl);
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

    private ListViewLine deserializeListViewLine(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), ListViewLine.class);
        } catch (JMSException e) {
            return null;
        }
    }
}
