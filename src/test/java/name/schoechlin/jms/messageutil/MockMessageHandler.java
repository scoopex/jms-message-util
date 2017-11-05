package name.schoechlin.jms.messageutil;

public class MockMessageHandler extends MessageHandler {

	String[] messages = null;

	public MockMessageHandler(String connectionDefinition, String... messages) {
		super(connectionDefinition);
		this.messages = messages;
	}

	public MockMessageHandler(String connectionDefinition) {
		super(connectionDefinition);
	}

	@Override
	public boolean processMessages(String queueName, IMessageProcessor processor, Boolean isConsumer) {
		if (messages != null) {
			for (String message : messages) {
				processor.process(message);
			}
		}
		return true;
	}

	public Boolean sendMessage(String queueName, String text) {
		return true;
	}

}
