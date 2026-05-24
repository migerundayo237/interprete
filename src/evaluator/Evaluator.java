package evaluator;

import ast.AST;
import ast.AST.*;
import object.Environment;
import object.LObject;
import object.LObject.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluator — Tree-Walking Interpreter para LauraSeFue.
 *
 * Slide 21: "La Función Eval: método de Tree-Walking que visita nodos
 * usando pattern matching."
 *
 * El evaluador recorre el AST nodo por nodo y produce objetos LObject.
 * No compila a bytecode; ejecuta directamente desde el árbol (Tree-Walking).
 *
 * Flujo completo:
 *   Código fuente → Lexer → Tokens → Parser → AST → Evaluator → LObject
 */
public class Evaluator {

    // Singletons convenientes (atajos a LObject.NULL, TRUE, FALSE, etc.)
    private static final LObj NULL     = LObject.NULL;
    private static final LObj TRUE     = LObject.TRUE;
    private static final LObj FALSE    = LObject.FALSE;
    private static final LObj BREAK    = LObject.BREAK;
    private static final LObj CONTINUE = LObject.CONTINUE;

    // ── Punto de entrada ─────────────────────────────────────────────────────

    /**
     * eval — el método central del Tree-Walking Interpreter.
     * Despacha cada tipo de nodo AST a su handler específico.
     */
    public LObj eval(AST.Node node, Environment env) {
        if (node == null) return NULL;

        // ── Nodo raíz ──────────────────────────────────────────────────────
        if (node instanceof Program p)
            return evalProgram(p, env);

        // ── Sentencias ─────────────────────────────────────────────────────
        if (node instanceof BlockStatement b)
            return evalBlockStatement(b, env);

        if (node instanceof ExpressionStatement es)
            return eval(es.expression, env);

        if (node instanceof LetStatement ls)
            return evalLetStatement(ls, env);

        if (node instanceof ReturnStatement rs) {
            LObj val = eval(rs.returnValue, env);
            if (isError(val)) return val;
            return new LReturnValue(val);
        }

        if (node instanceof WhileStatement ws)
            return evalWhileStatement(ws, env);

        if (node instanceof ForStatement fs)
            return evalForStatement(fs, env);

        if (node instanceof BreakStatement)
            return BREAK;

        if (node instanceof ContinueStatement)
            return CONTINUE;

        if (node instanceof PrintStatement ps)
            return evalPrintStatement(ps, env);

        // ── Expresiones ────────────────────────────────────────────────────
        if (node instanceof IntegerLiteral il)
            return new LInteger(il.value);

        if (node instanceof FloatLiteral fl)
            return new LFloat(fl.value);

        if (node instanceof StringLiteral sl)
            return new LString(sl.value);

        if (node instanceof BooleanLiteral bl)
            return nativeBool(bl.value);

        if (node instanceof Identifier id)
            return evalIdentifier(id, env);

        if (node instanceof PrefixExpression pe) {
            LObj right = eval(pe.right, env);
            if (isError(right)) return right;
            return evalPrefixExpression(pe.operator, right);
        }

        if (node instanceof InfixExpression ie) {
            LObj left  = eval(ie.left, env);
            if (isError(left)) return left;
            LObj right = eval(ie.right, env);
            if (isError(right)) return right;
            return evalInfixExpression(ie.operator, left, right);
        }

        if (node instanceof IfExpression ife)
            return evalIfExpression(ife, env);

        if (node instanceof FunctionLiteral fnLit)
            return new LFunction(fnLit.parameters, fnLit.body, env);

        if (node instanceof CallExpression ce)
            return evalCallExpression(ce, env);

        return newError("Tipo de nodo no reconocido: " + node.getClass().getSimpleName());
    }

    // ── Evaluación del programa raíz ─────────────────────────────────────────

    private LObj evalProgram(Program program, Environment env) {
        LObj result = NULL;
        for (Statement stmt : program.statements) {
            result = eval(stmt, env);
            if (result instanceof LReturnValue rv) return rv.value();
            if (result instanceof LError)          return result;
        }
        return result;
    }

    /**
     * Evalúa un bloque { }. A diferencia de evalProgram, NO desenvuelve
     * LReturnValue — lo deja pasar para que la función lo capture.
     */
    private LObj evalBlockStatement(BlockStatement block, Environment env) {
        LObj result = NULL;
        for (Statement stmt : block.statements) {
            result = eval(stmt, env);
            if (result != null) {
                ObjectType t = result.type();
                if (t == ObjectType.RETURN_VALUE  ||
                        t == ObjectType.BREAK_SIGNAL   ||
                        t == ObjectType.CONTINUE_SIGNAL ||
                        t == ObjectType.ERROR) {
                    return result;
                }
            }
        }
        return result;
    }

