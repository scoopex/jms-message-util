package name.schoechlin.jms.messageutil;

import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Objects;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnectionFactory;

public class MessageHandler {

	protected ActiveMQConnectionFactory connectionFactory;
	protected String connectionString = null;
	protected Connection connection;
	protected Session session;
	private String connectionDefinition;

	public MessageHandler(String connectionDefinition) {
		this.connectionDefinition = Objects.requireNonNull(connectionDefinition);
		System.out.println("DEFAULT Charset : " + Charset.defaultCharset());
	}

	private void initConnection(String connectionDefinition) {

		if (connectionFactory != null) {
			return;
		}

		this.connectionString = connectionDefinition;
		System.out.printf("Connection string : '%s'\n", connectionString);
		try {
			connectionFactory = new ActiveMQConnectionFactory(connectionString);

			connection = connectionFactory.createConnection();
			connection.start();

			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

		} catch (Exception e) {
			System.err.println("Caught: " + e);
			e.printStackTrace();
		}
	}

	public void cleanup() {
		try {
			session.close();
			connection.close();
		} catch (Exception e) {
			System.out.println("Caught Exception: " + e);
			e.printStackTrace();
		}
	}

	public Boolean sendMessage(String queueName, String text) throws Exception {
		initConnection(connectionDefinition);

		Boolean ret = true;

		if (text.isEmpty()) {
			throw new Exception("You are trying to send empty content");
		}
		try {
			Destination destination = session.createQueue(queueName);

			MessageProducer producer = session.createProducer(destination);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);

			TextMessage message = session.createTextMessage(text);

			producer.send(message);

		} catch (JMSException e) {
			System.err.println("JMS Problem - Exception occurred: " + e.toString());
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}

	public boolean processMessages(String queueName, IMessageProcessor processor, Boolean isConsumer) {
		initConnection(connectionDefinition);

		Boolean ret = true;
		Queue destination;
		QueueBrowser browser = null;

		try {
			destination = session.createQueue(queueName);

			int numMsgs = 0;
			if (!isConsumer) {
				browser = session.createBrowser(destination);

				@SuppressWarnings("unchecked")
				Enumeration<Message> msgs = browser.getEnumeration();
				while (msgs.hasMoreElements()) {
					Message message = msgs.nextElement();

					if (message instanceof TextMessage) {
						TextMessage txtMsg = (TextMessage) message;
						processor.process(txtMsg.getText());
					} else {
						System.err.printf("ERROR: Message '%s' is not a Textmessage\n", message.getJMSMessageID());
						ret = false;
					}
					numMsgs++;
				}

			} else {
				MessageConsumer consumer = session.createConsumer(destination);

				while (true) {

					Message message = consumer.receive(3000);

					if (message == null) {
						break;
					}

					if (message instanceof TextMessage) {
						TextMessage textMessage = (TextMessage) message;
						processor.process(textMessage.getText());
						numMsgs++;
					} else {
						System.err.println("ERROR: No text message received: " + message);
					}
				}

			}
			System.out.println("No of messages = " + numMsgs);

		} catch (JMSException e) {
			System.err.println("Exception occurred: " + e.toString());
			ret = false;
		} finally {
			if (browser != null) {
				try {
					browser.close();
				} catch (JMSException e) {
					// This is o.k. [TM]
				}
			}
			if (connection != null) {
				try {
					connection.close();
				} catch (JMSException e) {
					// This is o.k. [TM]
				}
			}
		}

		return ret;
	}

}
