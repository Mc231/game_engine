package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CarControllerTest {

    private static final CarController.Ground flat = (x, z) -> 0f;

    @Test
    void acceleratesForwardAlongPositiveZ() {
        // Heading 0 faces +Z, so forward throttle should build speed and move +Z.
        CarController car = new CarController();
        for (int i = 0; i < 10; i++) {
            car.update(0.1f, 1f, 0f, false, flat);
        }
        assertTrue(car.speed() > 0f, "forward throttle should build positive speed");
        assertTrue(car.position().z > 0f, "heading 0 should move the car along +Z");
    }

    @Test
    void brakingReducesSpeed() {
        CarController car = new CarController();
        // Build some speed first.
        for (int i = 0; i < 10; i++) {
            car.update(0.1f, 1f, 0f, false, flat);
        }
        float before = car.speed();
        // Brake for a few frames.
        for (int i = 0; i < 3; i++) {
            car.update(0.1f, 0f, 0f, true, flat);
        }
        assertTrue(car.speed() < before, "braking should reduce speed magnitude");
        assertTrue(car.speed() >= 0f, "braking should not overshoot past zero");
    }

    @Test
    void steeringNeedsSpeed() {
        CarController car = new CarController();
        // With the car stopped, steering barely changes the heading.
        float h0 = car.heading();
        car.update(0.1f, 0f, 1f, false, flat);
        assertEquals(h0, car.heading(), 1e-4f, "stopped car should barely turn");

        // Build speed, then steering should change the heading noticeably.
        for (int i = 0; i < 20; i++) {
            car.update(0.1f, 1f, 0f, false, flat);
        }
        float h1 = car.heading();
        car.update(0.1f, 0f, 1f, false, flat);
        assertTrue(Math.abs(car.heading() - h1) > 0.05f, "moving car should turn noticeably");
    }

    @Test
    void respectsMaxSpeedCap() {
        CarController car = new CarController();
        for (int i = 0; i < 200; i++) {
            car.update(0.1f, 1f, 0f, false, flat);
        }
        assertTrue(car.speed() <= 28f + 0.01f, "speed must not exceed the max speed cap");
    }

    @Test
    void clampsToGround() {
        CarController car = new CarController();
        for (int i = 0; i < 10; i++) {
            car.update(0.1f, 1f, 0f, false, flat);
        }
        assertEquals(0f, car.position().y, 0f, "on flat ground with zero ride height, y stays 0");
    }
}
