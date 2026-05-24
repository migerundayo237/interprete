package Token;

import java.util.HashMap;
import java.util.Map;

public class Token {
    public final TokenType tokenType;
    public final String literal;

    public Token(TokenType tokenType, String literal) {
        this.tokenType = tokenType;
        this.literal = literal;
    }

    @Override
    public String toString() {
        return String.format("Type: %-12s  Literal: \"%s\"", tokenType.name(), literal);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Token)) return false;
        Token other = (Token) obj;
        return tokenType == other.tokenType && literal.equals(other.literal);
    }

    // ── Tabla de palabras clave ─────────────────────────────────────────
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("function", TokenType.FUNCTION);
        KEYWORDS.put("let",      TokenType.LET);
        KEYWORDS.put("return",   TokenType.RETURN);
        KEYWORDS.put("if",       TokenType.IF);
        KEYWORDS.put("elseif",   TokenType.ELSEIF);
        KEYWORDS.put("else",     TokenType.ELSE);
        KEYWORDS.put("while",    TokenType.WHILE);
        KEYWORDS.put("for",      TokenType.FOR);
        KEYWORDS.put("break",    TokenType.BREAK);
        KEYWORDS.put("continue", TokenType.CONTINUE);
        KEYWORDS.put("true",     TokenType.TRUE);
        KEYWORDS.put("false",    TokenType.FALSE);
        KEYWORDS.put("and",      TokenType.AND);
        KEYWORDS.put("or",       TokenType.OR);
        KEYWORDS.put("print",    TokenType.PRINT);
    }

    public static TokenType lookupTokenType(String literal) {
        return KEYWORDS.getOrDefault(literal, TokenType.IDENTIFIER);
    }
}

