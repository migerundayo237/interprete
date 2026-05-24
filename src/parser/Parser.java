package parser;

import ast.AST.*;
import lexer.Lexer;
import Token.Token;
import Token.TokenType;

import java.util.*;

/**
 * Parser — Analizador Sintáctico de LauraSeFue.
 *
 * Implementa un Pratt Parser (Top-Down Operator Precedence, 1973).
 * Toma la secuencia de Tokens del Lexer y construye el AST.
 *
 * Conceptos clave del algoritmo de Pratt:
 *
 *   PREFIX fn  → qué hacer cuando un token aparece sin nada a su izquierda
 *                Ej: -5, !true, (expr), identificador, literal
 *
 *   INFIX fn   → qué hacer cuando un token aparece entre dos operandos
 *                Ej: 5 + 3, fun(args)
 *
 *   PRECEDENCE → jerarquía numérica que determina qué operador "atrae" más
 *                PRODUCT(7) > SUM(6) garantiza que * se evalúa antes que +
 *
 * Uso:
 *   Parser parser = new Parser(new Lexer("let x = 5 + 3;"));
 *   Program program = parser.parseProgram();
 *   if (!parser.getErrors().isEmpty()) { // mostrar errores }
 */
public class Parser {

    // ── Tabla de Precedencias (corazón del algoritmo de Pratt) ──────────────
    // Números más altos = mayor prioridad de parseo
    private enum Precedence {
        LOWEST      (1),
        OR          (2),   // or
        AND         (3),   // and
        EQUALS      (4),   // == !=
        LESSGREATER (5),   // < > <= >=
        SUM         (6),   // + -
        PRODUCT     (7),   // * / %
        PREFIX      (8),   // -X  !X
        POWER       (9),   // ^
        CALL        (10);  // miFuncion(X)

        final int value;
        Precedence(int v) { this.value = v; }
    }

    // Mapa de token → su precedencia cuando aparece en posición INFIX
    private static final Map<TokenType, Precedence> PRECEDENCES = new EnumMap<>(TokenType.class);
    static {
        PRECEDENCES.put(TokenType.OR,       Precedence.OR);
        PRECEDENCES.put(TokenType.AND,      Precedence.AND);
        PRECEDENCES.put(TokenType.EQ,       Precedence.EQUALS);
        PRECEDENCES.put(TokenType.DIF,   Precedence.EQUALS);
        PRECEDENCES.put(TokenType.LT,       Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.GT,       Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.LTE,    Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.GTE,    Precedence.LESSGREATER);
        PRECEDENCES.put(TokenType.PLUS,     Precedence.SUM);
        PRECEDENCES.put(TokenType.MINUS,    Precedence.SUM);
        PRECEDENCES.put(TokenType.MULTIPLY, Precedence.PRODUCT);
        PRECEDENCES.put(TokenType.DIVISION,   Precedence.PRODUCT);
        PRECEDENCES.put(TokenType.MOD,   Precedence.PRODUCT);
        PRECEDENCES.put(TokenType.POW,    Precedence.POWER);
        PRECEDENCES.put(TokenType.LPAREN,   Precedence.CALL);
    }

    // Tipos funcionales para las funciones de parseo
    @FunctionalInterface interface PrefixParseFn { Expression parse(); }
    @FunctionalInterface interface InfixParseFn  { Expression parse(Expression left); }

    // ── Estado del Parser ────────────────────────────────────────────────────
    private final Lexer lexer;
    private Token currentToken;
    private Token peekToken;
    private final List<String> errors = new ArrayList<>();

    // Registros: TokenType → función de parseo
    private final Map<TokenType, PrefixParseFn> prefixParseFns = new EnumMap<>(TokenType.class);
    private final Map<TokenType, InfixParseFn>  infixParseFns  = new EnumMap<>(TokenType.class);

    // ── Constructor ──────────────────────────────────────────────────────────

