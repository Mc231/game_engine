package engine;

import org.joml.Vector3f;

/**
 * An arcade-style <em>car</em> driving controller: longitudinal acceleration
 * and braking along a heading, speed-scaled steering, rolling drag, and
 * clamping to a height field.
 *
 * <p>The physics are pure and self-contained: the ground height comes from the
 * {@link Ground} functional interface, never from OpenGL. This makes the
 * controller fully unit-testable without a rendering context.
 *
 * <p><b>Forward convention:</b> {@code forward = (sin(heading), 0, cos(heading))},
 * so a heading of {@code 0} faces {@code +Z}, and increasing heading rotates the
 * car. A positive {@link #speed} drives along {@link #forward()}; a negative
 * speed reverses.
 */
public class CarController {

    /** Supplies terrain height at a world XZ coordinate (wheel-contact level). */
    @FunctionalInterface
    public interface Ground {
        float heightAt(float x, float z);
    }

    private final Vector3f position = new Vector3f(0f, 0f, 0f);
    private float heading = 0f;   // yaw in radians; 0 faces +Z
    private float speed = 0f;     // units per second; negative = reverse

    // Tunables.
    private float enginePower = 18f;      // forward/reverse acceleration, units/s^2
    private float brakePower = 30f;       // deceleration toward zero when braking, units/s^2
    private float maxSpeed = 28f;         // forward speed cap
    private float maxReverseSpeed = 8f;   // reverse speed cap (magnitude)
    private float drag = 0.9f;            // per-second velocity retention base
    private float turnRate = 1.8f;        // radians/second at full steering and speed
    private float rideHeight = 0f;        // body height above the ground

    /**
     * Advance the simulation by {@code dt} seconds.
     *
     * @param dt       time step in seconds
     * @param throttle forward/reverse input in {@code [-1, 1]}
     * @param steer    steering input in {@code [-1, 1]}
     * @param brake    whether the brake is applied this frame
     * @param ground   terrain height field to clamp against
     */
    public void update(float dt, float throttle, float steer, boolean brake, Ground ground) {
        // 1) Longitudinal: brake toward zero, else accelerate along the heading.
        if (brake) {
            if (speed > 0f) {
                speed = Math.max(0f, speed - brakePower * dt);
            } else if (speed < 0f) {
                speed = Math.min(0f, speed + brakePower * dt);
            }
        } else {
            speed += throttle * enginePower * dt;
        }

        // 2) Rolling drag, with a dead zone so the car fully stops when coasting.
        speed *= (float) Math.pow(drag, dt);
        if (Math.abs(speed) < 0.05f && throttle == 0f) {
            speed = 0f;
        }

        // 3) Clamp to the speed envelope.
        speed = Math.max(-maxReverseSpeed, Math.min(maxSpeed, speed));

        // 4) Steering, scaled by speed so a stopped car barely turns.
        float turn = steer * turnRate * dt * Math.signum(speed) * Math.min(Math.abs(speed) / 4f, 1f);
        heading += turn;

        // 5) Move along the heading on the XZ plane.
        Vector3f f = forward();
        position.x += f.x * speed * dt;
        position.z += f.z * speed * dt;

        // 6) Clamp the body to the ground.
        position.y = ground.heightAt(position.x, position.z) + rideHeight;
    }

    /** The car's forward direction as a new normalized vector. */
    public Vector3f forward() {
        return new Vector3f((float) Math.sin(heading), 0f, (float) Math.cos(heading)).normalize();
    }

    public Vector3f position() {
        return position;
    }

    public float heading() {
        return heading;
    }

    public float speed() {
        return speed;
    }

    public CarController setPosition(float x, float y, float z) {
        position.set(x, y, z);
        return this;
    }

    public CarController setHeading(float radians) {
        this.heading = radians;
        return this;
    }

    public CarController setEnginePower(float enginePower) {
        this.enginePower = enginePower;
        return this;
    }

    public CarController setBrakePower(float brakePower) {
        this.brakePower = brakePower;
        return this;
    }

    public CarController setMaxSpeed(float maxSpeed) {
        this.maxSpeed = maxSpeed;
        return this;
    }

    public CarController setMaxReverseSpeed(float maxReverseSpeed) {
        this.maxReverseSpeed = maxReverseSpeed;
        return this;
    }

    public CarController setDrag(float drag) {
        this.drag = drag;
        return this;
    }

    public CarController setTurnRate(float turnRate) {
        this.turnRate = turnRate;
        return this;
    }

    public CarController setRideHeight(float rideHeight) {
        this.rideHeight = rideHeight;
        return this;
    }
}
