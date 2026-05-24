package repl;

import evaluator.Evaluator;
import lexer.Lexer;
import object.Environment;
import object.LObject.LObj;
import object.LObject.LNull;
import parser.Parser;
import ast.AST.Program;

import java.util.List;
import java.util.Scanner;

/**
 * REPL — Read-Eval-Print Loop del intérprete LauraSeFue.
 *
 * Permite ejecutar código de forma interactiva línea a línea.
 * El entorno (variables, funciones) persiste entre entradas.
 *
 * Flujo por cada línea:
 *   1. READ   → leer input del usuario
 *   2. LEX    → Lexer tokeniza el input
 *   3. PARSE  → Parser construye el AST
 *   4. EVAL   → Evaluador recorre el AST
 *   5. PRINT  → mostrar el resultado
 */
public class Repl {

    private static final String PROMPT = ">> ";
    private static final String BANNER = """
            ╔══════════════════════════════════════════╗
            ║    LauraSeFue Language Interpreter       ║
            ║    Laura se fue... Laura no está :'(     ║
            ║    Escribe 'exit' o 'salir' para salir   ║
            ╚══════════════════════════════════════════╝
            """;

    public static void start() {
        System.out.println(BANNER);

        Scanner scanner  = new Scanner(System.in);
        Evaluator eval   = new Evaluator();
        Environment env  = new Environment();  // Entorno global persiste

        while (true) {
            System.out.print(PROMPT);

            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;
            if (line.equals("exit") || line.equals("salir")) {
                System.out.println("¡Hasta pronto! Laura sigue sin estar :'(");
                break;
            }

            // ── Lexer ──────────────────────────────────────────────────────
            Lexer  lexer  = new Lexer(line);
            Parser parser = new Parser(lexer);

            // ── Parser ─────────────────────────────────────────────────────
            Program program = parser.parseProgram();

            // Mostrar errores de parseo sin evaluar
            List<String> errors = parser.getErrors();
            if (!errors.isEmpty()) {
                System.out.println("⚠ Errores de sintaxis:");
                errors.forEach(e -> System.out.println("  → " + e));
                continue;
            }

            // ── Evaluador ──────────────────────────────────────────────────
            LObj result = eval.eval(program, env);

            // No imprimir null (resultados de let, print, while…)
            if (result != null && !(result instanceof LNull)) {
                System.out.println(result.inspect());
            }
        }
    }
}

