import ast.AST.*;
import evaluator.Evaluator;
import lexer.Lexer;
import object.Environment;
import object.LObject.*;
import parser.Parser;

import java.util.List;

/**
 * Tests — Suite de pruebas para el intérprete LauraSeFue.
 *
 * Cubre: Lexer, Parser y Evaluador de extremo a extremo.
 *
 * Ejecutar:
 *   javac -d out -sourcepath src src/Tests.java
 *   java -cp out Tests
 */
public class Tests {

    // ── Contadores ────────────────────────────────────────────────────────────
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("    LauraSeFue — Suite de Pruebas");
        System.out.println("═══════════════════════════════════════════════════\n");

        testLiterals();
        testArithmeticPrecedence();
        testBooleanExpressions();
        testPrefixExpressions();
        testLetStatements();
        testIfElse();
        testFunctions();
        testClosures();
        testRecursion();
        testWhileLoop();
        testForLoop();
        testBreakContinue();
        testStrings();
        testErrors();
        testParserAST();

        System.out.println("\n═══════════════════════════════════════════════════");
        System.out.printf("  Resultado: %d passed ✅   %d failed ❌%n", passed, failed);
        System.out.println("═══════════════════════════════════════════════════");

        if (failed > 0) System.exit(1);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private static void testLiterals() {
        section("Literales");
        assertEval("42",    42L,    "Entero positivo");
        assertEval("0",     0L,     "Cero");
        assertEval("3.14",  3.14,   "Float");
        assertEval("true",  true,   "Booleano true");
        assertEval("false", false,  "Booleano false");
    }

    private static void testArithmeticPrecedence() {
        section("Aritmética y Precedencia (Pratt)");
        assertEval("5 + 3",         8L,   "Suma básica");
        assertEval("10 - 4",        6L,   "Resta básica");
        assertEval("3 * 4",         12L,  "Multiplicación");
        assertEval("10 / 2",        5L,   "División");
        assertEval("10 % 3",        1L,   "Módulo");
        assertEval("2 ^ 10",        1024L,"Potencia");
        assertEval("5 + 3 * 2",     11L,  "Precedencia: * antes que +");
        assertEval("(5 + 3) * 2",   16L,  "Paréntesis fuerzan precedencia");
        assertEval("2 ^ 3 ^ 2",     512L, "Potencia right-associative: 2^(3^2)");
        assertEval("10 / 3 + 1",    4L,   "División entera + suma");
        assertEval("1 + 2 + 3 + 4", 10L,  "Cadena de sumas");
        assertEval("1.5 + 2.5",     4.0,  "Suma de floats");
        assertEval("5 + 1.0",       6.0,  "Promoción int→float");
    }

    private static void testBooleanExpressions() {
        section("Expresiones Booleanas");
        assertEval("1 == 1",        true,  "Igual true");
        assertEval("1 == 2",        false, "Igual false");
        assertEval("1 != 2",        true,  "Distinto true");
        assertEval("5 > 3",         true,  "Mayor que");
        assertEval("3 < 5",         true,  "Menor que");
        assertEval("5 >= 5",        true,  "Mayor o igual");
        assertEval("3 <= 2",        false, "Menor o igual false");
        assertEval("true == true",  true,  "Booleanos iguales");
        assertEval("true == false", false, "Booleanos distintos");
        assertEval("true and false",false, "and lógico");
        assertEval("true or false", true,  "or lógico");
    }

    private static void testPrefixExpressions() {
        section("Operadores Prefijo");
        assertEval("!true",  false, "Negación de true");
        assertEval("!false", true,  "Negación de false");
        assertEval("!!true", true,  "Doble negación");
        assertEval("-5",     -5L,   "Negativo entero");
        assertEval("-3.14",  -3.14, "Negativo float");
        assertEval("-(3+2)", -5L,   "Negativo de expresión");
    }

