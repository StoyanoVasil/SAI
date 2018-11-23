package bank.model;

/**
 *
 * This class stores all information about a
 * request that a client submits to get a loan.
 *
 */
public class BankInterestRequest {

    private int amount; // the ammount to borrow
    private int time; // the time-span of the loan in years

    public BankInterestRequest() {
        this.amount = 0;
        this.time = 0;
    }

    public BankInterestRequest(int amount, int time) {
        this.amount = amount;
        this.time = time;
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
        return "amount=" + String.valueOf(amount) + " time=" + String.valueOf(time);
    }

    @Override
    public boolean equals(Object o) {

        if(o instanceof BankInterestRequest) {
            BankInterestRequest req = (BankInterestRequest) o;
            if(req.getAmount() == this.getAmount() &&
                    req.getTime() == this.getTime()) {
                return true;
            }
        }
        return false;
    }
}
