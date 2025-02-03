import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.File;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

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

//            ANTLRInputStream input = new ANTLRInputStream("doc(\"j_caesar.xml\")/PLAY");
            CharStream input = CharStreams.fromString("doc(\"j_caesar.xml\")/PLAY");

            XPathLexer lexer = new XPathLexer(input);
//            CommonTokenStream tokens = new CommonTokenStream(lexer);
            XPathParser parser = new XPathParser(new CommonTokenStream(lexer));
            ParseTree tree = parser.xpath();
            System.out.println(tree.toStringTree(parser));
//            parser.removeErrorListeners();
//            ParseTree tree = parser.eval();

//            CustomizedVisitor customizedVisitor = new CustomizedVisitor();
//            LinkedList<Node> results = customizedVisitor.visit(tree);

//            WriteXml writer = new WriteXml();
//            writer.getPath("output.xml");
//            writer.setNodesToWrite(results);
//            writer.createSon();
//            System.out.println(results.size());

//            String fname = args[0];
//            ExpLexer lexer = new ExpLexer(new ANTLRFileStream(fname));
//            ExpParser parser = new ExpParser(new CommonTokenStream(lexer));
//            ParseTree tree = parser.eval();
//            System.out.println(compute(tree));

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