    // ── Sentencias ────────────────────────────────────────────────────────────

    private LObj evalLetStatement(LetStatement ls, Environment env) {
        LObj val = eval(ls.value, env);
        if (isError(val)) return val;
        env.setOrUpdate(ls.name.value, val);
        return NULL;
    }

    private LObj evalWhileStatement(WhileStatement ws, Environment env) {
        while (true) {
            LObj cond = eval(ws.condition, env);
            if (isError(cond)) return cond;
            if (!isTruthy(cond)) break;

            LObj result = evalBlockStatement(ws.body, env);
            if (result instanceof LReturnValue) return result;
            if (result instanceof LError)       return result;
            if (result instanceof LBreak)       break;
            if (result instanceof LContinue)    continue;
        }
        return NULL;
    }

    private LObj evalForStatement(ForStatement fs, Environment env) {
        Environment loopEnv = Environment.newEnclosed(env);

        if (fs.init != null) {
            LObj init = eval(fs.init, loopEnv);
            if (isError(init)) return init;
        }

        while (true) {
            if (fs.condition != null) {
                LObj cond = eval(fs.condition, loopEnv);
                if (isError(cond)) return cond;
                if (!isTruthy(cond)) break;
            }

            LObj result = evalBlockStatement(fs.body, loopEnv);
            if (result instanceof LReturnValue) return result;
            if (result instanceof LError)       return result;
            if (result instanceof LBreak)       break;

            if (fs.update != null) {
                LObj upd = eval(fs.update, loopEnv);
                if (isError(upd)) return upd;
            }
        }
        return NULL;
    }

    private LObj evalPrintStatement(PrintStatement ps, Environment env) {
        LObj val = eval(ps.value, env);
        if (isError(val)) return val;
        System.out.println(val.inspect());
        return NULL;
    }

    // ── Expresiones ───────────────────────────────────────────────────────────

    private LObj evalIdentifier(Identifier id, Environment env) {
        LObj val = env.get(id.value);
        if (val == null) return newError("Identificador no encontrado: '" + id.value + "'");
        return val;
    }

    // ── Operadores PREFIX ─────────────────────────────────────────────────────

    private LObj evalPrefixExpression(String op, LObj right) {
        return switch (op) {
            case "!" -> evalBangOperator(right);
            case "-" -> evalMinusPrefixOperator(right);
            default  -> newError("Operador prefijo desconocido: " + op + right.type());
        };
    }

    private LObj evalBangOperator(LObj right) {
        if (right == TRUE)  return FALSE;
        if (right == FALSE) return TRUE;
        if (right == NULL)  return TRUE;
        return FALSE;
    }

    private LObj evalMinusPrefixOperator(LObj right) {
        if (right instanceof LInteger li) return new LInteger(-li.value());
        if (right instanceof LFloat lf)   return new LFloat(-lf.value());
        return newError("Operador '-' no soportado para " + right.type());
    }

    // ── Operadores INFIX ──────────────────────────────────────────────────────

    private LObj evalInfixExpression(String op, LObj left, LObj right) {
        if (left instanceof LInteger li && right instanceof LInteger ri)
            return evalIntegerInfix(op, li.value(), ri.value());

        if ((left instanceof LFloat || left instanceof LInteger) &&
                (right instanceof LFloat || right instanceof LInteger)) {
            double l = toDouble(left), r = toDouble(right);
            return evalFloatInfix(op, l, r);
        }

        if (left instanceof LString ls && right instanceof LString rs) {
            if (op.equals("+"))  return new LString(ls.value() + rs.value());
            if (op.equals("==")) return nativeBool(ls.value().equals(rs.value()));
            if (op.equals("!=")) return nativeBool(!ls.value().equals(rs.value()));
            return newError("Operador '" + op + "' no soportado entre strings");
        }

        return switch (op) {
            case "==" -> nativeBool(left == right);
            case "!=" -> nativeBool(left != right);
            case "and"-> nativeBool(isTruthy(left) && isTruthy(right));
            case "or" -> nativeBool(isTruthy(left) || isTruthy(right));
            default   -> newError("Tipos incompatibles: " + left.type() + " " + op + " " + right.type());
        };
    }

