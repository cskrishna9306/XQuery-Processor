grammar XQuery;

// Entry point of the parser
eval
    : xQuery
    ;

// Parser rules for XQuery
xQuery
    : VAR
    | STRING
    | absolutePath
    | '(' xQuery ')'
    | xQuery ',' xQuery
    | xQuery '/' relativePath
    | xQuery '//' relativePath
    | '<' TAGNAME '>' '{' xQuery '}' '</' TAGNAME '>'
    | forClause letClauseWithEmpty whereClause returnClause
    | letClause xQuery
    | 'for' VAR 'in' joinClause returnClause
    | joinClause
    ;

// Parser rules for the for clause
forClause
    : 'for' VAR 'in' xQuery (',' VAR 'in' xQuery)*
    ;

// Parser rules for let clause w/o the empty clause
letClause
    : 'let' VAR ':=' xQuery (',' VAR ':=' xQuery)*
    ;

// Parser rules for let clause including the empty clause
letClauseWithEmpty
    : 'let' VAR ':=' xQuery (',' VAR ':=' xQuery)*
    |
    ;

// Parser rules for where clause including the empty clause
whereClause
    : 'where' condition
    |
    ;

// Parser rules for return clause
returnClause
    : 'return' xQuery
    ;

// Parser rules for join clause
joinClause
    : 'join (' xQuery ',' xQuery ',' attributeList ',' attributeList ')'
    ;

// Parser rules for attribute list
attributeList
    : '[' TAGNAME (',' TAGNAME)* ']'
    ;

// Parser rules for condition
condition
    : xQuery '=' xQuery
    | xQuery 'eq' xQuery
    | xQuery '==' xQuery
    | xQuery 'is' xQuery
    | 'empty(' xQuery ')'
    | 'some' VAR 'in' xQuery (',' VAR 'in' xQuery)* 'satisfies' condition
    | '(' condition ')'
    | condition 'and' condition
    | condition 'or' condition
    | 'not' condition
    ;

// Parser rules for absolute path
absolutePath
  : ('document(' | 'doc(') STRING ')' '/' relativePath
  | ('document(' | 'doc(') STRING ')' '//' relativePath
  ;

// Parser rules for relative path
relativePath
  : TAGNAME
  | '*'
  | '.'
  | '..'
  | 'text()'
  | '@' ATTRIBUTENAME
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
  | relativePath '=' STRING
  | '(' filter ')'
  | filter 'and' filter
  | filter 'or' filter
  | 'not' filter
  ;

// Lexer rules
VAR: '$' LETTER (LETTER | DIGIT | '_')* ;
TAGNAME: LETTER (LETTER | DIGIT | '-')* ;
ATTRIBUTENAME: LETTER (LETTER | DIGIT | '_' | '-')* ;

// Define operators and other symbols as fragments
STRING: ('"' | '“') (~["\r\n])* ('"' | '”') ;
DIGIT: [0-9] ;
LETTER: [a-zA-Z] ;

// Define whitespace and comments
WS: [ \t\r\n]+ -> skip ;
