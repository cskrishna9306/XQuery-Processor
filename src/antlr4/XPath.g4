grammar XPath;

// Entry point of the parser
eval
    : absolutePath
    ;

// Parser rules for absolute path
absolutePath
  : 'doc(' fileName ')' '/' relativePath
  | 'doc(' fileName ')' '//' relativePath
  ;

// Parser rules for relative path
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

// Parser rules for filter
filter
  : relativePath
  | relativePath '=' relativePath
  | relativePath 'eq' relativePath
  | relativePath '==' relativePath
  | relativePath 'is' relativePath
  | relativePath '=' stringConstant
  | '(' filter ')'
  | filter 'and' filter
  | filter 'or' filter
  | 'not' filter
  ;

// Lexer rules
tagName: STRING;
attributeName: STRING ;
fileName: ('"' | '“') STRING ('"' | '”') ;
stringConstant:('"' | '“') STRING ('"' | '”') ;

// Define operators and other symbols as fragments
STRING: (LETTER | DIGIT | '_' | '.')+;
DIGIT: [0-9] ;
LETTER: [a-zA-Z] ;

// Define whitespace and comments
WS: [ \t\r\n]+ -> skip ;
