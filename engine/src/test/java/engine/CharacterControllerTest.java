package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CharacterControllerTest {

    private static final float DT = 0.016f;
    private static final CharacterController.Ground flat = (x, z) -> 0f;

    @Test
    void fallsAndLandsOnGround() {
        CharacterController c = new CharacterController().setPosition(0, 50, 0).setEyeHeight(2f);
        for (int i = 0; i < 300; i++) {
            c.update(DT, 0, 0, false, flat);
        }
        assertEquals(2f, c.position().y, 0.01f);
        assertTrue(c.onGround());
    }

    @Test
    void jumpsThenLandsBackOnGround() {
        CharacterController c = new CharacterController().setPosition(0, 50, 0).setEyeHeight(2f);
        // Settle onto the ground first.
        for (int i = 0; i < 300; i++) {
            c.update(DT, 0, 0, false, flat);
        }
        assertTrue(c.onGround());

        // Launch.
        c.update(DT, 0, 0, true, flat);
        assertFalse(c.onGround());

        // Track the peak while airborne, then confirm we settle back down.
        float peak = c.position().y;
        for (int i = 0; i < 300; i++) {
            c.update(DT, 0, 0, false, flat);
            peak = Math.max(peak, c.position().y);
        }
        assertTrue(peak > 2f, "jump should rise above resting height");
        assertEquals(2f, c.position().y, 0.01f);
        assertTrue(c.onGround());
    }

    @Test
    void staysGroundedWhenIdle() {
        CharacterController c = new CharacterController().setPosition(0, 50, 0).setEyeHeight(2f);
        for (int i = 0; i < 300; i++) {
            c.update(DT, 0, 0, false, flat);
        }
        for (int i = 0; i < 500; i++) {
            c.update(DT, 0, 0, false, flat);
            float y = c.position().y;
            assertFalse(Float.isNaN(y), "y must never be NaN");
            assertTrue(y >= 2f - 0.001f, "must never sink below the ground");
            assertEquals(2f, y, 0.001f);
        }
        assertTrue(c.onGround());
    }

    @Test
    void movesForwardAlongNegativeZ() {
        // Default yaw -90 faces -Z, so forward input should decrease z.
        CharacterController c = new CharacterController();
        float startZ = c.position().z;
        c.update(0.1f, 1f, 0f, false, flat);
        assertTrue(c.position().z < startZ, "forward at yaw -90 should move along -Z");
    }
}
