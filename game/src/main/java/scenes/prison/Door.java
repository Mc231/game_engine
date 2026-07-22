package scenes.prison;

import engine.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

/**
 * A door that blocks movement and sight while closed. Opens on use if the
 * player holds {@link #requiredKey} (null = no key needed). When open it stops
 * being a collider and isn't drawn.
 */
public class Door {

    public final String requiredKey;   // null = unlocked
    public boolean open;
    public final Vector3f center;
    public final Vector3f size;
    private final AABB aabb;

    public Door(String requiredKey, float cx, float cy, float cz, float sx, float sy, float sz) {
        this.requiredKey = requiredKey;
        this.center = new Vector3f(cx, cy, cz);
        this.size = new Vector3f(sx, sy, sz);
        this.aabb = AABB.fromCenterSize(center, size);
    }

    public AABB aabb() {
        return aabb;
    }

    public boolean inReach(Vector3f playerPos, float radius) {
        float dx = playerPos.x - center.x;
        float dz = playerPos.z - center.z;
        return dx * dx + dz * dz < radius * radius;
    }

    /** Try to open with the given inventory; returns true if it just opened. */
    public boolean tryOpen(Set<String> inventory) {
        if (open) {
            return false;
        }
        if (requiredKey == null || inventory.contains(requiredKey)) {
            open = true;
            return true;
        }
        return false;
    }

    public Matrix4f model(Matrix4f dest) {
        return dest.identity().translate(center).scale(size.x, size.y, size.z);
    }
}
