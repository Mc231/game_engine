package scenes.city;

import engine.AABB;
import engine.Collide;
import engine.OrbitCamera;
import org.joml.Vector3f;

/**
 * Drives a character on foot from an {@link OrbitCamera}'s point of view: move
 * input is interpreted <em>relative to the camera</em> (W = away from camera),
 * the body slides against walls via {@link Collide}, and the avatar smoothly
 * turns to face its movement direction.
 *
 * <p>Physics are deliberately simple (flat ground at {@code y = 0}, no gravity);
 * Phase 2 will swap the flat ground for the city's height/collision. Facing uses
 * the engine convention forward = {@code (sin facing, 0, cos facing)}.
 */
public class ThirdPersonController {

    private final Vector3f position = new Vector3f();
    private final Vector3f moveDir = new Vector3f();
    private float facing = 0f;
    private float speed = 0f;      // actual planar speed this frame (for animation)

    private final float radius = 0.4f;
    private final float walkSpeed = 3.4f;
    private final float runSpeed = 7.0f;
    private final float turnRate = 12f;   // radians/sec toward the move direction

    public ThirdPersonController(float x, float z, float facing) {
        position.set(x, 0f, z);
        this.facing = facing;
    }

    /**
     * @param forwardIn -1..1 (W positive)
     * @param strafeIn  -1..1 (D positive)
     * @param run       hold to run
     */
    public void update(float dt, float forwardIn, float strafeIn, boolean run,
                       OrbitCamera camera, AABB[] walls) {
        Vector3f f = camera.forwardXZ();
        Vector3f r = camera.rightXZ();
        moveDir.set(f.x * forwardIn + r.x * strafeIn, 0f, f.z * forwardIn + r.z * strafeIn);

        float mag = moveDir.length();
        if (mag > 1e-4f) {
            moveDir.div(mag);
            float target = run ? runSpeed : walkSpeed;
            float dx = moveDir.x * target * dt;
            float dz = moveDir.z * target * dt;

            Vector3f before = new Vector3f(position);
            Collide.slideXZ(position, radius, dx, dz, walls);
            speed = position.distance(before) / Math.max(dt, 1e-4f);

            // Turn toward the intended move direction (shortest angle).
            float want = (float) Math.atan2(moveDir.x, moveDir.z);
            float da = wrapPi(want - facing);
            facing += da * Math.min(dt * turnRate, 1f);
        } else {
            speed = 0f;
        }
    }

    private static float wrapPi(float a) {
        return (float) Math.atan2(Math.sin(a), Math.cos(a));
    }

    public Vector3f position() {
        return position;
    }

    public float facing() {
        return facing;
    }

    public float speed() {
        return speed;
    }
}
