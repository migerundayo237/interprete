package ast;

import Token.Token;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * AST — Árbol Sintáctico Abstracto (Abstract Syntax Tree)
 *
 * Traducción directa del ast.py de LauraSeFue a Java.
 * Jerarquía:
 *
 *   Node
 *   ├── Statement
 *   │   ├── Program              ← nodo raíz
 *   │   ├── LetStatement         ← let x = expr;
 *   │   ├── ReturnStatement      ← return expr;
 *   │   ├── ExpressionStatement  ← expr;
 *   │   ├── BlockStatement       ← { stmt... }
 *   │   ├── WhileStatement       ← while (cond) { }
 *   │   ├── ForStatement         ← for (init; cond; update) { }
 *   │   ├── BreakStatement       ← break;
 *   │   ├── ContinueStatement    ← continue;
 *   │   └── PrintStatement       ← print(expr);
 *   │
 *   └── Expression
 *       ├── Identifier           ← x
 *       ├── IntegerLiteral       ← 42
 *       ├── FloatLiteral         ← 3.14
 *       ├── StringLiteral        ← "hola"
 *       ├── BooleanLiteral       ← true / false
 *       ├── PrefixExpression     ← !x  -x
 *       ├── InfixExpression      ← x + y
 *       ├── IfExpression         ← if / elseif / else
 *       ├── FunctionLiteral      ← function(p) { body }
 *       └── CallExpression       ← f(args)
 */
public class AST {

    // =========================================================================
    // INTERFACES BASE
    // =========================================================================

    public interface Node {
        String tokenLiteral();
        String toString();
    }

    public interface Statement extends Node {}

    public interface Expression extends Node {}

    // =========================================================================
    // NODO RAÍZ
    // =========================================================================

    /**
     * Program — Nodo raíz. Contiene todas las sentencias del programa.
     */
    public static class Program implements Node {
        public List<Statement> statements = new ArrayList<>();

        @Override public String tokenLiteral() {
            return statements.isEmpty() ? "" : statements.get(0).tokenLiteral();
        }

        @Override public String toString() {
            return statements.stream().map(Object::toString).collect(Collectors.joining("\n"));
        }
    }

    // =========================================================================
    // SENTENCIAS
    // =========================================================================

    /**
     * LetStatement — let <name> = <value>;
     *
     * Ejemplo:  let resultado = 10 + 5;
     *   LetStatement
     *     name:  Identifier("resultado")
     *     value: InfixExpression(10 + 5)
     */
    public static class LetStatement implements Statement {
        public Token token;          // Token LET
        public Identifier name;
        public Expression value;

