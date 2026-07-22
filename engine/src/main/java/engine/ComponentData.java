package engine;

/**
 * A flat, JSON-friendly description of a single entity component.
 *
 * <p>Fields are nullable and discriminated by {@code type} so Gson can (de)serialize
 * without polymorphic adapters:
 * <ul>
 *   <li>{@code type="mesh"}: {@code mesh} is a geometry key ("cube") or an .obj path,
 *       {@code texture} is a classpath texture path, {@code tint} is rgb
 *       (nullable, consumer defaults to white), and {@code shininess} is the specular exponent.</li>
 *   <li>{@code type="light"}: {@code lightType} is "point"/"directional"/"spot" and
 *       {@code color} is rgb.</li>
 * </ul>
 */
public record ComponentData(String type, String mesh, String texture, float[] tint,
                            float shininess, String lightType, float[] color) {
}
