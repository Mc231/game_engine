package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GeometryTest {

    @Test
    void cubeHas36VerticesOfEightFloats() {
        float[] cube = Geometry.cubeWithNormalsAndUV();
        assertEquals(36 * 8, cube.length);
    }

    @Test
    void planeHasSixVerticesFacingUp() {
        float[] plane = Geometry.plane(10f, 5f);
        assertEquals(6 * 8, plane.length);
        // Each vertex's normal (floats 3..5 of the 8-float stride) is (0,1,0).
        for (int i = 0; i < plane.length; i += 8) {
            assertEquals(0f, plane[i + 3], 0f);
            assertEquals(1f, plane[i + 4], 0f);
            assertEquals(0f, plane[i + 5], 0f);
        }
    }

    @Test
    void planeSpansTheGivenHalfExtent() {
        float half = 10f;
        float[] plane = Geometry.plane(half, 1f);
        float maxAbsX = 0f;
        float maxAbsZ = 0f;
        for (int i = 0; i < plane.length; i += 8) {
            maxAbsX = Math.max(maxAbsX, Math.abs(plane[i]));
            maxAbsZ = Math.max(maxAbsZ, Math.abs(plane[i + 2]));
        }
        assertEquals(half, maxAbsX, 0f);
        assertEquals(half, maxAbsZ, 0f);
    }
}
