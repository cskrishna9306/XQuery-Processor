# Quickstart
Run with `./script.sh`
or
```bash
 mvn clean compile
 javac -cp lib/antlr-4.13.1-complete.jar:target/main -d target/main src/main/XPathProcessor.java
 java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[.//SPEAKER[text() = \"CAESAR\"] is .//SPEAKER]"
 ```

**NOTE**: due to using CLI to input XPath queries, quotes within the query must be escaped with a backslash.