package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CameraTest {

    @Test
    void defaultLooksDownNegativeZ() {
        Camera cam = new Camera();
        Vector3f f = cam.front();
        assertEquals(0f, f.x, 1e-5f);
        assertEquals(0f, f.y, 1e-5f);
        assertEquals(-1f, f.z, 1e-5f);
    }

    @Test
    void frontIsNormalized() {
        Camera cam = new Camera();
        assertEquals(1f, cam.front().length(), 1e-5f);
    }

    @Test
    void setPositionUpdatesPosition() {
        Camera cam = new Camera().setPosition(3, 4, 5);
        assertEquals(new Vector3f(3, 4, 5), cam.position());
    }

    @Test
    void fluentSettersReturnSameInstance() {
        Camera cam = new Camera();
        assertSame(cam, cam.setPosition(0, 0, 0));
        assertSame(cam, cam.setMoveSpeed(10f));
    }
}
