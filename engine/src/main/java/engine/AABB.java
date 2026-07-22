package engine;

import org.joml.Vector3f;

/**
 * An axis-aligned bounding box (AABB) defined by its {@code min} and {@code max}
 * corners. Instances are immutable: the corner vectors are stored as copies, so
 * callers may safely reuse the vectors they pass in.
 */
public class AABB {

    public final Vector3f min;
    public final Vector3f max;

    /** Builds a box from its two extreme corners (copies are stored). */
    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
    }

    /** Builds a box from a center and a full {@code size}; each side extends half the size. */
    public static AABB fromCenterSize(Vector3f center, Vector3f size) {
        Vector3f half = new Vector3f(size).mul(0.5f);
        return new AABB(new Vector3f(center).sub(half), new Vector3f(center).add(half));
    }

    /** Standard 3-axis overlap test; boxes that merely touch on a face count as intersecting. */
    public boolean intersects(AABB other) {
        return min.x <= other.max.x && max.x >= other.min.x
                && min.y <= other.max.y && max.y >= other.min.y
                && min.z <= other.max.z && max.z >= other.min.z;
    }

    /** True if point {@code p} lies within this box (inclusive of the faces). */
    public boolean contains(Vector3f p) {
        return p.x >= min.x && p.x <= max.x
                && p.y >= min.y && p.y <= max.y
                && p.z >= min.z && p.z <= max.z;
    }

    /** The center of the box (new vector). */
    public Vector3f center() {
        return new Vector3f(min).add(max).mul(0.5f);
    }

    /** The full size of the box, {@code max - min} (new vector). */
    public Vector3f size() {
        return new Vector3f(max).sub(min);
    }
}
