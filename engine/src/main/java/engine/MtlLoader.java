package engine;

import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A minimal Wavefront .MTL (material library) parser. Reads {@code newmtl},
 * {@code Kd} (diffuse color), {@code Ns} (shininess), and {@code map_Kd}
 * (diffuse texture) directives; all other lines are ignored.
 *
 * Parsing is pure text-in / data-out so it can be unit-tested without an
 * OpenGL context, mirroring {@link OBJLoader}.
 */
public final class MtlLoader {

    /** A single material definition. {@code diffuseTexture} may be null. */
    public record MaterialDef(String name, Vector3f diffuse, float shininess, String diffuseTexture) {
    }

    private MtlLoader() {
    }

    /**
     * Parse .mtl text into a map from material name to {@link MaterialDef},
     * preserving insertion order. Defaults per material: diffuse = (1,1,1),
     * shininess = 32, diffuseTexture = null. An {@code Ns} value {@code <= 0}
     * is clamped to 1.
     */
    public static Map<String, MaterialDef> parse(String mtlText) {
        Map<String, MaterialDef> materials = new LinkedHashMap<>();

        String name = null;
        Vector3f diffuse = new Vector3f(1f, 1f, 1f);
        float shininess = 32f;
        String diffuseTexture = null;

        for (String raw : mtlText.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "newmtl" -> {
                    // Flush the material in progress before starting a new one.
                    if (name != null) {
                        materials.put(name, new MaterialDef(name, diffuse, shininess, diffuseTexture));
                    }
                    name = parts.length > 1 ? parts[1] : null;
                    diffuse = new Vector3f(1f, 1f, 1f);
                    shininess = 32f;
                    diffuseTexture = null;
                }
                case "Kd" -> diffuse = new Vector3f(
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
                case "Ns" -> {
                    float ns = Float.parseFloat(parts[1]);
                    shininess = ns <= 0 ? 1f : ns;
                }
                case "map_Kd" -> diffuseTexture = parts[parts.length - 1];
                default -> { /* ignore unsupported lines */ }
            }
        }

        if (name != null) {
            materials.put(name, new MaterialDef(name, diffuse, shininess, diffuseTexture));
        }
        return materials;
    }
}
