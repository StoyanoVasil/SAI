package bank.gui;

import bank.messageGateway.Gateway;
import bank.model.BankInterestReply;
import bank.model.BankInterestRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import javax.jms.*;
import java.net.URL;
import java.util.*;

public class BankController implements Initializable {

    // connection details and queue names
    private String BANK_ID;
    private String BANK_QUEUE;

    // javafx objects
    public ListView<ListViewLine> lvBankRequestReply;
    public TextField tfInterest;

    // declare gateway
    private Gateway gateway;

    public void initData(String bankName, String bankQueue) {
        this.BANK_ID = bankName;
        this.BANK_QUEUE = bankQueue;

        this.gateway = new Gateway(this.BANK_QUEUE) {
            public void onBankInterestRequestArrived(BankInterestRequest req) {
                ListViewLine lvl = new ListViewLine(req);
                addLvlToLv(lvl);
            }
        };
    }

    @FXML
    public void btnSendBankInterestReplyClicked(){

        try {
            // set BankInterestReply
            double interest = Double.parseDouble(tfInterest.getText());
            ListViewLine lvl = lvBankRequestReply.getFocusModel().getFocusedItem();
            BankInterestReply reply = new BankInterestReply(interest, BANK_ID);

            // send message
            this.gateway.replyOnRequest(lvl.getBankInterestRequest(), reply);

            // update ui
            lvl.setBankInterestReply(reply);
            addLvlToLv(lvl);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {


    }

    public void addLvlToLv(ListViewLine lvl) {

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
