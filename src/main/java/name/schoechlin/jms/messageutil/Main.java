package name.schoechlin.jms.messageutil;

import gnu.getopt.Getopt;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class Main {

	public static String getJar() {
		return new java.io.File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
	}

	public static void usage() {
		String format = "%-25s %s\n";
		System.out.printf("\nUsage: java -jar %s <options> <dir>\n\n", getJar());
		System.out.println("General options:");
		System.out.printf(format, "-u", "Broker url");
		System.out.println("\n\nDumping options:");
		System.out.printf(format, "-d <queue>", "Dump Messages from Queue");
		System.out.printf(format, "-b <inscrement>", "Start increment counter at this number");
		System.out.printf(format, "-i <inscrement>", "Increment by this number");
		System.out.printf(format, "-m", "Remove 'MANUAL' string in queuename while saving files");
		System.out.printf(format, "-c", "Consume messages instead of browsing  the messages");
		System.out.println("\n\nReplay options:");
		System.out.printf(format, "-r", "Replay messages");
		System.out.printf(format, "-q <queue>", "Use a fixed queue for replay");
		System.out.printf(format, "", "(ignore queuename included in the filenames)");
		System.out.printf(format, "-p", "File pattern");
		System.out.printf(format, "", "Default: '" + MessageSender.getFilenamePattern() + "'");
		System.out.printf(format, "-s", "Shuffle messages on sending");
		System.out.printf(format, "-t [a|e]#<name>#<time mofification>",
				"Modify all time information of the specified xml element or attribute before sending, separate them by ';'");
		System.out.printf(format, "", "(s=seconds,m=minutes,h=hours,d=days)");
		System.out.printf(format, "", "Example: 'e#ns0:LastModifiedDateTime#-2d;-2m' 't15m;+5s");

	}

	public static void main(String[] argv) {

		int c;
		Boolean dump = false;
		Boolean replay = false;
		Boolean removeManual = false;
		Boolean shuffleMessages = false;
		Boolean isConsumer = false;
		String brokerAdress = "tcp://127.0.0.1:61616";
		String queueName = null;
		Integer stepCount = null;
		Integer incrementBegin = null;
		ArrayList<String> timeModifications = new ArrayList<String>();

		/* nasty hack to guarantee the file encoding */
		try {
			System.setProperty("file.encoding", "UTF-8");
			Field charset = Charset.class.getDeclaredField("defaultCharset");
			charset.setAccessible(true);
			charset.set(null, null);
		} catch (Exception e) {
			System.err.println("ERROR: unable to set locale :" + e.getMessage());
			System.exit(1);
		}

		String fileNamePattern = MessageSender.getFilenamePattern();

		Getopt g = new Getopt("testprog", argv, "hd:rd:msi:b:u:t:p:q:c");
		g.setOpterr(true);

		while ((c = g.getopt()) != -1)
			switch (c) {
			case 'h':
				usage();
				System.exit(1);
			case 's':
				shuffleMessages = true;
				break;
			case 'c':
				isConsumer = true;
				break;
			case 'm':
				removeManual = true;
				break;
			case 'u':
				brokerAdress = g.getOptarg();
				break;
			case 'p':
				fileNamePattern = g.getOptarg();
				break;
			case 't':
				timeModifications.add(g.getOptarg());
				break;
			case 'd':
				queueName = g.getOptarg();
				dump = true;
				break;
			case 'q':
				queueName = g.getOptarg();
				System.out.printf("INFO: using the alternate Queue '%s'\n", queueName);
				break;
			case 'i':
				stepCount = Integer.parseInt(g.getOptarg());
				break;
			case 'b':
				incrementBegin = Integer.parseInt(g.getOptarg());
				break;
			case 'r':
				replay = true;
				break;
			default:
				usage();
				break;
			}

		/* Missing mandatory parameter */
		if (g.getOptind() == 0 || g.getOptind() == argv.length) {
			usage();
			System.exit(1);
		}

		MessageHandler msgHandler = new MessageHandler(brokerAdress);

		if (dump && replay) {
			System.err.println("ERROR: you cannot dump and replay in one step");
			System.exit(1);
		}

		if (dump) {
			MessageReceiver mr = null;
			try {
				mr = new MessageReceiver(msgHandler, argv[g.getOptind()]);

				if (incrementBegin != null) {
					mr.currentCount = incrementBegin;
				}
				if (stepCount != null) {
					mr.stepCount = stepCount;
				}

				mr.receive(queueName, removeManual, isConsumer);
			} finally {
				if (mr != null) {
					msgHandler.cleanup();
				}
			}
		}
		if (replay) {
			MessageSender ms = null;
			try {
				MessageSender.setFilenamePattern(fileNamePattern);
				ms = new MessageSender(msgHandler, argv[g.getOptind()], shuffleMessages, timeModifications);
				if (queueName != null) {
					ms.setFixedQueueName(queueName);
				}
				ms.sendMessages();
			} finally {
				if (ms != null) {
					msgHandler.cleanup();
				}
			}
		}
	}

}