    public Parser(Lexer lexer) {
        this.lexer = lexer;

        // ── Registro de funciones PREFIX ─────────────────────────────────────
        // "¿Qué hacer si este token aparece al inicio de una expresión?"
        prefixParseFns.put(TokenType.IDENTIFIER, this::parseIdentifier);
        prefixParseFns.put(TokenType.INTEGER,    this::parseIntegerLiteral);
        prefixParseFns.put(TokenType.FLOAT,      this::parseFloatLiteral);
        prefixParseFns.put(TokenType.STRING,     this::parseStringLiteral);
        prefixParseFns.put(TokenType.TRUE,       this::parseBooleanLiteral);
        prefixParseFns.put(TokenType.FALSE,      this::parseBooleanLiteral);
        prefixParseFns.put(TokenType.NEGATION,       this::parsePrefixExpression);
        prefixParseFns.put(TokenType.MINUS,      this::parsePrefixExpression);
        prefixParseFns.put(TokenType.LPAREN,     this::parseGroupedExpression);
        prefixParseFns.put(TokenType.IF,         this::parseIfExpression);
        prefixParseFns.put(TokenType.FUNCTION,   this::parseFunctionLiteral);

        // ── Registro de funciones INFIX ──────────────────────────────────────
        // "¿Qué hacer si este token aparece en medio de dos operandos?"
        for (TokenType op : List.of(
                TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY,
                TokenType.DIVISION, TokenType.MOD, TokenType.POW,
                TokenType.EQ, TokenType.DIF,
                TokenType.LT, TokenType.GT, TokenType.LTE, TokenType.GTE,
                TokenType.AND, TokenType.OR)) {
            infixParseFns.put(op, this::parseInfixExpression);
        }
        infixParseFns.put(TokenType.LPAREN, this::parseCallExpression);

        // Avanzamos dos veces para llenar currentToken y peekToken
        advance();
        advance();
    }

    // ── API pública ──────────────────────────────────────────────────────────

    /**
     * Punto de entrada: parsea el programa completo y retorna el nodo raíz.
     */
    public Program parseProgram() {
        Program program = new Program();

        while (currentToken.tokenType != TokenType.EOF) {
            Statement stmt = parseStatement();
            if (stmt != null) program.statements.add(stmt);
            advance();
        }

        return program;
    }

    public List<String> getErrors() { return Collections.unmodifiableList(errors); }

    // ── Despacho de sentencias ───────────────────────────────────────────────

    /**
     * Decide qué tipo de sentencia parsear según el token actual.
     * Es el corazón del "Recursive Descent" para sentencias.
     */
    private Statement parseStatement() {
        return switch (currentToken.tokenType) {
            case LET      -> parseLetStatement();
            case RETURN   -> parseReturnStatement();
            case WHILE    -> parseWhileStatement();
            case FOR      -> parseForStatement();
            case BREAK    -> parseBreakStatement();
            case CONTINUE -> parseContinueStatement();
            case PRINT    -> parsePrintStatement();
            default       -> parseExpressionStatement();
        };
    }

    // ── Parseo de sentencias concretas ───────────────────────────────────────

    /**
     * let <name> = <value>;
     *
     * Flujo (según la presentación, slide 14.1):
     * 1. Crear nodo LetStatement con token actual (let)
     * 2. Exigir IDENTIFIER → extraer nombre
     * 3. Exigir ASSIGN (=)
     * 4. Parsear la expresión del valor con _parse_expression
     */
    private LetStatement parseLetStatement() {
        LetStatement stmt = new LetStatement(currentToken);

        if (!expectPeek(TokenType.IDENTIFIER)) return null;
        stmt.name = new Identifier(currentToken, currentToken.literal);

        if (!expectPeek(TokenType.ASSIGN)) return null;

        advance(); // ahora currentToken es el inicio de la expresión
        stmt.value = parseExpression(Precedence.LOWEST);

        // Propagar nombre a FunctionLiteral para mejor debug
        if (stmt.value instanceof FunctionLiteral fn && fn.name.isEmpty()) {
            fn.name = stmt.name.value;
        }

        if (peekTokenIs(TokenType.SEMICOLON)) advance();
        return stmt;
    }

