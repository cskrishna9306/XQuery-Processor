grammar XQuery;

// Entry point of the parser
eval
    : xQuery
    ;


//xQuery
//    : primaryXQ xqSuffix*
//    ;
//
//primaryXQ
//    : test
//    ;
//
//xqSuffix
//    : '/' relativePath                         # XQRelativePath
//    | '//' relativePath                         # XQDescendantPath
//    | ',' xQuery                         # CommaExpr
//    ;
//
//test
//    : var                             # Variable
//    | STRING                          # StringConstant
//    | absolutePath                              # AbsolutePathExpr
//    | '(' primaryXQ ')'                      # GroupedXQ
//    | '<' tagName '>' primaryXQ '</' tagName '>' # ElementConstructor
//    | forClause letClause whereClause returnClause # FLWRExpr
//    | letClause primaryXQ                    # LetXQ
//    ;

// Parser rules for XQuery
xQuery
    : var
    | ('"' | '“') STRING ('"' | '”')
    | absolutePath
    | '(' xQuery ')'
    | xQuery ',' xQuery
    | xQuery '/' relativePath
    | xQuery '//' relativePath
    | '<' tagName '>' '{' xQuery '}' '</' tagName '>'
    | forClause letClause whereClause returnClause
    | letClause xQuery
    ;

// Parser rules for the for clause
forClause
    : 'for' var 'in' xQuery (',' var 'in' xQuery)*
    ;

// Parser rules for let clause including the empty clause
letClause
    : 'let' var ':=' xQuery (',' var ':=' xQuery)*
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

// Parser rules for condition
condition
    : xQuery '=' xQuery
    | xQuery 'eq' xQuery
    | xQuery '==' xQuery
    | xQuery 'is' xQuery
    | 'empty(' xQuery ')'
    | 'some' var 'in' xQuery (',' var 'in' xQuery)* 'satisfies' condition
    | '(' condition ')'
    | condition 'and' condition
    | condition 'or' condition
    | 'not' condition
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
  | relativePath '=' ('"' | '“') STRING ('"' | '”')
  | '(' filter ')'
  | filter 'and' filter
  | filter 'or' filter
  | 'not' filter
  ;

// Lexer rules
var: '$' STRING;
tagName: STRING ;
attributeName: STRING ;
fileName: ('"' | '“') STRING ('"' | '”') ;

// Define operators and other symbols as fragments
STRING: (LETTER | DIGIT | '_' | '.')+;
DIGIT: [0-9] ;
LETTER: [a-zA-Z] ;

// Define whitespace and comments
WS: [ \t\r\n]+ -> skip ;
