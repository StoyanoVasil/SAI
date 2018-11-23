package loanclient.gui;

import com.google.gson.Gson;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import loanclient.messageGateway.Consumer;
import loanclient.messageGateway.Gateway;
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

    // javafx objects
    public TextField tfSsn;
    public TextField tfAmount;
    public TextField tfTime;
    public ListView<ListViewLine> lvLoanRequestReply;

    // Declare gateway
    private Gateway gateway;

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
            this.gateway.applyForLoan(loanRequest);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tfSsn.setText("123456");
        tfAmount.setText("80000");
        tfTime.setText("30");

        this.gateway = new Gateway() {
            public void onLoanReplyArrived(LoanRequest req, LoanReply rep) {
                ListViewLine lvl = getLvlForLoanRequest(req);
                lvl.setLoanReply(rep);
                addLVLToLv(lvl);
            }
        };
    }

    public void addLVLToLv(ListViewLine lvl) {

        Platform.runLater(() -> {
            this.lvLoanRequestReply.getItems().remove(lvl);
            this.lvLoanRequestReply.getItems().add(lvl);
        });
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
