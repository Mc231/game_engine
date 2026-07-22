package scenes.prison;

import engine.AABB;
import engine.Collide;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * First-person player for the prison: mouse-look + WASD movement that slides
 * along walls ({@link Collide}). Crouch lowers the eye height and speed.
 */
public class PrisonPlayer {

    public final Vector3f position = new Vector3f();

    private float yaw = 90f;      // forward on XZ = (cos yaw, 0, sin yaw)
    private float pitch = 0f;
    private boolean crouch;

    private static final float RADIUS = 0.35f;
    private static final float STAND_EYE = 1.7f, CROUCH_EYE = 1.05f;
    private static final float WALK_SPEED = 4.5f, CROUCH_SPEED = 2.2f;
    private static final float SENSITIVITY = 0.12f;

    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Vector3f fwd = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f target = new Vector3f();

    public void setStart(float x, float z, float yawDegrees) {
        position.set(x, STAND_EYE, z);
        yaw = yawDegrees;
        pitch = 0f;
        crouch = false;
    }

    public void addLook(float mouseDx, float mouseDy) {
        yaw += mouseDx * SENSITIVITY;
        pitch = Math.max(-89f, Math.min(89f, pitch + mouseDy * SENSITIVITY));
    }

    public void update(float dt, float forwardInput, float rightInput, boolean sneak, AABB[] walls) {
        crouch = sneak;
        position.y = crouch ? CROUCH_EYE : STAND_EYE;

        double y = Math.toRadians(yaw);
        fwd.set((float) Math.cos(y), 0f, (float) Math.sin(y)).normalize();
        fwd.cross(up, right).normalize();     // right = forward x up

        float speed = (crouch ? CROUCH_SPEED : WALK_SPEED) * dt;
        float dx = (fwd.x * forwardInput + right.x * rightInput) * speed;
        float dz = (fwd.z * forwardInput + right.z * rightInput) * speed;
        Collide.slideXZ(position, RADIUS, dx, dz, walls);
    }

    public Matrix4f viewMatrix() {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        Vector3f look = new Vector3f(
                (float) (Math.cos(y) * Math.cos(p)),
                (float) Math.sin(p),
                (float) (Math.sin(y) * Math.cos(p))).normalize();
        return new Matrix4f().lookAt(position, target.set(position).add(look), up);
    }

    public boolean crouching() {
        return crouch;
    }
}
