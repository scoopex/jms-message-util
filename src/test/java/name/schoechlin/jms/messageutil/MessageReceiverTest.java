package name.schoechlin.jms.messageutil;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.activemq.broker.BrokerService;

public class MessageReceiverTest {
	BrokerService broker;

	@org.junit.Test
	public void testReceive() {

		String filesToCheck[] = new String[] { "target/receive/0000001000_EGALQUEUE.xml",
				"target/receive/0000001000_EGALQUEUE.xml", "target/receive/0000001000_EGALQUEUE.xml" };

		for (String fileToCheck : filesToCheck) {
			File f = new File(fileToCheck);
			if (f.exists()) {
				f.delete();
			}
		}

		String brokerAdress = "tcp://127.0.0.1:61616";
		MessageHandler msgHandler = createMockHandler(brokerAdress);
		MessageReceiver mr = new MessageReceiver(msgHandler, "target/receive/");
		mr.receive("EGALQUEUE", false, false);

		assertTrue("Message counter is not 4000", 4000 == mr.currentCount);

		for (String fileToCheck : filesToCheck) {
			File f = new File(fileToCheck);
			assertTrue("File '" + fileToCheck + "' does not exist", f.exists());
		}

	}

	private MessageHandler createMockHandler(String connectionDefinition) {
		return new MockMessageHandler(connectionDefinition, "foo", "bar", "baz");
	}
}
