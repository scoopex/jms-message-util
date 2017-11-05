package name.schoechlin.jms.messageutil;

import java.io.File;
import java.io.PrintWriter;
import java.util.Objects;

public class MessageReceiver {

	public Integer currentCount = 1000;
	public Integer stepCount = 1000;

	private String dumpPath = null;
	private MessageHandler msgh;

	public MessageReceiver(MessageHandler msgHandler, String dumpPath) {

		this.msgh = Objects.requireNonNull(msgHandler);
		this.dumpPath = Objects.requireNonNull(dumpPath);

		/* Create the directory, if it does not exist */
		new File(dumpPath).mkdirs();

	}

	private String getFilename(String queueName, Boolean removeManual, String dumpPath, Integer currentCount) {
		if (removeManual) {
			queueName = queueName.replaceAll("-MANUAL", "");
		}

		String filename = String.format("%s" + File.separator + "%010d_%s.xml", dumpPath, currentCount, queueName);
		filename = filename.replaceAll(File.separator + "+", File.separator);

		return filename;
	}

	public Boolean receive(final String queueName, final Boolean removeManual, Boolean isConsumer) {

		System.out.printf("Dumping Messages of queue '%s'\n", queueName);

		IMessageProcessor processor = new IMessageProcessor() {

			@Override
			public void process(String message) {
				PrintWriter out = null;
				String filename = getFilename(queueName, removeManual, dumpPath, currentCount);

				System.out.printf("Dumping message file '%s'\n", filename);

				try {
					out = new PrintWriter(filename);
					out.print(message);
				} catch (Exception e) {
					System.err.println("Exception occurred: " + e.toString());
					e.printStackTrace();
				} finally {
					if (out != null) {
						out.close();
					}
				}
				currentCount = currentCount + stepCount;
			}

		};

		return msgh.processMessages(queueName, processor, isConsumer);
	}

}
