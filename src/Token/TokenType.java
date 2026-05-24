package Token;

public enum TokenType {
    // Aritmética
    PLUS, MINUS, MULTIPLY, DIVISION, MOD, POW,

    // Comparación
    EQ, DIF, LT, LTE, GT, GTE,

    // Lógica
    AND, OR, NEGATION,

    // Asignación
    ASSIGN,

    // Separación
    COMMA, SEMICOLON, LPAREN, RPAREN, LBRACE, RBRACE,

    // Literales
    INTEGER, FLOAT, STRING, TRUE, FALSE,

    // Identificadores
    IDENTIFIER,

    // Palabras clave
    FUNCTION, LET, RETURN, IF, ELSEIF, ELSE,
    WHILE, FOR, BREAK, CONTINUE, PRINT,

    // Especiales
    EOF, ILLEGAL
}
