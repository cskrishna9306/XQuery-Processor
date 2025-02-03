# with output folder
java -cp antlr-4.13.1-complete.jar org.antlr.v4.Tool -o ./output XPath.g4
javac -cp .:antlr-4.13.1-complete.jar ./output/*.java
java -cp .:antlr-4.13.1-complete.jar:output XQueryProcessor


# without output folder
#java -cp antlr-4.13.1-complete.jar org.antlr.v4.Tool XPath.g4
#javac -cp .:antlr-4.13.1-complete.jar *.java
#java -cp .:antlr-4.13.1-complete.jar XQueryProcessor