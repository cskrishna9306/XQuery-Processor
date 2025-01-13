import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.File;

public class XQueryProcessor {
    public static void XMLToDOMParser() {
        try {
            // Create a DocumentBuilderFactory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Configure factory if needed (optional)
            factory.setIgnoringComments(true);
            factory.setIgnoringElementContentWhitespace(true);

            // Create a DocumentBuilder
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parse the XML file
            File xmlFile = new File("j_caesar.xml");
            Document document = builder.parse(xmlFile);

            // Normalize the document (optional, but recommended)
            document.getDocumentElement().normalize();

            // Output root element for confirmation
            System.out.println("Root element: " + document.getDocumentElement().getNodeName());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void XQueryParser() {
        
    }

    public static void main(String[] args) {
        XMLToDOMParser();
    } 
}
