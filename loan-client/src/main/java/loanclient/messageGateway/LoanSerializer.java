package loanclient.messageGateway;

import com.google.gson.Gson;
import loanclient.model.LoanReply;
import loanclient.model.LoanRequest;

public class LoanSerializer {

    private Gson gson;

    public LoanSerializer() {
        this.gson = new Gson();
    }

    public String serializeLoanRequest(LoanRequest req) {
        return this.gson.toJson(req);
    }

    public String serializeLoanReply(LoanReply rep) {
        return this.gson.toJson(rep);
    }

    public LoanRequest deserializeLoanRequest(String obj) {
        return this.gson.fromJson(obj, LoanRequest.class);
    }

    public LoanReply deserializeLoanReply(String obj) {
        return this.gson.fromJson(obj, LoanReply.class);
    }
}
