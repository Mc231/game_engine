package engine;

import org.joml.Vector3f;

/**
 * Static guard line-of-sight helper: a target is visible when it lies within a
 * guard's view cone and range and no {@link AABB} blocker occludes the straight
 * path between the eye and the target. Pure geometry, no OpenGL.
 */
public final class Vision {

    private Vision() {
    }

    /**
     * Tests whether {@code target} is visible from {@code eye}.
     *
     * @param eye        the guard's eye position
     * @param forward    the guard's facing direction (need not be normalized)
     * @param fovCosHalf the cosine of the half field-of-view angle; the target must
     *                   lie within this cone around {@code forward}
     * @param range      the maximum sight distance in world units
     * @param target     the point being looked at
     * @param blockers   walls that occlude sight; {@code null}/empty entries are skipped
     * @return {@code true} if the target is in range, inside the cone, and unoccluded
     */
    public static boolean canSee(Vector3f eye, Vector3f forward, float fovCosHalf,
                                 float range, Vector3f target, AABB[] blockers) {
        Vector3f to = new Vector3f(target).sub(eye);
        float dist = to.length();
        if (dist > range) {
            return false;
        }
        if (dist < 1e-4f) {
            return true;   // target sits on the eye: trivially visible
        }

        Vector3f dir = new Vector3f(to).div(dist);
        if (dir.dot(new Vector3f(forward).normalize()) < fovCosHalf) {
            return false;  // outside the view cone
        }

        Ray ray = new Ray(eye, dir);
        if (blockers != null) {
            for (AABB box : blockers) {
                if (box == null) {
                    continue;
                }
                float t = Intersect.rayAABB(ray, box);
                if (t >= 0 && t < dist - 0.05f) {
                    return false;   // a wall stands between eye and target
                }
            }
        }
        return true;
    }
}
