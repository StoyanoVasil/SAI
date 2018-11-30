package brokerclient.messageGateway;

import brokerclient.model.CreditHistory;
import org.glassfish.jersey.client.ClientConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.net.URI;

public class CreditHistoryEnricher {

    private URI baseUri;
    private WebTarget client;
    private JSONParser parser;

    public CreditHistoryEnricher() {
        this.baseUri = UriBuilder.fromUri("http://localhost:8080/CreditAgency/rest/history/").build();
        this.client = ClientBuilder.newClient(new ClientConfig()).target(this.baseUri);
        this.parser = new JSONParser();
    }

    public CreditHistory getCreditHistoryForSSN(int ssn) {

        try {
            Builder req = this.client.path(Integer.toString(ssn)).request(MediaType.APPLICATION_JSON);
            Response res = req.get();
            if (res.getStatus() == 200) {
                JSONObject json = (JSONObject) this.parser.parse(res.readEntity(String.class));
                return new CreditHistory(Math.toIntExact((Long) json.get("creditScore")),
                        Math.toIntExact((Long) json.get("history")));
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}
