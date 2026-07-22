package engine;

import org.joml.Vector3f;

/**
 * Axis-separated AABB collision for a first-person mover. Resolving the X and Z
 * axes independently lets the mover slide along a wall instead of sticking to it.
 */
public final class Collide {

    private Collide() {
    }

    /**
     * Moves {@code pos} on the XZ plane by {@code (dx, dz)}, resolving each axis
     * separately against {@code walls} so the mover slides along surfaces. The
     * mover's body is the AABB with min {@code (x - radius, y - 1.6, z - radius)}
     * and max {@code (x + radius, y + 0.2, z + radius)}. {@code pos.y} is left
     * unchanged. If a moved axis makes the body intersect any wall, that axis is
     * reverted. A null or empty {@code walls} array applies the full motion.
     */
    public static void slideXZ(Vector3f pos, float radius, float dx, float dz, AABB[] walls) {
        if (walls == null || walls.length == 0) {
            pos.x += dx;
            pos.z += dz;
            return;
        }

        pos.x += dx;
        if (hitsAny(pos, radius, walls)) {
            pos.x -= dx;
        }

        pos.z += dz;
        if (hitsAny(pos, radius, walls)) {
            pos.z -= dz;
        }
    }

    /** True if the mover body at {@code pos} intersects any wall in {@code walls}. */
    private static boolean hitsAny(Vector3f pos, float radius, AABB[] walls) {
        AABB body = new AABB(
                new Vector3f(pos.x - radius, pos.y - 1.6f, pos.z - radius),
                new Vector3f(pos.x + radius, pos.y + 0.2f, pos.z + radius));
        for (AABB wall : walls) {
            if (body.intersects(wall)) {
                return true;
            }
        }
        return false;
    }
}
