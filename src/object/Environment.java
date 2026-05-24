package object;

import java.util.HashMap;
import java.util.Map;

/**
 * Environment — Entorno de ejecución (tabla de variables).
 *
 * Cada entorno tiene un puntero opcional al entorno padre (outer).
 * Esto implementa el Lexical Scoping (Closures).
 *
 *   Global env:  { x: 10, factorial: <fn> }
 *        ↑
 *   Function env: { n: 5 }  ← busca hacia arriba si no encuentra 'x'
 */
public class Environment {

    private final Map<String, LObject.LObj> store = new HashMap<>();
    private final Environment outer;

    public Environment() { this.outer = null; }

    public Environment(Environment outer) { this.outer = outer; }

    /** Busca variable por nombre; sube al padre si no la encuentra. */
    public LObject.LObj get(String name) {
        LObject.LObj value = store.get(name);
        if (value == null && outer != null) return outer.get(name);
        return value;
    }

    /** Crea o sobreescribe la variable en el scope ACTUAL. */
    public LObject.LObj set(String name, LObject.LObj value) {
        store.put(name, value);
        return value;
    }

    /**
     * Actualiza una variable buscando en toda la cadena de scopes.
     * Retorna true si la encontró y actualizó, false si no existe.
     */
    public boolean update(String name, LObject.LObj value) {
        if (store.containsKey(name)) {
            store.put(name, value);
            return true;
        }
        if (outer != null) return outer.update(name, value);
        return false;
    }

    /**
     * setOrUpdate — si la variable ya existe en algún scope, la actualiza ahí.
     * Si no existe en ninguno, la crea en el scope actual.
     *
     * Esto hace que 'let x = val' se comporte como reasignación cuando x ya
     * fue declarada en un scope externo (consistente con el Python original).
     */
    public LObject.LObj setOrUpdate(String name, LObject.LObj value) {
        if (update(name, value)) return value;
        return set(name, value);
    }

    /** Crea un entorno hijo que hereda el scope actual (para funciones/loops). */
    public static Environment newEnclosed(Environment outer) {
        return new Environment(outer);
    }
}

