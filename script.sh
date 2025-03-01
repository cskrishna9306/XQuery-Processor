mvn clean compile

# mkdir -p "target/test/Milestone I"
mkdir -p "target/test/Milestone II"

for i in {1..10}; do
#  java -cp lib/antlr-4.13.1-complete.jar:target/main Main src/main/j\_caesar.xml "src/test/Milestone I/q$i.txt" "target/test/Milestone I/q$i.xml"
  java -cp lib/antlr-4.13.1-complete.jar:target/main Main src/main/j\_caesar.xml "src/test/Milestone II/q$i.txt" "target/test/Milestone II/q$i.xml"
done