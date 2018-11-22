package bank.messageGateway;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Properties;

public class Consumer {

    // set jms connection address
    private static final String JMS_CONNECTION = "tcp://localhost:61616";

    // client connection objects
    private Connection connection;
    private Session session;
    private Destination destination;
    private MessageConsumer consumer;

    public Consumer(String queue) {

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
            this.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // create clientDestination and consumer
            this.destination = (Destination) jndiContext.lookup(queue);
            this.consumer = session.createConsumer(this.destination);

            // start connection
            this.connection.start();
        } catch (JMSException | NamingException e) {
            e.printStackTrace();
        }
    }

    public void setMessageListener(MessageListener listener) {
        try {
            this.consumer.setMessageListener(listener);
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
