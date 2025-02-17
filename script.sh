mvn clean compile
javac -cp lib/antlr-4.13.1-complete.jar:target/main -d target/main src/main/Main.java
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SCENE"
java -cp lib/antlr-4.13.1-complete.jar:target/main Main src/main/j\_caesar.xml q1.txt result.xml

# Find CAESAR eq speaker
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[.//SPEAKER = .//SPEAKER[text() = \"CAESAR\"]]"
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[.//SPEAKER eq .//SPEAKER[text() = \"CAESAR\"]]"

# SPEAKER not eq CAESAR
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[not .//SPEAKER = .//SPEAKER[text() = \"CAESAR\"]]"

# SPEAKER that contains CAESAR
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[.//SPEAKER[text() = \"CAESAR\"] = .//SPEAKER]"
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[.//SPEAKER[text() = \"CAESAR\"] is .//SPEAKER]"

# SPEAKER is not caesar is text with caesar -> NONE
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[.//SPEAKER[text() = \"CAESAR\"] is .//SPEAKER[not text() = \"CAESAR\"]]"

# SPEAKER THAT IS NOt CAESAR
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SPEECH[not .//SPEAKER[text() = \"CAESAR\"] is .//SPEAKER[not text() = \"CAESAR\"]]"