        public LetStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            return "let " + name + " = " + (value != null ? value : "") + ";";
        }
    }

    /**
     * ReturnStatement — return <value>;
     */
    public static class ReturnStatement implements Statement {
        public Token token;          // Token RETURN
        public Expression returnValue;

        public ReturnStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            return "return " + (returnValue != null ? returnValue : "") + ";";
        }
    }

    /**
     * ExpressionStatement — Una expresión usada como sentencia completa.
     * Ej: miFuncion(x);
     */
    public static class ExpressionStatement implements Statement {
        public Token token;
        public Expression expression;

        public ExpressionStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            return expression != null ? expression.toString() : "";
        }
    }

    /**
     * BlockStatement — { stmt; stmt; ... }
     */
    public static class BlockStatement implements Statement {
        public Token token;          // Token LBRACE {
        public List<Statement> statements = new ArrayList<>();

        public BlockStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            String body = statements.stream()
                    .map(s -> "  " + s)
                    .collect(Collectors.joining("\n"));
            return "{\n" + body + "\n}";
        }
    }

    /**
     * WhileStatement — while (<condition>) { <body> }
     */
    public static class WhileStatement implements Statement {
        public Token token;          // Token WHILE
        public Expression condition;
        public BlockStatement body;

        public WhileStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            return "while (" + condition + ") " + body;
        }
    }

    /**
     * ForStatement — for (<init>; <condition>; <update>) { <body> }
     *
     * Todos los componentes son opcionales (pueden ser null).
     */
    public static class ForStatement implements Statement {
        public Token token;           // Token FOR
        public Statement init;        // let i = 0
        public Expression condition;  // i < 10
        public Statement update;      // let i = i + 1
        public BlockStatement body;

        public ForStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            return "for (" + init + "; " + condition + "; " + update + ") " + body;
        }
    }

    /** BreakStatement — break; */
    public static class BreakStatement implements Statement {
        public Token token;
        public BreakStatement(Token token) { this.token = token; }
        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return "break;"; }
    }

    /** ContinueStatement — continue; */
    public static class ContinueStatement implements Statement {
        public Token token;
        public ContinueStatement(Token token) { this.token = token; }
        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return "continue;"; }
    }

    /** PrintStatement — print(<value>); */
    public static class PrintStatement implements Statement {
        public Token token;
        public Expression value;

        public PrintStatement(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return "print(" + value + ");"; }
    }

    // =========================================================================
    // EXPRESIONES
    // =========================================================================

    /** Identifier — nombre de variable o función: x, resultado */
    public static class Identifier implements Expression {
        public Token token;
        public String value;

        public Identifier(Token token, String value) {
            this.token = token;
            this.value = value;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return value; }
    }

    /** IntegerLiteral — 42 */
    public static class IntegerLiteral implements Expression {
        public Token token;
        public long value;

        public IntegerLiteral(Token token, long value) {
            this.token = token;
            this.value = value;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return String.valueOf(value); }
    }

    /** FloatLiteral — 3.14 */
    public static class FloatLiteral implements Expression {
        public Token token;
        public double value;

        public FloatLiteral(Token token, double value) {
            this.token = token;
            this.value = value;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return String.valueOf(value); }
    }

    /** StringLiteral — "hola mundo" */
    public static class StringLiteral implements Expression {
        public Token token;
        public String value;

        public StringLiteral(Token token, String value) {
            this.token = token;
            this.value = value;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return "\"" + value + "\""; }
    }

    /** BooleanLiteral — true / false */
    public static class BooleanLiteral implements Expression {
        public Token token;
        public boolean value;

        public BooleanLiteral(Token token, boolean value) {
            this.token = token;
            this.value = value;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return value ? "true" : "false"; }
    }

    /**
     * PrefixExpression — <operator><right>
     *
     * Ejemplos:  !verdadero   -5
     */
    public static class PrefixExpression implements Expression {
        public Token token;
        public String operator;
        public Expression right;

        public PrefixExpression(Token token, String operator) {
            this.token = token;
            this.operator = operator;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() { return "(" + operator + right + ")"; }
    }

    /**
     * InfixExpression — <left> <operator> <right>
     *
     * Ejemplos:  5 + 3   x == y   a and b
     */
    public static class InfixExpression implements Expression {
        public Token token;
        public Expression left;
        public String operator;
        public Expression right;

        public InfixExpression(Token token, Expression left, String operator) {
            this.token = token;
            this.left = left;
            this.operator = operator;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            return "(" + left + " " + operator + " " + right + ")";
        }
    }

    /**
     * ElseIfBranch — rama de elseif: (condición, bloque)
     */
    public static class ElseIfBranch {
        public Expression condition;
        public BlockStatement block;

        public ElseIfBranch(Expression condition, BlockStatement block) {
            this.condition = condition;
            this.block = block;
        }
    }

    /**
     * IfExpression — if (cond) { } elseif (c) { } else { }
     */
    public static class IfExpression implements Expression {
        public Token token;                      // Token IF
        public Expression condition;
        public BlockStatement consequence;
        public List<ElseIfBranch> alternatives = new ArrayList<>();
        public BlockStatement elseBlock;         // Bloque else final (puede ser null)

        public IfExpression(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("if (").append(condition).append(") ").append(consequence);
            for (ElseIfBranch b : alternatives) {
                sb.append(" elseif (").append(b.condition).append(") ").append(b.block);
            }
            if (elseBlock != null) sb.append(" else ").append(elseBlock);
            return sb.toString();
        }
    }

    /**
     * FunctionLiteral — function(<params>) { <body> }
     *
     * Las funciones son valores de primera clase: se pueden asignar,
     * pasar como argumentos y retornar desde otras funciones.
     */
    public static class FunctionLiteral implements Expression {
        public Token token;
        public List<Identifier> parameters = new ArrayList<>();
        public BlockStatement body;
        public String name = "";   // Nombre si fue asignada con let

        public FunctionLiteral(Token token) { this.token = token; }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            String params = parameters.stream().map(Object::toString).collect(Collectors.joining(", "));
            String nameStr = name.isEmpty() ? "" : " " + name;
            return "function" + nameStr + "(" + params + ") " + body;
        }
    }

    /**
     * CallExpression — <function>(<arguments>)
     *
     * Ej: factorial(5)   suma(x, y + 1)
     */
    public static class CallExpression implements Expression {
        public Token token;           // Token LPAREN de la llamada
        public Expression function;   // Identifier o FunctionLiteral
        public List<Expression> arguments = new ArrayList<>();

        public CallExpression(Token token, Expression function) {
            this.token = token;
            this.function = function;
        }

        @Override public String tokenLiteral() { return token.literal; }
        @Override public String toString() {
            String args = arguments.stream().map(Object::toString).collect(Collectors.joining(", "));
            return function + "(" + args + ")";
        }
    }
}