    private static void testLetStatements() {
        section("Declaraciones let");
        assertEval("let x = 5; x",           5L,   "let y uso de variable");
        assertEval("let x = 5; let y = 3; x + y", 8L, "Dos variables");
        assertEval("let x = 5 * 2; x",       10L,  "let con expresión");
        assertEval("let x = 5; let y = x + 1; y", 6L, "Variable que depende de otra");
    }

    private static void testIfElse() {
        section("if / elseif / else");
        assertEval("if (true) { 10 }",              10L,  "if simple verdadero");
        assertEval("if (false) { 10 } else { 20 }", 20L,  "if false → else");
        assertEval("if (1 > 2) { 1 } elseif (2 > 1) { 2 } else { 3 }", 2L, "elseif tomado");
        assertEval("if (false) { 1 } elseif (false) { 2 } else { 3 }", 3L, "else final");
        assertEval("let x = 5; if (x > 3) { x * 2 } else { x }", 10L, "if con variable");
    }

    private static void testFunctions() {
        section("Funciones");
        assertEval("let suma = function(a, b) { return a + b; }; suma(3, 4)", 7L,
                "Función con return");
        assertEval("let doble = function(x) { x * 2 }; doble(5)", 10L,
                "Función sin return explícito (última expresión)");
        assertEval("let identidad = function(x) { x }; identidad(42)", 42L,
                "Función identidad");
        assertEval("function(x) { x + 1 }(9)", 10L,
                "Función anónima invocada inmediatamente (IIFE)");
        assertEval("let a = 5; let f = function() { a }; f()", 5L,
                "Función captura variable del scope externo");
    }

    private static void testClosures() {
        section("Closures y Lexical Scoping");
        // makeCounter retorna una función que captura 'count'
        String src = """
            let makeCounter = function() {
                let count = 0;
                function() { let count = count + 1; count }
            };
            let counter = makeCounter();
            counter()
            """;
        // Nota: este es un closure simple — cada llamada crea nuevo scope
        assertEval("let x = 10; let f = function() { x }; f()", 10L,
                "Closure captura variable externa");
        assertEval("""
            let makeAdder = function(x) {
                function(y) { x + y }
            };
            let add5 = makeAdder(5);
            add5(3)
            """, 8L, "Closure: makeAdder retorna función que recuerda x");
    }

    private static void testRecursion() {
        section("Recursión");
        assertEval("""
            let factorial = function(n) {
                if (n <= 1) { return 1; }
                return n * factorial(n - 1);
            };
            factorial(5)
            """, 120L, "Factorial recursivo");

        assertEval("""
            let fib = function(n) {
                if (n <= 1) { return n; }
                return fib(n - 1) + fib(n - 2);
            };
            fib(10)
            """, 55L, "Fibonacci recursivo");
    }

    private static void testWhileLoop() {
        section("Bucle while");
        assertEval("""
            let i = 0;
            let sum = 0;
            while (i < 5) {
                let sum = sum + i;
                let i = i + 1;
            }
            sum
            """, 10L, "Suma 0+1+2+3+4 con while");
    }

    private static void testForLoop() {
        section("Bucle for");
        assertEval("""
            let result = 0;
            for (let i = 0; i < 5; let i = i + 1) {
                let result = result + i;
            }
            result
            """, 10L, "Suma 0+1+2+3+4 con for");
        assertEval("""
            let sum = function() {
                let result = 0;
                for (let i = 0; i < 5; let i = i + 1) {
                    let result = result + i;
                }
                result
            };
            sum()
            """, 10L, "Suma con for (dentro de función)");
    }

    private static void testBreakContinue() {
        section("break y continue");
        assertEval("""
            let i = 0;
            while (true) {
                if (i >= 3) { break; }
                let i = i + 1;
            }
            i
            """, 3L, "break sale del bucle");
    }

    private static void testStrings() {
        section("Strings");
        assertEvalString("\"hola\"",              "hola",       "String literal");
        assertEvalString("\"hola\" + \" mundo\"", "hola mundo", "Concatenación");
        assertEval("\"abc\" == \"abc\"",           true,         "String igualdad");
        assertEval("\"abc\" != \"xyz\"",           true,         "String desigualdad");
    }

