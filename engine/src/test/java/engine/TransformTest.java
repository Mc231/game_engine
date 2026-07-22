package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformTest {

    private static final float EPS = 1e-5f;

    @Test
    void defaultIsIdentity() {
        Transform t = new Transform();
        Vector3f p = t.matrix().transformPosition(new Vector3f(1, 2, 3));
        assertVec(1, 2, 3, p);
    }

    @Test
    void translationMovesTheOrigin() {
        Transform t = new Transform().setPosition(5, -2, 3);
        Vector3f p = t.matrix().transformPosition(new Vector3f(0, 0, 0));
        assertVec(5, -2, 3, p);
    }

    @Test
    void uniformScaleScalesPoints() {
        Transform t = new Transform().setScale(2f);
        Vector3f p = t.matrix().transformPosition(new Vector3f(1, 1, 1));
        assertVec(2, 2, 2, p);
    }

    @Test
    void scaleThenTranslateAppliesScaleFirst() {
        // translationRotateScale = translate * rotate * scale → scale is applied first.
        Transform t = new Transform().setPosition(1, 0, 0).setScale(2f);
        Vector3f p = t.matrix().transformPosition(new Vector3f(1, 0, 0));
        assertVec(3, 0, 0, p);  // (1 * 2) + 1
    }

    @Test
    void rotationAroundYByNinetyDegrees() {
        Transform t = new Transform().setRotationEuler(0f, (float) Math.toRadians(90), 0f);
        Vector3f p = t.matrix().transformPosition(new Vector3f(1, 0, 0));
        // +X rotated 90° about +Y → -Z.
        assertVec(0, 0, -1, p);
    }

    private static void assertVec(float x, float y, float z, Vector3f v) {
        assertEquals(x, v.x, EPS);
        assertEquals(y, v.y, EPS);
        assertEquals(z, v.z, EPS);
    }
}
