package brokerclient.messageGateway;

import brokerclient.model.LoanArchive;
import com.google.gson.Gson;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

public class Archiver {

    private URI baseUri;
    private WebTarget client;
    private Gson serilizer;

    public Archiver() {
        this.baseUri = UriBuilder.fromUri("http://localhost:8080/Archive/rest/accepted/").build();
        this.client = ClientBuilder.newClient(new ClientConfig()).target(this.baseUri);
        this.serilizer = new Gson();
    }

    public void archive(LoanArchive archive) {
        if(archive.getInterest() >= 0) {
            this.client.request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(this.serilizer.toJson(archive), MediaType.APPLICATION_JSON));
        }
    }
}
