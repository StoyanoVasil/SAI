package brokerclient.model;

public class LoanArchive {

    private int SSN;
    private int amount;
    private String bank;
    private Double interest;
    private int time;

    public LoanArchive(int ssn, int amount, String bank, Double interest, int time) {
        this.SSN = ssn;
        this.amount = amount;
        this.bank = bank;
        this.interest = interest;
        this.time = time;
    }

    public int getSSN() {
        return SSN;
    }

    public int getAmount() {
        return amount;
    }

    public String getBank() {
        return bank;
    }

    public Double getInterest() {
        return interest;
    }

    public int getTime() {
        return time;
    }
}
