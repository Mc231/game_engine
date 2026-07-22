package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CollideTest {

    private static final float EPS = 1e-4f;

    // A tall wall centered at (5,1,0), spanning x in [4,6], z in [-1,1].
    private static AABB[] wall() {
        return new AABB[]{AABB.fromCenterSize(new Vector3f(5, 1, 0), new Vector3f(2, 3, 2))};
    }

    @Test
    void blockedHeadOnRevertsX() {
        Vector3f pos = new Vector3f(3, 1.7f, 0);
        // Moving to x=6 would overlap the wall, so X is reverted; Z is untouched.
        Collide.slideXZ(pos, 0.4f, 3f, 0f, wall());
        assertEquals(3f, pos.x, EPS);
        assertEquals(0f, pos.z, EPS);
    }

    @Test
    void slidesAlongWall() {
        Vector3f pos = new Vector3f(3.5f, 1.7f, 0);
        // X to 4.5 gives body max 4.9 > 4, so X reverts; Z is clear and advances.
        Collide.slideXZ(pos, 0.4f, 1f, 2f, wall());
        assertEquals(3.5f, pos.x, EPS);
        assertEquals(2f, pos.z, EPS);
    }

    @Test
    void noWallsMovesFreely() {
        Vector3f pos = new Vector3f(0, 1.7f, 0);
        Collide.slideXZ(pos, 0.4f, 1f, 1f, new AABB[0]);
        assertEquals(1f, pos.x, EPS);
        assertEquals(1f, pos.z, EPS);
    }
}
