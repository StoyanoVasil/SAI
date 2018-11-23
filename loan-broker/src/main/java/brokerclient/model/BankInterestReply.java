package brokerclient.model;

/**
 * This class stores information about the bank reply
 *  to a loan request of the specific client
 * 
 */
public class BankInterestReply {

    private double interest; // the interest that the bank offers for the requested loan
    private String quoteID; // the unique quote identification of the bank which makes the offer
    
    public BankInterestReply() {
        this.interest = 0;
        this.quoteID = "";
    }
    
    public BankInterestReply(double interest, String quoteID) {
        super();
        this.interest = interest;
        this.quoteID = quoteID;
    }

    public double getInterest() {
        return interest;
    }

    public void setInterest(double interest) {
        this.interest = interest;
    }

    public String getQuoteId() {
        return quoteID;
    }

    public void setQuoteId(String quoteId) {
        this.quoteID = quoteId;
    }

    public String toString() {
        return "quote=" + this.quoteID + " interest=" + this.interest;
    }
}
