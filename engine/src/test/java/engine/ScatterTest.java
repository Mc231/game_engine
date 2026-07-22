package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScatterTest {

    private static final float EPS = 1e-5f;
    private static final Scatter.HeightSampler FLAT = (x, z) -> 0f;

    @Test
    void fillsAreaWhenNothingExcluded() {
        Matrix4f[] result = Scatter.onArea(50, 100f, 1L, 1f, 1f, FLAT, null);

        assertEquals(50, result.length);
        for (Matrix4f m : result) {
            Vector3f t = m.getTranslation(new Vector3f());
            assertTrue(t.x >= -100f && t.x <= 100f, "x within bounds: " + t.x);
            assertTrue(t.z >= -100f && t.z <= 100f, "z within bounds: " + t.z);
            assertEquals(0f, t.y, EPS);
        }
    }

    @Test
    void sameSeedIsDeterministic() {
        Vector3f a = Scatter.onArea(10, 100f, 7L, 1f, 1f, FLAT, null)[0].getTranslation(new Vector3f());
        Vector3f b = Scatter.onArea(10, 100f, 7L, 1f, 1f, FLAT, null)[0].getTranslation(new Vector3f());
        assertEquals(a.x, b.x, EPS);
        assertEquals(a.z, b.z, EPS);
    }

    @Test
    void differentSeedDiffers() {
        Vector3f a = Scatter.onArea(10, 100f, 1L, 1f, 1f, FLAT, null)[0].getTranslation(new Vector3f());
        Vector3f b = Scatter.onArea(10, 100f, 2L, 1f, 1f, FLAT, null)[0].getTranslation(new Vector3f());
        assertNotEquals(a.x, b.x, EPS);
    }

    @Test
    void excludeRegionIsNeverPlaced() {
        // Exclude the whole right half (x > 0): every placement must land on x <= 0.
        Scatter.Exclude rightHalf = (x, z) -> x > 0f;
        Matrix4f[] result = Scatter.onArea(50, 100f, 3L, 1f, 1f, FLAT, rightHalf);

        for (Matrix4f m : result) {
            Vector3f t = m.getTranslation(new Vector3f());
            assertTrue(t.x <= 0f, "no placement in excluded region, got x=" + t.x);
        }
    }

    @Test
    void scaleStaysWithinRange() {
        Matrix4f[] result = Scatter.onArea(30, 50f, 5L, 0.5f, 2f, FLAT, null);
        for (Matrix4f m : result) {
            float s = m.getScale(new Vector3f()).x;
            assertTrue(s >= 0.5f - EPS && s <= 2f + EPS, "scale within range: " + s);
        }
    }
}
