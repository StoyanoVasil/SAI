package brokerclient.gui;

import brokerclient.messageGateway.BankGateway;
import brokerclient.messageGateway.ClientGateway;
import brokerclient.model.*;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import javax.jms.JMSException;
import javax.swing.*;
import java.util.Iterator;

public class BrokerClientController {

    // Declare gateways
    private ClientGateway clientGateway;
    private BankGateway bankGateway;

    // javafx objects
    public ListView<ListViewLine> lvBroker;

    public BrokerClientController() {

        // initialize gateways
        this.clientGateway = new ClientGateway() {
            public void onLoanRequestArrived(LoanRequest req) {
                try {
                    ListViewLine lvl = new ListViewLine(req);
                    addListViewLineToLv(lvl);
                    bankGateway.applyForLoan(req);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        };

        this.bankGateway = new BankGateway() {
            public void onBankInterestRequestArrived(LoanRequest req, LoanReply rep) {
                try {
                    ListViewLine lvl = getLvlForLoanRequest(req);
                    lvl.setLoanReply(rep);
                    addListViewLineToLv(lvl);
                    clientGateway.replyOnRequest(req, rep);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void addListViewLineToLv(ListViewLine lvl) {

        Platform.runLater(() -> {
            Iterator<ListViewLine> iterator = lvBroker.getItems().iterator();
            while (iterator.hasNext()) {
                ListViewLine temp = iterator.next();
                if (temp.getLoanRequest().getSsn() == lvl.getLoanRequest().getSsn()) {
                    iterator.remove();
                    break;
                }
            }
            lvBroker.getItems().add(lvl);
        });
    }

    private ListViewLine getLvlForLoanRequest(LoanRequest req) {

        for(ListViewLine lvl : this.lvBroker.getItems()) {
            if(req.equals(lvl.getLoanRequest())) {
                return lvl;
            }
        }
        return null;
    }
}
