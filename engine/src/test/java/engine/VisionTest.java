package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisionTest {

    private static final Vector3f EYE = new Vector3f(0, 0, 0);
    private static final Vector3f FORWARD = new Vector3f(0, 0, 1);
    private static final float FOV_COS_HALF = (float) Math.cos(Math.toRadians(45));
    private static final float RANGE = 20f;

    private static boolean canSee(Vector3f target, AABB[] blockers) {
        return Vision.canSee(EYE, FORWARD, FOV_COS_HALF, RANGE, target, blockers);
    }

    @Test
    void targetAheadInRangeIsVisible() {
        assertTrue(canSee(new Vector3f(0, 0, 10), null));
    }

    @Test
    void targetBehindIsNotVisible() {
        assertFalse(canSee(new Vector3f(0, 0, -10), null));
    }

    @Test
    void targetBeyondRangeIsNotVisible() {
        assertFalse(canSee(new Vector3f(0, 0, 30), null));
    }

    @Test
    void targetOutsideConeIsNotVisible() {
        // (12,0,10) is ~50 deg off forward, beyond the 45 deg half-FOV.
        assertFalse(canSee(new Vector3f(12, 0, 10), null));
    }

    @Test
    void wallBetweenEyeAndTargetBlocksSight() {
        AABB wall = AABB.fromCenterSize(new Vector3f(0, 0, 5), new Vector3f(4, 4, 1));
        assertFalse(canSee(new Vector3f(0, 0, 10), new AABB[]{wall}));
    }

    @Test
    void targetInFrontOfWallIsVisible() {
        AABB wall = AABB.fromCenterSize(new Vector3f(0, 0, 5), new Vector3f(4, 4, 1));
        assertTrue(canSee(new Vector3f(0, 0, 3), new AABB[]{wall}));
    }
}