    private static void testErrors() {
        section("Manejo de Errores");
        assertError("x",                 "Identificador no encontrado");
        assertError("true + 1",          "Tipos incompatibles");
        assertError("5 / 0",             "División por cero");
        assertError("let f = function(x) { x }; f(1, 2)", "Cantidad incorrecta");
    }

    private static void testParserAST() {
        section("Parser → AST");

        // Verifica que el parser construye nodos correctos
        Program prog = parse("let x = 5;");
        check(prog.statements.size() == 1, "let produce 1 sentencia");
        check(prog.statements.get(0) instanceof LetStatement, "Nodo es LetStatement");

        LetStatement ls = (LetStatement) prog.statements.get(0);
        check("x".equals(ls.name.value), "Nombre de variable es 'x'");
        check(ls.value instanceof IntegerLiteral, "Valor es IntegerLiteral");
        check(((IntegerLiteral) ls.value).value == 5L, "Valor es 5");

        // Parser no produce errores en código válido
        Parser p = new Parser(new Lexer("let resultado = 2 + 3 * 4;"));
        p.parseProgram();
        check(p.getErrors().isEmpty(), "Código válido no produce errores de parser");

        // Parser detecta errores en código inválido
        Parser p2 = new Parser(new Lexer("let = 5;"));
        p2.parseProgram();
        check(!p2.getErrors().isEmpty(), "Código inválido produce errores de parser");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static LObj run(String src) {
        Lexer lexer = new Lexer(src);
        Parser parser = new Parser(lexer);
        Program prog = parser.parseProgram();
        if (!parser.getErrors().isEmpty()) {
            return new LError("Parser errors: " + String.join(", ", parser.getErrors()));
        }
        return new Evaluator().eval(prog, new Environment());
    }

    private static Program parse(String src) {
        return new Parser(new Lexer(src)).parseProgram();
    }

    private static void assertEval(String src, long expected, String desc) {
        LObj result = run(src);
        if (result instanceof LInteger li && li.value() == expected) {
            pass(desc);
        } else {
            fail(desc, "LInteger(" + expected + ")", result);
        }
    }

    private static void assertEval(String src, double expected, String desc) {
        LObj result = run(src);
        if (result instanceof LFloat lf && Math.abs(lf.value() - expected) < 1e-9) {
            pass(desc);
        } else {
            fail(desc, "LFloat(" + expected + ")", result);
        }
    }

    private static void assertEval(String src, boolean expected, String desc) {
        LObj result = run(src);
        if (result instanceof LBoolean lb && lb.value() == expected) {
            pass(desc);
        } else {
            fail(desc, "LBoolean(" + expected + ")", result);
        }
    }

    private static void assertEvalString(String src, String expected, String desc) {
        LObj result = run(src);
        if (result instanceof LString ls && ls.value().equals(expected)) {
            pass(desc);
        } else {
            fail(desc, "LString(\"" + expected + "\")", result);
        }
    }

    private static void assertError(String src, String msgContains) {
        LObj result = run(src);
        if (result instanceof LError le && le.message().contains(msgContains)) {
            pass("Error esperado: '" + msgContains + "'");
        } else {
            fail("Error '" + msgContains + "'", "LError(...)", result);
        }
    }

    private static void check(boolean condition, String desc) {
        if (condition) pass(desc);
        else           fail(desc, "true", "false");
    }

    private static void pass(String desc) {
        passed++;
        System.out.println("  ✅ " + desc);
    }

    private static void fail(String desc, String expected, Object actual) {
        failed++;
        System.out.println("  ❌ " + desc);
        System.out.println("       esperado : " + expected);
        System.out.println("       obtenido : " + (actual != null ? actual : "null"));
    }

    private static void section(String name) {
        System.out.println("\n▶ " + name);
    }
}

