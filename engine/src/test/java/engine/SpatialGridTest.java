package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpatialGridTest {

    private static AABB box(float cx, float cz, float half) {
        return AABB.fromCenterSize(new Vector3f(cx, 2f, cz), new Vector3f(half * 2f, 4f, half * 2f));
    }

    private static boolean contains(AABB[] arr, AABB b) {
        return Arrays.asList(arr).contains(b);
    }

    @Test
    void nearbyReturnsCloseBoxAndExcludesFarBox() {
        SpatialGrid grid = new SpatialGrid(10f);
        AABB near = box(5f, 5f, 1f);
        AABB far = box(200f, 200f, 1f);
        grid.insert(near);
        grid.insert(far);

        AABB[] hits = grid.nearby(6f, 6f, 3f);
        assertTrue(contains(hits, near), "near box should be returned");
        assertFalse(contains(hits, far), "far box should be excluded");
    }

    @Test
    void nearbyDeduplicatesBoxesSpanningManyCells() {
        SpatialGrid grid = new SpatialGrid(4f);
        AABB big = box(0f, 0f, 20f);   // spans many 4-unit cells
        grid.insert(big);
        AABB[] hits = grid.nearby(0f, 0f, 8f);
        assertEquals(1, hits.length, "a box spanning multiple cells must appear once");
    }

    @Test
    void insertAllAndEmptyRegion() {
        SpatialGrid grid = new SpatialGrid(8f);
        grid.insertAll(List.of(box(0f, 0f, 1f), box(40f, 0f, 1f)));
        assertEquals(0, grid.nearby(500f, 500f, 5f).length, "empty region returns nothing");
        assertEquals(1, grid.nearby(0f, 0f, 2f).length);
    }
}