    /**
     * return <value>;
     *
     * Flujo (slide 14.2):
     * 1. Crear nodo ReturnStatement
     * 2. Avanzar al valor
     * 3. Parsear la expresión
     */
    private ReturnStatement parseReturnStatement() {
        ReturnStatement stmt = new ReturnStatement(currentToken);
        advance();
        stmt.returnValue = parseExpression(Precedence.LOWEST);
        if (peekTokenIs(TokenType.SEMICOLON)) advance();
        return stmt;
    }

    /**
     * while (<condition>) { <body> }
     */
    private WhileStatement parseWhileStatement() {
        WhileStatement stmt = new WhileStatement(currentToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        advance();
        stmt.condition = parseExpression(Precedence.LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        if (!expectPeek(TokenType.LBRACE)) return null;
        stmt.body = parseBlockStatement();

        return stmt;
    }

    /**
     * for (<init>; <condition>; <update>) { <body> }
     *
     * El init y update son LetStatements o ExpressionStatements.
     * Todos los componentes son opcionales.
     */
    private ForStatement parseForStatement() {
        ForStatement stmt = new ForStatement(currentToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        advance();

        // After expectPeek(LPAREN) + advance(), currentToken is first token after '('
        // INIT: parse statement (LetStatement will consume its own ';')
        if (!currentTokenIs(TokenType.SEMICOLON)) {
            stmt.init = parseStatement();
            // After parseStatement, currentToken is on ';' (consumed by LetStatement)
        }
        // currentToken should be ';' separator between init and condition
        advance(); // move to first token of condition

        // CONDITION: parse expression up to ';'
        if (!currentTokenIs(TokenType.SEMICOLON)) {
            stmt.condition = parseExpression(Precedence.LOWEST);
            if (peekTokenIs(TokenType.SEMICOLON)) advance();
        }
        advance(); // move to first token of update

        // UPDATE: parse statement up to ')'
        if (!currentTokenIs(TokenType.RPAREN)) {
            stmt.update = parseStatement();
            // LetStatement consumes ';'; might leave us before ')'
        }
        // Ensure we are at ')'
        if (!currentTokenIs(TokenType.RPAREN) && !expectPeek(TokenType.RPAREN)) return null;
        if (!expectPeek(TokenType.LBRACE)) return null;
        stmt.body = parseBlockStatement();

        return stmt;
    }

    private BreakStatement parseBreakStatement() {
        BreakStatement stmt = new BreakStatement(currentToken);
        if (peekTokenIs(TokenType.SEMICOLON)) advance();
        return stmt;
    }

    private ContinueStatement parseContinueStatement() {
        ContinueStatement stmt = new ContinueStatement(currentToken);
        if (peekTokenIs(TokenType.SEMICOLON)) advance();
        return stmt;
    }

    private PrintStatement parsePrintStatement() {
        PrintStatement stmt = new PrintStatement(currentToken);
        if (!expectPeek(TokenType.LPAREN)) return null;
        advance();
        stmt.value = parseExpression(Precedence.LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        if (peekTokenIs(TokenType.SEMICOLON)) advance();
        return stmt;
    }

    private ExpressionStatement parseExpressionStatement() {
        ExpressionStatement stmt = new ExpressionStatement(currentToken);
        stmt.expression = parseExpression(Precedence.LOWEST);
        if (peekTokenIs(TokenType.SEMICOLON)) advance();
        return stmt;
    }

    /**
     * { <statements...> }
     *
     * Parsea un bloque de código. Se usa en if, while, for y funciones.
     */
    private BlockStatement parseBlockStatement() {
        BlockStatement block = new BlockStatement(currentToken);
        advance(); // consume '{'

        while (!currentTokenIs(TokenType.RBRACE) && !currentTokenIs(TokenType.EOF)) {
            Statement stmt = parseStatement();
            if (stmt != null) block.statements.add(stmt);
            advance();
        }

        return block;
    }

    // ── El corazón del algoritmo de Pratt ────────────────────────────────────

    /**
     * parseExpression — el algoritmo de Pratt en acción (slide 15).
     *
     * 1. Obtiene la función PREFIX para el token actual
     * 2. La ejecuta → obtiene la expresión izquierda
     * 3. Mientras el siguiente operador tenga MAYOR precedencia que la actual,
     *    envuelve la expresión izquierda dentro de la función INFIX
     *
     * Esto garantiza la precedencia matemática correcta:
     *   5 + 3 * 2  →  5 + (3 * 2)  porque PRODUCT > SUM
     */
    private Expression parseExpression(Precedence precedence) {
        PrefixParseFn prefixFn = prefixParseFns.get(currentToken.tokenType);
        if (prefixFn == null) {
            errors.add("No se encontró función de parseo para '" + currentToken.literal + "' (" + currentToken.tokenType + ")");
            return null;
        }

        Expression leftExpr = prefixFn.parse();

        // La "gravedad" de Pratt: mientras el operador peek tenga mayor
        // precedencia, el left es "absorbido" por el operador infix
        while (!peekTokenIs(TokenType.SEMICOLON) && precedence.value < peekPrecedence().value) {
            InfixParseFn infixFn = infixParseFns.get(peekToken.tokenType);
            if (infixFn == null) return leftExpr;
            advance();
            leftExpr = infixFn.parse(leftExpr);
        }

        return leftExpr;
    }

    // ── Funciones PREFIX ─────────────────────────────────────────────────────

    /** Slide 12.1: caso más simple — toma el token actual y crea Identifier */
    private Expression parseIdentifier() {
        return new Identifier(currentToken, currentToken.literal);
    }

    /** Slide 12.1: parsea entero, maneja error de conversión */
    private Expression parseIntegerLiteral() {
        try {
            long value = Long.parseLong(currentToken.literal);
            return new IntegerLiteral(currentToken, value);
        } catch (NumberFormatException e) {
            errors.add("No se pudo convertir '" + currentToken.literal + "' a entero");
            return null;
        }
    }

    private Expression parseFloatLiteral() {
        try {
            double value = Double.parseDouble(currentToken.literal);
            return new FloatLiteral(currentToken, value);
        } catch (NumberFormatException e) {
            errors.add("No se pudo convertir '" + currentToken.literal + "' a flotante");
            return null;
        }
    }

    private Expression parseStringLiteral() {
        return new StringLiteral(currentToken, currentToken.literal);
    }

    private Expression parseBooleanLiteral() {
        return new BooleanLiteral(currentToken, currentTokenIs(TokenType.TRUE));
    }

    /** Prefix: -expr  o  !expr */
    private Expression parsePrefixExpression() {
        PrefixExpression expr = new PrefixExpression(currentToken, currentToken.literal);
        advance();
        expr.right = parseExpression(Precedence.PREFIX);
        return expr;
    }

    /**
     * Slide 18: paréntesis agrupadores — (expr)
     *
     * No existe nodo "Paréntesis" en el AST. Los paréntesis simplemente
     * resetean la precedencia al mínimo para evaluar su interior aislado.
     */
    private Expression parseGroupedExpression() {
        advance(); // consume '('
        Expression expr = parseExpression(Precedence.LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        return expr;
    }

    /**
     * if (condition) { consequence } elseif (c) { } else { }
     */
    private Expression parseIfExpression() {
        IfExpression expr = new IfExpression(currentToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        advance();
        expr.condition = parseExpression(Precedence.LOWEST);
        if (!expectPeek(TokenType.RPAREN)) return null;
        if (!expectPeek(TokenType.LBRACE)) return null;
        expr.consequence = parseBlockStatement();

        // Ramas elseif (puede haber varias)
        while (peekTokenIs(TokenType.ELSEIF)) {
            advance(); // consume 'elseif'
            if (!expectPeek(TokenType.LPAREN)) return null;
            advance();
            Expression altCond = parseExpression(Precedence.LOWEST);
            if (!expectPeek(TokenType.RPAREN)) return null;
            if (!expectPeek(TokenType.LBRACE)) return null;
            BlockStatement altBlock = parseBlockStatement();
            expr.alternatives.add(new ElseIfBranch(altCond, altBlock));
        }

        // Rama else final
        if (peekTokenIs(TokenType.ELSE)) {
            advance(); // consume 'else'
            if (!expectPeek(TokenType.LBRACE)) return null;
            expr.elseBlock = parseBlockStatement();
        }

        return expr;
    }

    /**
     * Slide 19: function(<params>) { <body> }
     */
    private Expression parseFunctionLiteral() {
        FunctionLiteral fn = new FunctionLiteral(currentToken);

        if (!expectPeek(TokenType.LPAREN)) return null;
        fn.parameters = parseFunctionParameters();
        if (!expectPeek(TokenType.LBRACE)) return null;
        fn.body = parseBlockStatement();

        return fn;
    }

    private List<Identifier> parseFunctionParameters() {
        List<Identifier> params = new ArrayList<>();

        if (peekTokenIs(TokenType.RPAREN)) { advance(); return params; }

        advance();
        params.add(new Identifier(currentToken, currentToken.literal));

        while (peekTokenIs(TokenType.COMMA)) {
            advance(); advance();
            params.add(new Identifier(currentToken, currentToken.literal));
        }

        if (!expectPeek(TokenType.RPAREN)) return params;
        return params;
    }

    // ── Funciones INFIX ──────────────────────────────────────────────────────

    /**
     * Slide 15-17: operador binario — left <op> right
     *
     * Captura la expresión izquierda, avanza al operador,
     * y parsea recursivamente la derecha con la precedencia del operador.
     */
    private Expression parseInfixExpression(Expression left) {
        InfixExpression expr = new InfixExpression(currentToken, left, currentToken.literal);
        Precedence prec = currentPrecedence();
        advance();

        // POWER es right-associative (2^3^2 = 2^(3^2) = 512)
        if (expr.operator.equals("^")) {
            expr.right = parseExpression(Precedence.values()[prec.ordinal() - 1]);
        } else {
            expr.right = parseExpression(prec);
        }

        return expr;
    }

    /**
     * Slide 19: llamada a función — function(arg1, arg2, ...)
     *
     * El LPAREN tiene rol INFIX: si sigue a un identificador o función,
     * es una llamada, no una agrupación.
     */
    private Expression parseCallExpression(Expression function) {
        CallExpression call = new CallExpression(currentToken, function);
        call.arguments = parseCallArguments();
        return call;
    }

    private List<Expression> parseCallArguments() {
        List<Expression> args = new ArrayList<>();

        if (peekTokenIs(TokenType.RPAREN)) { advance(); return args; }

        advance();
        args.add(parseExpression(Precedence.LOWEST));

        while (peekTokenIs(TokenType.COMMA)) {
            advance(); advance();
            args.add(parseExpression(Precedence.LOWEST));
        }

        if (!expectPeek(TokenType.RPAREN)) return args;
        return args;
    }

    // ── Utilidades del Parser ────────────────────────────────────────────────

    private void advance() {
        currentToken = peekToken;
        peekToken = lexer.nextToken();
    }

    private boolean currentTokenIs(TokenType type) {
        return currentToken.tokenType == type;
    }

    private boolean peekTokenIs(TokenType type) {
        return peekToken.tokenType == type;
    }

    /**
     * Slide 20: mecanismo de "recovery" — exige un tipo de token.
     *
     * Si el siguiente token es del tipo esperado, avanza y retorna true.
     * Si no, agrega un error semántico y retorna false (sin explotar).
     */
    private boolean expectPeek(TokenType type) {
        if (peekTokenIs(type)) {
            advance();
            return true;
        }
        errors.add("Se esperaba el token '" + type + "' pero se encontró '" + peekToken.tokenType + "' (\"" + peekToken.literal + "\")");
        return false;
    }

    private Precedence peekPrecedence() {
        return PRECEDENCES.getOrDefault(peekToken.tokenType, Precedence.LOWEST);
    }

    private Precedence currentPrecedence() {
        return PRECEDENCES.getOrDefault(currentToken.tokenType, Precedence.LOWEST);
    }
}

