package brokerclient.model;

public class CreditHistory {

    private int credit;
    private int history;

    public CreditHistory(int credit, int history) {

        this.credit = credit;
        this.history = history;
    }

    public int getCredit() { return this.credit; }

    public int getHistory() { return this.history; }

    @Override
    public String toString() {

        return "credit: " + this.credit + ", history: " + this.history;
    }
}
