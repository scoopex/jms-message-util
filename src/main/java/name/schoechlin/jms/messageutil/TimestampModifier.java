package name.schoechlin.jms.messageutil;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class TimestampModifier {

	private static Pattern timemodifierFormat = Pattern.compile("^(?<count>[+-]?\\d+)(?<proportion>[smhd])$");
	private static Pattern timemodifierExpressionFormat = Pattern.compile("^(?<type>element|attribute)#(?<name>.+)#(?<time>.+)$");

	private static ArrayList<TimestampModification> timeModifications = new ArrayList<TimestampModification>();
	
	public TimestampModifier(ArrayList<String> modifications) {
		for (String timeModification: modifications) {
			timeModifications.add(new TimestampModification(timeModification));
		}
	}

	static String shiftTime(String time, String timeshifts) {

		DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser().withOffsetParsed();
		DateTime dt = fmt.parseDateTime(time);
		
		for (String timeshift : timeshifts.split(";")) {

			Matcher matcher = timemodifierFormat.matcher(timeshift);
			if (!matcher.find()) {
				System.err.printf("WARNING: wrong time format '%s' - ignore it\n", timeshift);
				continue;
			}

			Integer count = Integer.parseInt(matcher.group("count"));

			switch (matcher.group("proportion")) {
				case "s":
					dt = dt.plusSeconds(count);
					break;
				case "m":
					dt = dt.plusMinutes(count);
					break;
				case "h":
					dt = dt.plusHours(count);
					break;
				case "d":
					dt = dt.plusDays(count);
					break;
			}
		}
		return dt.toString();
	}
	
	
	public Document modifyTime(TimestampModification mod,Document doc) {
		
		Node rootContainer = doc.getFirstChild();
		NodeList dataSets = rootContainer.getChildNodes();
		if (mod.type.equals("attribute")){
			for (int i = 0; i < dataSets.getLength(); i++) {
				Node dataSet = dataSets.item(i);
				if (dataSet.hasAttributes()) {
					Element e = (Element) dataSet;
					if (e.hasAttribute(mod.name)) {
						e.setAttribute(mod.name,
								shiftTime(e.getAttribute(mod.name), mod.timeModification));
					}
				}
			}
		}
		else if(mod.type.equals("element")){
			dataSets = doc.getElementsByTagName(mod.name);
			for (int i = 0; i < dataSets.getLength(); i++) {
				Node dataSet = dataSets.item(i);
				dataSet.setTextContent(shiftTime(dataSet.getTextContent(), mod.timeModification));
			}	
		}
		return doc;
	}

	
	public String process(String xml) throws Exception {

		String document;

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		InputSource is = new InputSource(new StringReader(xml));

		Document doc = docBuilder.parse(is);

		for(TimestampModification timeModification: timeModifications){
			doc = this.modifyTime(timeModification, doc);
		}
		
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		StringWriter writer = new StringWriter();
		transformer.transform(new DOMSource(doc), new StreamResult(writer));
		document = writer.getBuffer().toString();

		return document;
	}
	
	class TimestampModification {
		
		public String type;
		public String name;
		public String timeModification;
		
		public TimestampModification(String mod) {
		    Matcher m = timemodifierExpressionFormat.matcher(mod);
		    if (!m.matches()){
		    	throw new IllegalArgumentException("Modification is not in accpetable format: "+mod);
		    }
	    	this.type = m.group("type");
	    	this.timeModification = m.group("time");
	    	this.name = m.group("name");
		}
	}
}
