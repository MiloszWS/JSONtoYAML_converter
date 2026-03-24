grammar JSON;

// --- REGUŁY SKŁADNIOWE (Parsowanie) ---
// Punkt wejścia do naszego parsera
json : value ;

obj : '{' pair (',' pair)* '}'
    | '{' '}'
    ;

pair : STRING ':' value ;

arr : '[' value (',' value)* ']'
    | '[' ']'
    ;

value : STRING
      | NUMBER
      | obj
      | arr
      | 'true'
      | 'false'
      | 'null'
      ;

// --- REGUŁY LEKSYKALNE (Tokenizacja) ---
// STRING uwzględnia znaki ucieczki (escape characters)
STRING : '"' (ESC | ~["\\])* '"' ;

fragment ESC : '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

// NUMBER uwzględnia liczby całkowite, zmiennoprzecinkowe i notację naukową
NUMBER : '-'? INT ('.' [0-9]+)? EXP? ;

fragment INT : '0' | [1-9] [0-9]* ;
fragment EXP : [Ee] [+\-]? [0-9]+ ;

// Pomiń białe znaki (spacje, taby, entery)
WS : [ \t\n\r]+ -> skip ;