package engine;

/**
 * A minimal leveled logger with no external dependencies. info/warn go to
 * standard out, error to standard error, each line prefixed with its level.
 */
public final class Log {

    private Log() {
    }

    public static void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public static void warn(String message) {
        System.out.println("[WARN] " + message);
    }

    public static void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}
