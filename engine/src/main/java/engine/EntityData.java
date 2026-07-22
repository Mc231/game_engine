package engine;

import java.util.List;

/**
 * A JSON-friendly description of a scene entity: a named transform plus its components.
 *
 * <p>{@code position}, {@code rotation} (euler radians) and {@code scale} are each 3-float
 * arrays. They are nullable; the consumer applies the defaults (position/rotation
 * {@code (0,0,0)}, scale {@code (1,1,1)}) rather than this record.
 */
public record EntityData(String name, float[] position, float[] rotation, float[] scale,
                         List<ComponentData> components) {
}
