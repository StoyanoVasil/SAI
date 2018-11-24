package brokerclient.gui;

import brokerclient.messageGateway.BankGateway;
import brokerclient.messageGateway.ClientGateway;
import brokerclient.model.BankInterestReply;
import brokerclient.model.BankInterestRequest;
import brokerclient.model.LoanReply;
import brokerclient.model.LoanRequest;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import javax.jms.JMSException;
import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BrokerClientController {

    // connection details and queue names

    // Declare gateways
    private ClientGateway clientGateway;
    private BankGateway bankGateway;

    // javafx objects
    public ListView<ListViewLine> lvBroker;

    // map correlation to ListViewLine
    private Map<String, ListViewLine> map;

    public BrokerClientController() {

        this.map = new HashMap<>();

        // initialize gateways
        this.clientGateway = new ClientGateway() {
            public void onLoanReplyArrived(LoanRequest req, String correlation) {
                try {
                    ListViewLine lvl = new ListViewLine(req);
                    addListViewLineToLv(lvl);
                    map.put(correlation, lvl);
                    BankInterestRequest request = new BankInterestRequest(req.getAmount(), req.getTime());
                    bankGateway.applyForLoan(request, correlation);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        };

        this.bankGateway = new BankGateway() {
            public void onBankInterestRequestArrived(BankInterestReply rep, String correlation) {
                try {
                    LoanReply reply = new LoanReply(rep.getInterest(), rep.getQuoteId());
                    ListViewLine lvl = map.get(correlation);
                    lvl.setLoanReply(reply);
                    addListViewLineToLv(lvl);
                    clientGateway.replyOnRequest(reply, correlation);
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

    private ListViewLine getRequestReply(LoanRequest request) {

        for (ListViewLine lvl : this.lvBroker.getItems()) {
            if(request.equals(lvl.getLoanRequest())) {
                return lvl;
            }
        }
        return null;
    }
}
