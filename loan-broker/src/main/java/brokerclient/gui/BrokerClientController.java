package brokerclient.gui;

import brokerclient.messageGateway.Archiver;
import brokerclient.messageGateway.BankGateway;
import brokerclient.messageGateway.ClientGateway;
import brokerclient.messageGateway.CreditHistoryEnricher;
import brokerclient.model.*;
import javafx.application.Platform;
import javafx.scene.control.ListView;

import javax.jms.JMSException;
import javax.swing.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class BrokerClientController {

    // Declare gateways
    private ClientGateway clientGateway;
    private BankGateway bankGateway;

    // Declare Acrhiver
    private Archiver archiver;

    // Declare content enricher
    private CreditHistoryEnricher enricher;

    // Map BankInterestRequest to LoanRequest
    private Map<BankInterestRequest, LoanRequest> bankInterestRequestLoanRequestMap;

    // javafx objects
    public ListView<ListViewLine> lvBroker;

    public BrokerClientController() {

        // Initialize Archiver
        this.archiver = new Archiver();

        // Initialize content enricher
        this.enricher = new CreditHistoryEnricher();

        // Initialize map
        this.bankInterestRequestLoanRequestMap = new HashMap();

        // initialize gateways
        this.clientGateway = new ClientGateway() {
            public void onLoanRequestArrived(LoanRequest req) {
                try {
                    ListViewLine lvl = new ListViewLine(req);
                    addListViewLineToLv(lvl);
                    CreditHistory ch = enricher.getCreditHistoryForSSN(req.getSsn());
                    BankInterestRequest request = new BankInterestRequest(req.getAmount(), req.getTime(),
                            ch.getCredit(), ch.getHistory());
                    bankGateway.applyForLoan(request);
                    bankInterestRequestLoanRequestMap.put(request, req);
                } catch (JMSException e) {
                    e.printStackTrace();
                }
            }
        };

        this.bankGateway = new BankGateway() {

            public void onBankInterestRequestArrived(BankInterestRequest req, BankInterestReply rep) {
                try {
                    LoanRequest request = bankInterestRequestLoanRequestMap.get(req);
                    ListViewLine lvl = getLvlForLoanRequest(request);
                    LoanReply reply = new LoanReply(rep.getInterest(), rep.getQuoteId());
                    lvl.setLoanReply(reply);
                    addListViewLineToLv(lvl);
                    clientGateway.replyOnRequest(request, reply);
                    archiver.archive(new LoanArchive(request.getSsn(), request.getAmount(),
                            reply.getQuoteID(), reply.getInterest(), request.getTime()));
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
