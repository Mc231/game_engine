package engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;

/**
 * Loads a {@link ShaderProgram} from vertex/fragment source files on the
 * filesystem and rebuilds it when either file changes on disk, enabling live
 * shader editing during development. Rebuilds never crash a running game: on a
 * compile/link or IO failure the previously working program is kept and the
 * error is logged.
 */
public class ShaderReloader implements Disposable {

    private final Path vertexPath;
    private final Path fragmentPath;

    private ShaderProgram program;
    private FileTime vertexModified;
    private FileTime fragmentModified;

    /**
     * Read both files (UTF-8) and build the initial program.
     *
     * @throws RuntimeException if a file cannot be read, or the shader fails to
     *                          compile/link
     */
    public ShaderReloader(String vertexPath, String fragmentPath) {
        this.vertexPath = Paths.get(vertexPath);
        this.fragmentPath = Paths.get(fragmentPath);

        this.program = build();
        this.vertexModified = lastModified(this.vertexPath);
        this.fragmentModified = lastModified(this.fragmentPath);
    }

    /** The current program; never null after construction. */
    public ShaderProgram get() {
        return program;
    }

    /**
     * Rebuild only if either file's last-modified time changed since the last
     * load. Timestamps are refreshed even on failure so a broken file is not
     * retried every frame.
     *
     * @return true if the program was successfully swapped, false otherwise
     */
    public boolean reloadIfChanged() {
        FileTime v = lastModified(vertexPath);
        FileTime f = lastModified(fragmentPath);
        boolean changed = !equalTimes(v, vertexModified) || !equalTimes(f, fragmentModified);
        if (!changed) {
            return false;
        }
        vertexModified = v;
        fragmentModified = f;
        return rebuild();
    }

    /**
     * Force a rebuild regardless of timestamps.
     *
     * @return true if the program was successfully swapped, false otherwise
     */
    public boolean reload() {
        vertexModified = lastModified(vertexPath);
        fragmentModified = lastModified(fragmentPath);
        return rebuild();
    }

    /** Dispose the current program. */
    @Override
    public void dispose() {
        program.dispose();
    }

    /**
     * Attempt to build a new program; on success dispose the old one and swap.
     * On any failure keep the old program, log, and return false.
     */
    private boolean rebuild() {
        ShaderProgram rebuilt;
        try {
            rebuilt = build();
        } catch (RuntimeException e) {
            Log.error("Shader reload failed for " + vertexPath + " / " + fragmentPath
                    + ": " + e.getMessage());
            return false;
        }
        program.dispose();
        program = rebuilt;
        return true;
    }

    /** Read both sources and construct a program (may throw). */
    private ShaderProgram build() {
        String vertexSource = readString(vertexPath);
        String fragmentSource = readString(fragmentPath);
        return new ShaderProgram(vertexSource, fragmentSource);
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read shader file: " + path, e);
        }
    }

    /** Last-modified time, or null if it cannot be read. */
    private static FileTime lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return null;
        }
    }

    private static boolean equalTimes(FileTime a, FileTime b) {
        return a == null ? b == null : a.equals(b);
    }
}
