import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

public class XMLToDOMParser {

    public static Document parse(String fileName) {
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Configure factory if needed (optional)
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            // Create a DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the XML file
            File xmlFile = new File(fileName);
            Document document = builder.parse(xmlFile);

            // Normalize the document (optional, but recommended)
            document.getDocumentElement().normalize();

            // Output root element for confirmation
            System.out.println("Completed XML to DOM parsing.");

            return document;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
