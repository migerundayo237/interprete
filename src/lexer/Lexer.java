package lexer;

import Token.*;

public class Lexer {

    private final String source;
    private char character;
    private int position;
    private int readPosition;

    public Lexer(String source) {
        this.source = source;
        this.character = 0;
        this.position = 0;
        this.readPosition = 0;
        readCharacter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Método principal
    // ─────────────────────────────────────────────────────────────────────────

    public Token nextToken() {
        skipWhitespaceAndComments();

        // EOF
        if (character == 0) {
            readCharacter();
            return new Token(TokenType.EOF, "");
        }

        // ── Aritmética ───────────────────────────────────────────────────────
        if (character == '+') { readCharacter(); return new Token(TokenType.PLUS,     "+"); }
        if (character == '-') { readCharacter(); return new Token(TokenType.MINUS,    "-"); }
        if (character == '*') { readCharacter(); return new Token(TokenType.MULTIPLY, "*"); }
        if (character == '/') { readCharacter(); return new Token(TokenType.DIVISION, "/"); }
        if (character == '%') { readCharacter(); return new Token(TokenType.MOD,      "%"); }
        if (character == '^') { readCharacter(); return new Token(TokenType.POW,      "^"); }

        // ── Operadores con uno o dos caracteres ──────────────────────────────
        if (character == '=') {
            if (peekCharacter() == '=') return makeTwoCharToken(TokenType.EQ);
            readCharacter(); return new Token(TokenType.ASSIGN, "=");
        }
        if (character == '!') {
            if (peekCharacter() == '=') return makeTwoCharToken(TokenType.DIF);
            readCharacter(); return new Token(TokenType.NEGATION, "!");
        }
        if (character == '<') {
            if (peekCharacter() == '=') return makeTwoCharToken(TokenType.LTE);
            readCharacter(); return new Token(TokenType.LT, "<");
        }
        if (character == '>') {
            if (peekCharacter() == '=') return makeTwoCharToken(TokenType.GTE);
            readCharacter(); return new Token(TokenType.GT, ">");
        }

        // ── Separación ───────────────────────────────────────────────────────
        if (character == ',') { readCharacter(); return new Token(TokenType.COMMA,     ","); }
        if (character == ';') { readCharacter(); return new Token(TokenType.SEMICOLON, ";"); }
        if (character == '(') { readCharacter(); return new Token(TokenType.LPAREN,    "("); }
        if (character == ')') { readCharacter(); return new Token(TokenType.RPAREN,    ")"); }
        if (character == '{') { readCharacter(); return new Token(TokenType.LBRACE,    "{"); }
        if (character == '}') { readCharacter(); return new Token(TokenType.RBRACE,    "}"); }

        // ── Literal de un string ─────────────────────────────────────────────
        if (character == '"') {
            String content = readString();
            return new Token(TokenType.STRING, content);
        }

        // ── Identificador/palabra clave ──────────────────────────────────────
        if (isLetter(character)) {
            String literal = readIdentifier();
            TokenType type = Token.lookupTokenType(literal);
            return new Token(type, literal);
        }

        // ── Número (entero o float) ──────────────────────────────────────────
        if (Character.isDigit(character)) {
            return readNumber();
        }

        // ── Caracter ilegal ──────────────────────────────────────────────────
        char illegal = character;
        readCharacter();
        return new Token(TokenType.ILLEGAL, String.valueOf(illegal));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos de apoyo
    // ─────────────────────────────────────────────────────────────────────────

    private void readCharacter() {
        if (readPosition >= source.length()) {
            character = 0;
        } else {
            character = source.charAt(readPosition);
        }
        position = readPosition;
        readPosition++;
    }

    private char peekCharacter() {
        if (readPosition >= source.length()) return 0;
        return source.charAt(readPosition);
    }

    private Token makeTwoCharToken(TokenType type) {
        char first = character;
        readCharacter();
        char second = character;
        readCharacter();
        return new Token(type, "" + first + second);
    }

    private void skipWhitespaceAndComments() {
        while (true) {
            if (Character.isWhitespace(character)) {
                readCharacter();
            } else if (character == '/' && peekCharacter() == '/') {
                while (character != '\n' && character != 0) {
                    readCharacter();
                }
            } else {
                break;
            }
        }
    }

    private boolean isLetter(char ch) {
        return Character.isLetter(ch) || ch == '_';
    }

    private String readIdentifier() {
        int start = position;
        while (isLetter(character) || Character.isDigit(character)) {
            readCharacter();
        }
        return source.substring(start, position);
    }

    private Token readNumber() {
        int start = position;
        TokenType type = TokenType.INTEGER;

        while (Character.isDigit(character)) readCharacter();

        if (character == '.' && Character.isDigit(peekCharacter())) {
            type = TokenType.FLOAT;
            readCharacter();
            while (Character.isDigit(character)) readCharacter();
        }

        return new Token(type, source.substring(start, position));
    }

    private String readString() {
        readCharacter();
        int start = position;
        while (character != '"' && character != 0) {
            readCharacter();
        }
        String content = source.substring(start, position);
        readCharacter();
        return content;
    }
}
