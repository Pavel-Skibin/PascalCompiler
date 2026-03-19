grammar Pascal;

options {
    caseInsensitive = true;
}

program
    : PROGRAM identifier SEMI block DOT EOF
    ;

block
    : declarationPart compoundStatement
    ;

declarationPart
    : varSection? subroutineDeclaration*
    ;

varSection
    : VAR variableDeclaration+
    ;

variableDeclaration
    : identifierList COLON typeSpec SEMI
    ;

identifierList
    : identifier (COMMA identifier)*
    ;

subroutineDeclaration
    : procedureDeclaration
    | functionDeclaration
    ;

procedureDeclaration
    : PROCEDURE identifier formalParameters? SEMI block SEMI
    ;

functionDeclaration
    : FUNCTION identifier formalParameters? COLON typeSpec SEMI block SEMI
    ;

formalParameters
    : LPAREN formalParameterSection (SEMI formalParameterSection)* RPAREN
    ;

formalParameterSection
    : identifierList COLON typeSpec
    ;

typeSpec
    : primitiveType
    | arrayType
    | identifier
    ;

primitiveType
    : INTEGER_TYPE
    | CHAR_TYPE
    | BOOLEAN_TYPE
    | STRING_TYPE
    | DOUBLE_TYPE
    ;

arrayType
    : ARRAY LBRACK INTEGER_LITERAL DOTDOT INTEGER_LITERAL RBRACK OF typeSpec
    ;

compoundStatement
    : BEGIN statementSequence? END
    ;

statementSequence
    : statement (SEMI statement)* SEMI?
    ;

statement
    : compoundStatement
    | assignmentStatement
    | ifStatement
    | whileStatement
    | repeatStatement
    | forStatement
    | breakStatement
    | continueStatement
    | procedureCallStatement
    | ioStatement
    ;

assignmentStatement
    : variable ASSIGN expression
    ;

ifStatement
    : IF expression THEN statement (ELSE statement)?
    ;

whileStatement
    : WHILE expression DO statement
    ;

repeatStatement
    : REPEAT statementSequence UNTIL expression
    ;

forStatement
    : FOR identifier ASSIGN expression (TO | DOWNTO) expression DO statement
    ;

breakStatement
    : BREAK
    ;

continueStatement
    : CONTINUE
    ;

procedureCallStatement
    : identifier (LPAREN argumentList? RPAREN)?
    ;

ioStatement
    : WRITE (LPAREN argumentList? RPAREN)?
    | WRITELN (LPAREN argumentList? RPAREN)?
    | READ LPAREN variableList? RPAREN
    | READLN (LPAREN variableList? RPAREN)?
    ;

argumentList
    : expression (COMMA expression)*
    ;

variableList
    : variable (COMMA variable)*
    ;

expression
    : orExpression
    ;

orExpression
    : andExpression (OR andExpression)*
    ;

andExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQUAL | NOT_EQUAL) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LT | LTE | GT | GTE) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | SLASH | DIV | MOD) unaryExpression)*
    ;

unaryExpression
    : (NOT | PLUS | MINUS) unaryExpression
    | primary
    ;

primary
    : functionCall
    | variable
    | literal
    | LPAREN expression RPAREN
    ;

functionCall
    : identifier LPAREN argumentList? RPAREN
    ;

variable
    : identifier (LBRACK expression RBRACK)?
    ;

literal
    : INTEGER_LITERAL
    | REAL_LITERAL
    | STRING_LITERAL
    | TRUE
    | FALSE
    ;

identifier
    : IDENTIFIER
    ;

PROGRAM: 'program';
VAR: 'var';
ARRAY: 'array';
OF: 'of';
BEGIN: 'begin';
END: 'end';
PROCEDURE: 'procedure';
FUNCTION: 'function';
IF: 'if';
THEN: 'then';
ELSE: 'else';
FOR: 'for';
TO: 'to';
DOWNTO: 'downto';
DO: 'do';
WHILE: 'while';
REPEAT: 'repeat';
UNTIL: 'until';
BREAK: 'break';
CONTINUE: 'continue';
WRITE: 'write';
WRITELN: 'writeln';
READ: 'read';
READLN: 'readln';

INTEGER_TYPE: 'integer';
CHAR_TYPE: 'char';
BOOLEAN_TYPE: 'boolean';
STRING_TYPE: 'string';
DOUBLE_TYPE: 'double';

TRUE: 'true';
FALSE: 'false';

DIV: 'div';
MOD: 'mod';
NOT: 'not';
AND: 'and';
OR: 'or';

ASSIGN: ':=';
DOTDOT: '..';
SEMI: ';';
COLON: ':';
COMMA: ',';
DOT: '.';
LPAREN: '(';
RPAREN: ')';
LBRACK: '[';
RBRACK: ']';

EQUAL: '=';
NOT_EQUAL: '<>';
GTE: '>=';
LTE: '<=';
GT: '>';
LT: '<';

PLUS: '+';
MINUS: '-';
STAR: '*';
SLASH: '/';

REAL_LITERAL: DIGIT+ '.' DIGIT+;
INTEGER_LITERAL: DIGIT+;
IDENTIFIER: LETTER (LETTER | DIGIT | '_')*;

STRING_LITERAL
    : '\'' ( '\'\'' | ~['\r\n] )* '\''
    ;

COMMENT_BRACES: '{' .*? '}' -> skip;
COMMENT_LINE: '//' ~[\r\n]* -> skip;
WS: [ \t\r\n\f]+ -> skip;

fragment LETTER: [a-z];
fragment DIGIT: [0-9];
