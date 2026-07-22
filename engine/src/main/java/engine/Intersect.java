package engine;

import org.joml.Vector3f;

/**
 * Static ray/box/plane intersection helpers. Distances are returned as the ray
 * parameter {@code t} (world units, since ray directions are normalized); a
 * negative result means "no forward intersection".
 */
public final class Intersect {

    private Intersect() {
    }

    /**
     * Ray vs. AABB using the slab method. Returns the nearest non-negative hit
     * distance {@code t}, or {@code -1} if the ray never enters the box in front
     * of its origin. A ray starting inside the box returns {@code 0}. Rays
     * parallel to a slab are handled via the inverse-direction sign trick, which
     * yields signed infinities that behave correctly under min/max.
     */
    public static float rayAABB(Ray ray, AABB box) {
        float invX = 1.0f / ray.direction.x;
        float invY = 1.0f / ray.direction.y;
        float invZ = 1.0f / ray.direction.z;

        float t1 = (box.min.x - ray.origin.x) * invX;
        float t2 = (box.max.x - ray.origin.x) * invX;
        float t3 = (box.min.y - ray.origin.y) * invY;
        float t4 = (box.max.y - ray.origin.y) * invY;
        float t5 = (box.min.z - ray.origin.z) * invZ;
        float t6 = (box.max.z - ray.origin.z) * invZ;

        float tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        // tmax < 0: box is entirely behind the ray. tmin > tmax: the ray misses.
        if (tmax < 0 || tmin > tmax) {
            return -1f;
        }
        // tmin < 0 means the origin is inside the box: the first forward hit is now.
        return tmin < 0 ? 0f : tmin;
    }

    /** AABB vs. AABB overlap test; delegates to {@link AABB#intersects(AABB)}. */
    public static boolean aabbAABB(AABB a, AABB b) {
        return a.intersects(b);
    }

    /**
     * Ray vs. infinite plane defined by a point and a normal. Returns {@code t >= 0}
     * of the intersection, or {@code -1} if the ray is parallel to the plane or the
     * plane lies behind the origin.
     */
    public static float rayPlane(Ray ray, Vector3f planePoint, Vector3f planeNormal) {
        float denom = planeNormal.dot(ray.direction);
        if (Math.abs(denom) < 1e-6f) {
            return -1f;   // parallel: no single intersection
        }
        float t = new Vector3f(planePoint).sub(ray.origin).dot(planeNormal) / denom;
        return t < 0 ? -1f : t;
    }
}
