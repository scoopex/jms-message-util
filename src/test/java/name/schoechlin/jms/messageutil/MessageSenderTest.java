package name.schoechlin.jms.messageutil;

import static org.junit.Assert.*;

import java.util.ArrayList;

public class MessageSenderTest {

	@org.junit.Test
	public void testSend() {

		String brokerAdress = "tcp://127.0.0.1:61616";
		MessageHandler msgHandler = createMockHandler(brokerAdress);
		Boolean shuffleMessages = false;
		ArrayList<String> timeModification = new ArrayList<String>();

		MessageSender ms = new MessageSender(msgHandler, "src/test/resources/xml/good/", shuffleMessages,
				timeModification);
		ms.sendMessages();
		ms.setFixedQueueName("LALAL");
		ms.sendMessages();
		assertTrue("Number of sent messages is not 6", ms.numberOfMessageSent == 6);
	}

	@org.junit.Test
	public void testSendBroken() {

		String brokerAdress = "tcp://127.0.0.1:61616";
		MessageHandler msgHandler = createMockHandler(brokerAdress);
		Boolean shuffleMessages = false;
		ArrayList<String> timeModification = new ArrayList<String>();

		MessageSender ms = new MessageSender(msgHandler, "src/test/resources/xml/bad/", shuffleMessages,
				timeModification);
		assertFalse("Sending not failed", ms.sendMessages());
		assertTrue("Number of sent messages is not 0", ms.numberOfMessageSent == 0);
	}

	private MessageHandler createMockHandler(String connectionDefinition) {
		return new MockMessageHandler(connectionDefinition);
	}
}
