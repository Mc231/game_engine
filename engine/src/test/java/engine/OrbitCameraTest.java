package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-math tests for {@link OrbitCamera}: the camera-relative movement basis and
 * the wall pull-in. No GL context needed.
 */
class OrbitCameraTest {

    private static final Vector3f ORIGIN = new Vector3f(0f, 0f, 0f);

    @Test
    void forwardAndRightAreUnitAndPerpendicular() {
        OrbitCamera cam = new OrbitCamera();
        cam.addLook(500f, 0f);   // rotate yaw off zero
        Vector3f f = new Vector3f(cam.forwardXZ());
        Vector3f r = new Vector3f(cam.rightXZ());
        assertEquals(1f, f.length(), 1e-4f);
        assertEquals(1f, r.length(), 1e-4f);
        assertEquals(0f, f.dot(r), 1e-4f);   // perpendicular
        assertEquals(0f, f.y, 1e-6f);        // stays on XZ plane
        assertEquals(0f, r.y, 1e-6f);
    }

    @Test
    void atZeroYawForwardPointsAlongMinusZ() {
        // yaw 0: camera sits behind at +Z, so "forward" (into screen) is -Z.
        OrbitCamera cam = new OrbitCamera();
        Vector3f f = cam.forwardXZ();
        assertEquals(0f, f.x, 1e-5f);
        assertEquals(-1f, f.z, 1e-5f);
    }

    @Test
    void cameraPullsInWhenWallBlocksTheView() {
        OrbitCamera cam = new OrbitCamera().setDistance(6.5f).setPitch(0.2f).setTargetHeight(1.4f);
        // A wall just behind the target along +Z (where the camera wants to sit).
        AABB wall = AABB.fromCenterSize(new Vector3f(0f, 1.4f, 3f), new Vector3f(6f, 6f, 0.5f));
        cam.update(ORIGIN, 1f, new AABB[]{wall});
        float lookY = 1.4f;
        float dist = cam.position().distance(new Vector3f(0f, lookY, 0f));
        assertTrue(dist < 6.5f, "camera should be pulled closer than the orbit distance, was " + dist);
        assertTrue(dist > 1f, "camera should not collapse onto the target, was " + dist);
    }

    @Test
    void withoutWallsCameraSitsAtFullDistance() {
        OrbitCamera cam = new OrbitCamera().setDistance(6.5f).setPitch(0.3f).setTargetHeight(1.4f);
        cam.update(ORIGIN, 1f, null);
        float dist = cam.position().distance(new Vector3f(0f, 1.4f, 0f));
        assertEquals(6.5f, dist, 1e-3f);
    }
}
