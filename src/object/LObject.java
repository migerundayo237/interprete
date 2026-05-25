package object;

import ast.AST.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Object System — Representaciones dinámicas de ejecución.
 *
 * El Evaluador produce estos objetos al recorrer el AST.
 * Cada tipo de valor que Javadabadú puede manejar en runtime
 * tiene su clase correspondiente aquí.
 *
 * Slide 21 de la presentación:
 *   "Objetos: representaciones dinámicas de ejecución
 *    (Integer, String, Function Object)"
 */
public class LObject {

    public enum ObjectType {
        INTEGER, FLOAT, STRING, BOOLEAN,
        NULL, RETURN_VALUE, FUNCTION,
        BREAK_SIGNAL, CONTINUE_SIGNAL,
        ERROR
    }

    // ── Interfaz base ────────────────────────────────────────────────────────

    public interface LObj {
        ObjectType type();
        String inspect();
    }

    // ── Tipos de valor ───────────────────────────────────────────────────────

    /** Número entero: 42, -7, 0 */
    public record LInteger(long value) implements LObj {
        @Override public ObjectType type() { return ObjectType.INTEGER; }
        @Override public String inspect() { return String.valueOf(value); }
    }

    /** Número decimal: 3.14, 2.0 */
    public record LFloat(double value) implements LObj {
        @Override public ObjectType type() { return ObjectType.FLOAT; }
        @Override public String inspect() { return String.valueOf(value); }
    }

    /** Cadena de texto: "hola mundo" */
    public record LString(String value) implements LObj {
        @Override public ObjectType type() { return ObjectType.STRING; }
        @Override public String inspect() { return value; }
    }

    /** Booleano: true / false */
    public record LBoolean(boolean value) implements LObj {
        @Override public ObjectType type() { return ObjectType.BOOLEAN; }
        @Override public String inspect() { return value ? "true" : "false"; }
    }

    /** Null: ausencia de valor */
    public static class LNull implements LObj {
        public static final LNull INSTANCE = new LNull();
        private LNull() {}
        @Override public ObjectType type() { return ObjectType.NULL; }
        @Override public String inspect() { return "null"; }
    }

    /**
     * ReturnValue — Envuelve el valor de un 'return' para detener la ejecución
     * del bloque actual y propagar el valor hacia arriba en el call stack.
     */
    public record LReturnValue(LObj value) implements LObj {
        @Override public ObjectType type() { return ObjectType.RETURN_VALUE; }
        @Override public String inspect() { return value.inspect(); }
    }

    /**
     * Break / Continue — Señales de control de flujo para bucles.
     * Funcionan como LReturnValue pero no llevan valor.
     */
    public static class LBreak implements LObj {
        public static final LBreak INSTANCE = new LBreak();
        private LBreak() {}
        @Override public ObjectType type() { return ObjectType.BREAK_SIGNAL; }
        @Override public String inspect() { return "break"; }
    }

    public static class LContinue implements LObj {
        public static final LContinue INSTANCE = new LContinue();
        private LContinue() {}
        @Override public ObjectType type() { return ObjectType.CONTINUE_SIGNAL; }
        @Override public String inspect() { return "continue"; }
    }

    /**
     * Function — Objeto función de primera clase.
     *
     * Slide 21: "Lexical Scoping: al instanciar una función,
     * guardamos una referencia al Environment actual para soportar
     * recursión y Closures."
     *
     * Al almacenar el Environment del momento de la definición,
     * la función "recuerda" el contexto donde fue creada (closure).
     */
    public record LFunction(
            List<Identifier> parameters,
            BlockStatement body,
            Environment env   // Entorno capturado (closure / lexical scope)
    ) implements LObj {
        @Override public ObjectType type() { return ObjectType.FUNCTION; }
        @Override public String inspect() {
            String params = parameters.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            return "function(" + params + ") {\n" + body + "\n}";
        }
    }

    /**
     * Error — Error en tiempo de ejecución.
     *
     * En lugar de explotar con una excepción Java no controlada,
     * el Evaluador produce un LError que se propaga por el árbol
     * hasta llegar al REPL, que lo muestra al usuario.
     */
    public record LError(String message) implements LObj {
        @Override public ObjectType type() { return ObjectType.ERROR; }
        @Override public String inspect() { return "ERROR: " + message; }
    }

    // ── Singletons comunes ───────────────────────────────────────────────────
    // Reutilizamos instancias de valores inmutables para eficiencia

    public static final LBoolean TRUE  = new LBoolean(true);
    public static final LBoolean FALSE = new LBoolean(false);
    public static final LNull    NULL  = LNull.INSTANCE;
    public static final LBreak   BREAK = LBreak.INSTANCE;
    public static final LContinue CONTINUE = LContinue.INSTANCE;

    public static LBoolean nativeBool(boolean value) {
        return value ? TRUE : FALSE;
    }
}
