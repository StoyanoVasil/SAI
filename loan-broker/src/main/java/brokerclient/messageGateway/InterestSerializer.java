package brokerclient.messageGateway;

import brokerclient.model.BankInterestReply;
import brokerclient.model.BankInterestRequest;
import com.google.gson.Gson;

public class InterestSerializer {

    private Gson gson;

    public InterestSerializer() {
        this.gson = new Gson();
    }

    public String serializeBankInterestRequest(BankInterestRequest req) { return this.gson.toJson(req); }

    public String serializeBankInterestReply(BankInterestReply rep) { return this.gson.toJson(rep); }

    public BankInterestRequest deserializeBankInterestRequest(String json) {
        return this.gson.fromJson(json, BankInterestRequest.class);
    }

    public BankInterestReply deserializeBankInterestReply(String json) {
        return this.gson.fromJson(json, BankInterestReply.class);
    }
}
