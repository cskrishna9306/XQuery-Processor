grammar Exp;

/* This will be the entry point of our parser. */
eval
    :    absolutePath
    ;

/* Addition and subtraction have the lowest precedence. */
absolutePath
    :    'doc(' fileName ')' '/' relativePath
    |   'doc(' fileName ')' '//' relativePath
    ;

/* Multiplication and division have a higher precedence. */
relativePath
    :    tagName
    ;

/* An expression atom is the smallest part of an expression: a number. Or 
   when we encounter parenthesis, we're making a recursive call back to the
   rule 'additionExp'. As you can see, an 'atomExp' has the highest precedence. */
tagName
    :    String
    ;

fileName
    :    '"' String '"'
    ;

/* A number: can be an integer value, or a decimal value */
Number
    :    ('0'..'9')+ ('.' ('0'..'9')+)?
    ;

/* A number: can be an integer value, or a decimal value */
String
    :    ('a'..'z')+
    ;


/* We're going to ignore all white space characters */
WS  
    :   (' ' | '\t' | '\r'| '\n') -> skip
    ;

