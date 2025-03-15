#!/bin/bash
#mvn clean compile
#javac -cp lib/antlr-4.13.1-complete.jar:target/main -d target/main src/main/XPathProcessor.java
#javac -cp lib/antlr-4.13.1-complete.jar:target/main -d target/main src/main/Main.java
#java -cp lib/antlr-4.13.1-complete.jar:target/main XPathProcessor "doc(\"j_caesar.xml\")//SCENE"

#OLDDDDD
java -cp lib/antlr-4.13.1-complete.jar org.antlr.v4.Tool -package main.antlr -o . main/antlr/XQuery.g4
#javac -cp lib/antlr-4.13.1-complete.jar ./main/XMLToDOMParser.java
#javac -cp .:lib/*:main main/XPathProcessor.java
##javac -cp lib/antlr-4.13.1-complete.jar:lib/*:main:main/antlr -d . main/*.java
#javac -cp .:lib/*:main:main/antlr main/Main.java
##java -cp .:lib/* main.Main
##ORIGINAL
#java -cp .:lib/*:lib main.Main ./main/data/j\_caesar.xml ./milestone1/q6.txt result.xml
##NEXT
##java -cp "classes:lib/*" main.Main data/j_caesar.xml data/query/q$i.txt data/result/q$i.xml


#autograde
javac -cp "lib/*" -d classes main/*.java main/antlr/*.java
java_files=$(find main -name "*.java")
javac -cp lib/* -d classes $java_files
#java -cp "classes:lib/*" main.Main ./main/data/j_caesar.xml ./milestone3/query1.txt ./main/data/result/queryRewrite1.txt query1.xml
#java -cp "classes:lib/*" main.Main ./main/data/j_caesar.xml ./milestone3/query1_re.txt ./main/data/result/queryRewrite1_re.txt query1_re.xml
#java -cp "classes:lib/*" main.Main ./main/data/large-data.xml ./milestone3/query2.txt ./main/data/result/queryRewrite2.txt query2.xml
#java -cp "classes:lib/*" main.Main ./main/data/large-data.xml ./milestone3/query2_j.txt ./main/data/result/queryRewrite2_j.txt query2_j.xml
#java -cp "classes:lib/*" main.Main ./main/data/large-data.xml ./milestone3/query2_re.txt ./main/data/result/queryRewrite2re.txt query2_re.xml
#java -cp "classes:lib/*" main.Main ./main/data/large-data.xml ./milestone3/rewrite3.txt ./main/data/result/rewrite3.txt rewrite3.xml
#java -cp "classes:lib/*" main.Main ./main/data/large-data.xml ./milestone3/query3.txt ./main/data/result/queryRewrite3.txt query3.xml
java -cp "classes:lib/*" main.Main ./main/data/large-data.xml ./milestone3/query4.txt ./main/data/result/queryRewrite4.txt query4.xml

#javac -cp lib/* ./main/Main.java
#java -cp lib/* main.Main ./main/j\_caesar.xml ./test.xml result.xml

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