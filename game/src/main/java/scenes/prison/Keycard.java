package scenes.prison;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/** A collectible keycard that floats and spins; picked up when the player is near and presses use. */
public class Keycard {

    public final String id;
    public final Vector3f position;
    public final Vector3f color;
    public boolean collected;

    public Keycard(String id, float x, float y, float z, Vector3f color) {
        this.id = id;
        this.position = new Vector3f(x, y, z);
        this.color = color;
    }

    public boolean inReach(Vector3f playerPos, float radius) {
        if (collected) {
            return false;
        }
        float dx = playerPos.x - position.x;
        float dz = playerPos.z - position.z;
        return dx * dx + dz * dz < radius * radius;
    }

    public Matrix4f model(Matrix4f dest, float spin) {
        return dest.identity().translate(position).rotateY(spin).scale(0.4f, 0.06f, 0.26f);
    }
}
