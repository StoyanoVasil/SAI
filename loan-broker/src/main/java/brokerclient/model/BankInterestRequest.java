package brokerclient.model;

/**
 *
 * This class stores all information about an request from a bank to offer
 * a loan to a specific client.
 */
public class BankInterestRequest {

    private int amount; // the ammount to borrow
    private int time; // the time-span of the loan in years
    private int creditScore;
    private int history;

    public BankInterestRequest() {
        super();
        this.amount = 0;
        this.time = 0;
    }

    public BankInterestRequest(int amount, int time, int score, int history) {
        super();
        this.amount = amount;
        this.time = time;
        this.creditScore = score;
        this.history = history;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }


    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public String toString() {

        return "Amount=" + amount + ", time=" + time + ", credit score=" + this.creditScore + ", history=" + this.history;
    }
}
