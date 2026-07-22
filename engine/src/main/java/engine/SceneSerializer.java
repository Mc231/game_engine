package engine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reads and writes {@link SceneData} as JSON, so levels can be authored as data.
 *
 * <p>The flat {@link ComponentData} model means Gson needs no custom adapters.
 */
public final class SceneSerializer {

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson PLAIN = new Gson();

    private SceneSerializer() {
    }

    /** Serializes a scene to pretty-printed JSON. */
    public static String toJson(SceneData scene) {
        return PRETTY.toJson(scene);
    }

    /** Parses a scene from JSON text. */
    public static SceneData fromJson(String json) {
        return PLAIN.fromJson(json, SceneData.class);
    }

    /** Writes {@link #toJson(SceneData)} to {@code path} as UTF-8. */
    public static void saveToFile(SceneData scene, Path path) {
        try {
            Files.writeString(path, toJson(scene), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to save scene to " + path, e);
        }
    }

    /** Loads a scene from a classpath resource, throwing if it is missing. */
    public static SceneData loadFromResource(String classpathPath) {
        try (InputStream in = SceneSerializer.class.getClassLoader().getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new RuntimeException("Scene resource not found on classpath: " + classpathPath);
            }
            return fromJson(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load scene resource " + classpathPath, e);
        }
    }
}
