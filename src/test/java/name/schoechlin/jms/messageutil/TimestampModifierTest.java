package name.schoechlin.jms.messageutil;

import static java.nio.file.Files.readAllBytes;
import static java.nio.file.Paths.get;

import java.security.MessageDigest;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TimestampModifierTest extends TestCase {

	public TimestampModifierTest(String testName) {
		super(testName);
	}

	public static Test suite() {
		return new TestSuite(TimestampModifierTest.class);
	}
	
	public void testShiftTime() {

		String date = "2012-05-15T07:08:09+03:00";
		String newTime = TimestampModifier.shiftTime(date, "2d;2m;-3d");
		assertEquals("2012-05-14T07:10:09.000+03:00", newTime);

		newTime = TimestampModifier.shiftTime("2002-10-10T12:01:32+02:00", "-3d");
		assertEquals("2002-10-07T12:01:32.000+02:00", newTime);
	}

	private static String bytesToHex(byte[] b) {
		char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
		StringBuilder buf = new StringBuilder();
		for (byte aB : b) {
			buf.append(hexDigit[(aB >> 4) & 0x0f]);
			buf.append(hexDigit[aB & 0x0f]);
		}
		return buf.toString();
	}

	public void testChangeTimestamps() {
		byte[] digest = null;
		try {
			ArrayList<String> modifications = new ArrayList<String>();
			modifications.add("attribute#LastModifiedDateTime#-2d;+2m");
			modifications.add("element#ns0:CreatedDateTime#-2d;+2m");
			modifications.add("element#ns0:ModifiedDateTime#-2d;+2m");

			TimestampModifier tsm = new TimestampModifier(modifications);
			
			String xml = new String(readAllBytes(get("src/test/resources/xml/good/0000001000_FOO.BAR_BAZ-MANUAL.IN.xml")));
			String message = tsm.process(xml);

			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(message.getBytes("UTF-8"));
			digest = md.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// System.out.println("===================>"+bytesToHex(digest));
		// System.out.println(message);
		assertEquals("E89B3D329057251370588565B83714F1B6DCD78D285DC2E96F945EDFC394940C", bytesToHex(digest));
	}
}
