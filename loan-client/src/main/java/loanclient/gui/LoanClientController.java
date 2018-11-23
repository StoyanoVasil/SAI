package loanclient.gui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import loanclient.messageGateway.Consumer;
import loanclient.messageGateway.Producer;
import loanclient.model.LoanReply;
import loanclient.model.LoanRequest;

import javax.jms.*;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
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

    // map to store msgId -> LoanRequest
    private Map<String, LoanRequest> idToRequest;

    public LoanClientController() {

        this.gson = new Gson();
        this. idToRequest = new HashMap<>();

        // initialize consumer and producer
        this.producer = new Producer(JMS_BROKER_QUEUE_NAME);
        this.consumer = new Consumer(JMS_CLIENT_QUEUE_NAME);

        // set event listener
        this.consumer.setMessageListener(message -> {
            try {
                LoanReply reply = deserializeLoanReply(message);
                if(reply == null) return;

                // get LoanRequest from map
                String id = message.getJMSCorrelationID();
                LoanRequest req = this.idToRequest.get(id);
                ListViewLine lvl = this.getLvlForLoanRequest(req);
                if(lvl == null) return;

                // set LoanReply
                lvl.setLoanReply(reply);
                addLVLToLv(lvl);
                // remove from map
                this.idToRequest.remove(id);
            } catch (JMSException e) {
                e.printStackTrace();
            }
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
            addLVLToLv(lvl);

            // create message
            Message msg = this.producer.createMessage(this.gson.toJson(loanRequest));

            // send message
            String msgId = producer.send(msg);

            // add to map
            this.idToRequest.put(msgId, loanRequest);
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

    public void addLVLToLv(ListViewLine lvl) {

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

    private LoanReply deserializeLoanReply(Message message) {

        try {
            TextMessage msg = (TextMessage) message;
            return this.gson.fromJson(msg.getText(), LoanReply.class);
        } catch (JMSException e) {
            return null;
        }
    }

    private ListViewLine getLvlForLoanRequest(LoanRequest req) {

        for(ListViewLine lvl : this.lvLoanRequestReply.getItems()) {
            if(req.equals(lvl.getLoanRequest())) {
                return lvl;
            }
        }
        return null;
    }
}
