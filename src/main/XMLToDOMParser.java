import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.util.List;

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

    public static void exportToXML(Document resultDoc, List<Node> result, String fileName) {
        try {
            // Create a new Document
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document tempDoc = builder.newDocument();

            // Create a root element to wrap nodes (optional)
//            Element root = resultDoc.createElement("RESULT");
//            resultDoc.appendChild(root);
//
//            // Import nodes into the new document and append them
//            for (Node node : result) {
//                Node importedNode = resultDoc.importNode(node, true);
//                root.appendChild(importedNode);
//            }

            // Serialize the Document to a file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes"); // Pretty print

            File outputFile = new File(fileName);
            transformer.transform(new DOMSource(resultDoc), new StreamResult(outputFile));

            System.out.println("XML saved to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            throw new RuntimeException("Error serializing XML to file", e);
        }
    }
}
