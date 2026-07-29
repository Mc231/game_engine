package scenes.gta;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Minimal persistence for money + completed missions, stored as a properties file
 * in the working directory. Never throws — a missing/broken save just yields
 * defaults (money 0, nothing completed), matching the engine's forgiving
 * settings-loading style.
 */
public final class SaveGame {

    private static final String FILE = "gta-save.properties";

    /** Loaded snapshot. */
    public static final class State {
        public final int money;
        public final Set<String> completed;

        State(int money, Set<String> completed) {
            this.money = money;
            this.completed = completed;
        }
    }

    private SaveGame() {
    }

    public static State load() {
        Properties p = new Properties();
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (Exception ignored) {
                // fall through to defaults
            }
        }
        int money = parseInt(p.getProperty("money"), 0);
        String done = p.getProperty("completed", "");
        Set<String> completed = done.isBlank()
                ? new HashSet<>()
                : Arrays.stream(done.split(";")).map(String::trim).filter(s -> !s.isEmpty())
                        .collect(Collectors.toCollection(HashSet::new));
        return new State(money, completed);
    }

    public static void save(int money, Set<String> completed) {
        Properties p = new Properties();
        p.setProperty("money", Integer.toString(money));
        p.setProperty("completed", String.join(";", completed));
        try (FileOutputStream out = new FileOutputStream(FILE)) {
            p.store(out, "Grand Theft LWJGL save");
        } catch (Exception ignored) {
            // best-effort: a failed save shouldn't crash the game
        }
    }

    private static int parseInt(String s, int fallback) {
        try {
            return s == null ? fallback : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
