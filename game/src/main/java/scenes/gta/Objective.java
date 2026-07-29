package scenes.gta;

import org.joml.Vector3f;

/** A single mission step: reach {@code pos} within {@code radius}. */
public class Objective {

    public final Vector3f pos;
    public final float radius;
    public final String label;

    public Objective(float x, float z, float radius, String label) {
        this.pos = new Vector3f(x, 0f, z);
        this.radius = radius;
        this.label = label;
    }

    public boolean reached(Vector3f p) {
        float dx = p.x - pos.x, dz = p.z - pos.z;
        return dx * dx + dz * dz <= radius * radius;
    }
}
