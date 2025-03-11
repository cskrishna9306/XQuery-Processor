mvn clean compile

# mkdir -p "target/test/Milestone I"
# mkdir -p "target/test/Milestone II"
mkdir -p "target/test/Milestone III"

for i in {1..4}; do
#  java -cp lib/antlr-4.13.1-complete.jar:target/main Main src/main/j\_caesar.xml "src/test/Milestone I/q$i.txt" "target/test/Milestone I/rewrite$i.txt" "target/test/Milestone I/q$i.xml"
#  java -cp lib/antlr-4.13.1-complete.jar:target/main Main src/main/j\_caesar.xml "src/test/Milestone II/q$i.txt" "target/test/Milestone II/rewrite$i.txt" "target/test/Milestone II/q$i.xml"
  java -cp lib/antlr-4.13.1-complete.jar:target/main Main src/main/j\_caesar.xml "src/test/Milestone III/query$i.txt" "target/test/Milestone III/rewrite$i.txt" "target/test/Milestone III/query$i.xml"
done