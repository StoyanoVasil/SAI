package loanclient.messageGateway;

import com.google.gson.Gson;
import loanclient.gui.ListViewLine;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;


public class Producer {

    // set jms connection address
    private static final String JMS_CONNECTION = "tcp://localhost:61616";

    // create connection objects
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageProducer producer;

    // serialization object
    private Gson gson;

    public Producer(String queue) {

        this.gson = new Gson();

        try {
            // set properties
            Properties props = new Properties();
            props.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                    "org.apache.activemq.jndi.ActiveMQInitialContextFactory");
            props.setProperty(Context.PROVIDER_URL, JMS_CONNECTION);
            props.put(("queue." + queue), queue);

            // create connection and session
            Context jndiContext = new InitialContext(props);
            ConnectionFactory connectionFactory = (ConnectionFactory) jndiContext
                    .lookup("ConnectionFactory");
            this.connection = connectionFactory.createConnection();
            this.session = this.connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create brokerDestination and producer
            this.destination = (Destination) jndiContext.lookup(queue);
            this.producer = session.createProducer(this.destination);
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
        }
    }

    public void send(ListViewLine lvl) throws JMSException {

        Message msg = this.session.createTextMessage(this.gson.toJson(lvl));
        this.producer.send(msg);
    }
}
