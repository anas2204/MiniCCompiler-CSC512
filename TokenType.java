/*
IDENTIFIER -->  <letter> (<letter> | <digit>)* (<letter> --> a | b | ... | y | z | A | B | ... | Z | underscore)
NUMBER -->  <digit>+
RESERVED --> int  | void | if | while | return | read | write | print | continue | break | binary | decimal
STRING --> any string between (and including) the closest pair of quotation marks.
RELOP --> <,>,==,<=,>=,!=
LOGOP --> &&,||,!
MATHOP --> +,-,*,/
SYMBOL --> (,),{,},[,],',',;,=
META --> any string begins with '#' or '//' and ends with the end of line ('\n')
*/

public enum TokenType
{
    IDENTIFIER,
    NUMBER,
    RESERVED,
    STRING,
    SYMBOL,
    RELOP,
    LOGOP,
    MATHOP,
    META,
    UNDEFINED
}