    private LObj evalIntegerInfix(String op, long l, long r) {
        return switch (op) {
            case "+"  -> new LInteger(l + r);
            case "-"  -> new LInteger(l - r);
            case "*"  -> new LInteger(l * r);
            case "/"  -> {
                if (r == 0) yield newError("División por cero");
                yield new LInteger(l / r);
            }
            case "%"  -> new LInteger(l % r);
            case "^"  -> new LInteger((long) Math.pow(l, r));
            case "==" -> nativeBool(l == r);
            case "!=" -> nativeBool(l != r);
            case "<"  -> nativeBool(l <  r);
            case ">"  -> nativeBool(l >  r);
            case "<=" -> nativeBool(l <= r);
            case ">=" -> nativeBool(l >= r);
            case "and"-> nativeBool(l != 0 && r != 0);
            case "or" -> nativeBool(l != 0 || r != 0);
            default   -> newError("Operador entero desconocido: " + op);
        };
    }

    private LObj evalFloatInfix(String op, double l, double r) {
        return switch (op) {
            case "+"  -> new LFloat(l + r);
            case "-"  -> new LFloat(l - r);
            case "*"  -> new LFloat(l * r);
            case "/"  -> {
                if (r == 0.0) yield newError("División por cero");
                yield new LFloat(l / r);
            }
            case "%"  -> new LFloat(l % r);
            case "^"  -> new LFloat(Math.pow(l, r));
            case "==" -> nativeBool(l == r);
            case "!=" -> nativeBool(l != r);
            case "<"  -> nativeBool(l <  r);
            case ">"  -> nativeBool(l >  r);
            case "<=" -> nativeBool(l <= r);
            case ">=" -> nativeBool(l >= r);
            default   -> newError("Operador flotante desconocido: " + op);
        };
    }

    // ── If / elseif / else ────────────────────────────────────────────────────

    private LObj evalIfExpression(IfExpression ife, Environment env) {
        LObj cond = eval(ife.condition, env);
        if (isError(cond)) return cond;

        if (isTruthy(cond)) return eval(ife.consequence, env);

        for (ElseIfBranch branch : ife.alternatives) {
            LObj altCond = eval(branch.condition, env);
            if (isError(altCond)) return altCond;
            if (isTruthy(altCond)) return eval(branch.block, env);
        }

        if (ife.elseBlock != null) return eval(ife.elseBlock, env);
        return NULL;
    }

    // ── Llamadas a función ────────────────────────────────────────────────────

    private LObj evalCallExpression(CallExpression ce, Environment env) {
        LObj fn = eval(ce.function, env);
        if (isError(fn)) return fn;

        List<LObj> args = evalExpressions(ce.arguments, env);
        if (args.size() == 1 && isError(args.get(0))) return args.get(0);

        return applyFunction(fn, args);
    }

    private List<LObj> evalExpressions(List<AST.Expression> exprs, Environment env) {
        List<LObj> results = new ArrayList<>();
        for (AST.Expression expr : exprs) {
            LObj val = eval(expr, env);
            if (isError(val)) return List.of(val);
            results.add(val);
        }
        return results;
    }

    private LObj applyFunction(LObj fn, List<LObj> args) {
        if (!(fn instanceof LFunction func)) {
            return newError("No es una función: " + fn.type());
        }
        if (args.size() != func.parameters().size()) {
            return newError("Cantidad incorrecta de argumentos: se esperaban "
                    + func.parameters().size() + ", se recibieron " + args.size());
        }

        // Entorno cerrado sobre el entorno de DEFINICIÓN (closure / lexical scope)
        Environment fnEnv = Environment.newEnclosed(func.env());
        for (int i = 0; i < func.parameters().size(); i++) {
            fnEnv.set(func.parameters().get(i).value, args.get(i));
        }

        LObj result = evalBlockStatement(func.body(), fnEnv);
        if (result instanceof LReturnValue rv) return rv.value();
        return result;
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    private boolean isTruthy(LObj obj) {
        if (obj == NULL || obj == FALSE)    return false;
        if (obj instanceof LBoolean lb)     return lb.value();
        if (obj instanceof LInteger li)     return li.value() != 0;
        if (obj instanceof LFloat lf)       return lf.value() != 0.0;
        return true;
    }

    private boolean isError(LObj obj) {
        return obj instanceof LError;
    }

    private LError newError(String msg) {
        return new LError(msg);
    }

    private LObj nativeBool(boolean value) {
        return value ? TRUE : FALSE;
    }

    private double toDouble(LObj obj) {
        if (obj instanceof LFloat lf)   return lf.value();
        if (obj instanceof LInteger li) return (double) li.value();
        return 0.0;
    }
}

