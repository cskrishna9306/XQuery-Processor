// Define the grammar name
grammar XPath;

xpath
    : absolutePath
    ;

// Parser rules
absolutePath
  : 'doc(' fileName ')' '/' relativePath
  | 'doc(' fileName ')' '//' relativePath
  ;

// relativePath: STRING ;
relativePath
  : tagName
  | '*'
  | '.'
  | '..'
  | 'text()'
  | '@' attributeName
  | '(' relativePath ')'
  | relativePath '/' relativePath
  | relativePath '//' relativePath
  | relativePath '[' filter ']'
  | relativePath ',' relativePath
  ;

filter: relativePath
  | relativePath '=' relativePath
  | relativePath 'eq' relativePath
  | relativePath '==' relativePath
  | relativePath 'is' relativePath
  | relativePath '=' '"' STRING '"'
  | '(' filter ')'
  | filter 'and' filter
  | filter 'or' filter
  | 'not' filter
  ;

// Lexer rules
tagName: STRING;
attributeName: STRING ;
fileName: '"' STRING '"' ;

// Define operators and other symbols as fragments
// TODO: add special chars?
STRING: (LETTER | DIGIT | '_' | '.')+;
DIGIT: [0-9] ;
LETTER: [a-zA-Z] ;

// Define whitespace and comments
WS: [ \t\r\n]+ -> skip ;
