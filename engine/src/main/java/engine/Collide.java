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

    /**
     * Pushes a circle of {@code radius} centered at {@code pos} (XZ plane) out of
     * any overlapping {@code walls}, resolving penetration so the mover ends up
     * just touching each wall's edge. Y is ignored (walls are treated as infinite
     * vertical prisms). Unlike {@link #slideXZ} this is displacement-based, so it
     * works for a mover whose motion was already integrated elsewhere (e.g. a
     * {@link CarController}); tangential motion is preserved (it slides).
     *
     * @return true if any correction was applied.
     */
    public static boolean resolveCircle(Vector3f pos, float radius, AABB[] walls) {
        if (walls == null || walls.length == 0) {
            return false;
        }
        boolean moved = false;
        for (AABB wall : walls) {
            float closestX = clamp(pos.x, wall.min.x, wall.max.x);
            float closestZ = clamp(pos.z, wall.min.z, wall.max.z);
            float dx = pos.x - closestX;
            float dz = pos.z - closestZ;
            float d2 = dx * dx + dz * dz;

            if (d2 > 1e-8f) {
                // Center is outside the box: push out along the nearest-point normal.
                if (d2 < radius * radius) {
                    float d = (float) Math.sqrt(d2);
                    float push = radius - d;
                    pos.x += dx / d * push;
                    pos.z += dz / d * push;
                    moved = true;
                }
            } else {
                // Center is inside the box: eject along the axis of least penetration.
                float toMinX = pos.x - wall.min.x, toMaxX = wall.max.x - pos.x;
                float toMinZ = pos.z - wall.min.z, toMaxZ = wall.max.z - pos.z;
                float penX = Math.min(toMinX, toMaxX);
                float penZ = Math.min(toMinZ, toMaxZ);
                if (penX < penZ) {
                    pos.x += (toMinX < toMaxX ? -(penX + radius) : (penX + radius));
                } else {
                    pos.z += (toMinZ < toMaxZ ? -(penZ + radius) : (penZ + radius));
                }
                moved = true;
            }
        }
        return moved;
    }

    private static float clamp(float v, float lo, float hi) {
        return v < lo ? lo : (v > hi ? hi : v);
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
