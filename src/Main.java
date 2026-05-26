import repl.Repl;

public class Main {
    public static void main(String[] args) {
        if (args.length == 1) {
            Repl.runFile(args[0]);
        } else {
            Repl.start();
        }
    }
}
