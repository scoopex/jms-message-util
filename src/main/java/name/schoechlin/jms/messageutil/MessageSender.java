package name.schoechlin.jms.messageutil;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class MessageSender {

	private String messageFilesPath = null;
	private Boolean shuffleMessages = false;
	private static String filenamePattern = "\\d{10}_(?<queuename>.+)\\.xml";
	private static Pattern filenamePatternCompiled = Pattern.compile(filenamePattern);
	private TimestampModifier timestampModifier = null;
	private MessageHandler msgh;
	public int numberOfMessageSent = 0;
	private String fixedQueueName = null;

	public MessageSender(MessageHandler msgh, String messageFilesPath, Boolean shuffleMessages,
			ArrayList<String> timeModifications) {
		this.msgh = msgh;
		this.messageFilesPath = messageFilesPath;
		this.shuffleMessages = shuffleMessages;
		this.timestampModifier = new TimestampModifier(timeModifications);
		System.out.printf("Sending files from directory '%s'\n", messageFilesPath);
	}

	public static Boolean setFilenamePattern(String pattern) {
		filenamePattern = pattern;
		filenamePatternCompiled = Pattern.compile(filenamePattern);
		return true;
	}

	public static String getFilenamePattern() {
		return filenamePattern;
	}

	public Boolean sendMessages() {
		Boolean ret = true;
		File dir = new File(messageFilesPath);
		File[] directoryListing = dir.listFiles();
		String currentQueuename;

		if (directoryListing != null) {
			if (shuffleMessages) {
				Collections.shuffle(Arrays.asList(directoryListing));
			} else {
				Collections.sort(Arrays.asList(directoryListing));
			}

			for (File file : directoryListing) {
				Matcher matcher = filenamePatternCompiled.matcher(file.getName());
				if (!matcher.find()) {
					continue;
				}

				if (fixedQueueName != null) {
					currentQueuename = fixedQueueName;
				} else {
					currentQueuename = matcher.group("queuename");
				}

				Boolean quiet = false;
				if ((numberOfMessageSent > 2000) && (numberOfMessageSent % 1000 != 0)) {
					quiet = true;
				}

				if (!readAndSentFile(file.getAbsoluteFile(), currentQueuename, quiet)) {
					ret = false;
					continue;
				}
				numberOfMessageSent++;
			}
		} else {
			System.err.printf("ERROR - directory '%s' changed recently to a file\n", messageFilesPath);
			ret = false;
		}
		System.out.printf("INFO: sent %s messages\n", numberOfMessageSent);

		return ret;
	}

	private Boolean readAndSentFile(File file, String queueName, Boolean quiet) {

		Boolean ret = true;
		String text;

		try {
			String filename = file.getAbsoluteFile().toString();

			if (!quiet) {
				System.out.printf("Replaying content of file '%s' to queue '%s'\n", filename, queueName);
			}
			text = readFile(filename);

			text = this.timestampModifier.process(text);
			
			if (!msgh.sendMessage(queueName, text)) {
				ret = false;
			}
		} catch (Exception e) {
			System.err.println("Exception occurred: " + e.toString());
			e.printStackTrace();
			ret = false;
		}
		return ret;
	}

	private String readFile(String filename) throws IOException {

		String ret;

		if (filename.toLowerCase().endsWith(".gz")) {
			try (FileInputStream fin = new FileInputStream(filename);
					GZIPInputStream gzis = new GZIPInputStream(fin);
					InputStreamReader xover = new InputStreamReader(gzis);
					BufferedReader is = new BufferedReader(xover)) {

				// Now read lines of text: the BufferedReader puts them in
				// lines,
				// the InputStreamReader does Unicode conversion, and the
				// GZipInputStream "gunzip"s the data from the FileInputStream.

				StringWriter sw = new StringWriter();
				char[] buffer = new char[1024 * 32];
				int n;
				while (-1 != (n = is.read(buffer))) {
					sw.write(buffer, 0, n);
				}
				ret = sw.toString();

			}
		} else {
			ret = new String(readAllBytes(get(filename)));
		}

		return ret;
	}

	public void setFixedQueueName(String queueName) {
		this.fixedQueueName = queueName;
	}
}
