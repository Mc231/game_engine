package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollisionTest {

    private static final float EPS = 1e-4f;

    // A unit box centered at (0,0,-5): spans z in [-6, -4].
    private static AABB boxInFront() {
        return AABB.fromCenterSize(new Vector3f(0, 0, -5), new Vector3f(2, 2, 2));
    }

    @Test
    void rayHitsBoxInFrontAtGapDistance() {
        Ray ray = new Ray(new Vector3f(0, 0, 0), new Vector3f(0, 0, -1));
        float t = Intersect.rayAABB(ray, boxInFront());
        // Near face of the box is at z = -4, so the gap is 4 units.
        assertEquals(4f, t, EPS);
        assertTrue(t >= 0);
    }

    @Test
    void rayPointingAwayMisses() {
        Ray ray = new Ray(new Vector3f(0, 0, 0), new Vector3f(0, 0, 1));
        assertTrue(Intersect.rayAABB(ray, boxInFront()) < 0);
    }

    @Test
    void offsetRayMisses() {
        // Aimed forward but far to the side of the box.
        Ray ray = new Ray(new Vector3f(10, 0, 0), new Vector3f(0, 0, -1));
        assertTrue(Intersect.rayAABB(ray, boxInFront()) < 0);
    }

    @Test
    void rayStartingInsideBoxReturnsZero() {
        AABB box = AABB.fromCenterSize(new Vector3f(0, 0, 0), new Vector3f(2, 2, 2));
        Ray ray = new Ray(new Vector3f(0, 0, 0), new Vector3f(1, 0, 0));
        float t = Intersect.rayAABB(ray, box);
        assertTrue(t >= 0, "ray from inside must not miss");
        assertEquals(0f, t, EPS);
    }

    @Test
    void rayParallelToSlabButAlignedStillHits() {
        // Direction has zero z-component; ray runs along +x through the box.
        AABB box = AABB.fromCenterSize(new Vector3f(5, 0, 0), new Vector3f(2, 2, 2));
        Ray ray = new Ray(new Vector3f(0, 0, 0), new Vector3f(1, 0, 0));
        assertEquals(4f, Intersect.rayAABB(ray, box), EPS);
    }

    @Test
    void overlappingBoxesIntersect() {
        AABB a = AABB.fromCenterSize(new Vector3f(0, 0, 0), new Vector3f(2, 2, 2));
        AABB b = AABB.fromCenterSize(new Vector3f(1, 1, 1), new Vector3f(2, 2, 2));
        assertTrue(a.intersects(b));
        assertTrue(Intersect.aabbAABB(a, b));
    }

    @Test
    void separatedBoxesDoNotIntersect() {
        AABB a = AABB.fromCenterSize(new Vector3f(0, 0, 0), new Vector3f(2, 2, 2));
        AABB b = AABB.fromCenterSize(new Vector3f(10, 0, 0), new Vector3f(2, 2, 2));
        assertFalse(a.intersects(b));
    }

    @Test
    void touchingBoxesCountAsIntersecting() {
        // a spans x in [-1,1], b spans x in [1,3]; they share the face at x = 1.
        // Documented choice: face-touching boxes are treated as intersecting (inclusive test).
        AABB a = AABB.fromCenterSize(new Vector3f(0, 0, 0), new Vector3f(2, 2, 2));
        AABB b = AABB.fromCenterSize(new Vector3f(2, 0, 0), new Vector3f(2, 2, 2));
        assertTrue(a.intersects(b));
    }

    @Test
    void containsPointInsideAndOutside() {
        AABB box = AABB.fromCenterSize(new Vector3f(0, 0, 0), new Vector3f(2, 2, 2));
        assertTrue(box.contains(new Vector3f(0.5f, -0.5f, 0.9f)));
        assertFalse(box.contains(new Vector3f(2, 0, 0)));
    }

    @Test
    void centerAndSizeRoundTrip() {
        AABB box = AABB.fromCenterSize(new Vector3f(1, 2, 3), new Vector3f(4, 6, 8));
        Vector3f c = box.center();
        Vector3f s = box.size();
        assertEquals(1f, c.x, EPS);
        assertEquals(2f, c.y, EPS);
        assertEquals(3f, c.z, EPS);
        assertEquals(4f, s.x, EPS);
        assertEquals(6f, s.y, EPS);
        assertEquals(8f, s.z, EPS);
    }

    @Test
    void rayHitsHorizontalPlaneBelow() {
        // Ray drops straight down from y = 10 toward the plane y = 0.
        Ray ray = new Ray(new Vector3f(0, 10, 0), new Vector3f(0, -1, 0));
        float t = Intersect.rayPlane(ray, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        assertEquals(10f, t, EPS);
        Vector3f hit = ray.pointAt(t);
        assertEquals(0f, hit.y, EPS);
    }

    @Test
    void rayParallelToPlaneMisses() {
        Ray ray = new Ray(new Vector3f(0, 5, 0), new Vector3f(1, 0, 0));
        assertTrue(Intersect.rayPlane(ray, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0)) < 0);
    }

    @Test
    void planeBehindRayMisses() {
        // Ray points up, plane is below it → intersection is behind the origin.
        Ray ray = new Ray(new Vector3f(0, 10, 0), new Vector3f(0, 1, 0));
        assertTrue(Intersect.rayPlane(ray, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0)) < 0);
    }
}
