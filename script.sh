mvn clean compile
javac -cp lib/antlr-4.13.1-complete.jar:target/main -d target/main src/main/XPathProcessor.java
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//ACT/../ACT/SCENE/TITLE"
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//ACT/SCENE/TITLE[text() = "Dramatis Personae"]"
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SCENE/SPEECH/SPEAKER"
java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SCENE/SPEECH/SPEAKER[text()="CAESAR"]"