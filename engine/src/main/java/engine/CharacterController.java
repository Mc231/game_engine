package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A first-person <em>walking</em> controller: horizontal movement on the XZ
 * plane driven by yaw, plus gravity, jumping, and clamping to a height field.
 *
 * <p>The physics are pure and self-contained: the ground height comes from the
 * {@link Ground} functional interface, never from OpenGL. This makes the
 * controller fully unit-testable without a rendering context.
 */
public class CharacterController {

    /** Supplies terrain height at a world XZ coordinate (feet level). */
    @FunctionalInterface
    public interface Ground {
        float heightAt(float x, float z);
    }

    private final Vector3f position = new Vector3f(0f, 0f, 0f);
    private float velocityY = 0f;

    // Look angles in degrees. yaw = -90 points down -Z (into the screen).
    private float yaw = -90f;
    private float pitch = 0f;
    private boolean onGround = false;

    // Tunables.
    private float eyeHeight = 1.7f;         // camera height above the feet
    private float moveSpeed = 8f;           // world units per second
    private float jumpSpeed = 7f;           // upward launch velocity
    private float gravity = 20f;            // downward acceleration
    private float mouseSensitivity = 0.1f;  // degrees per pixel

    // Scratch vectors reused each frame to avoid allocation.
    private final Vector3f forwardDir = new Vector3f();
    private final Vector3f rightDir = new Vector3f();
    private final Vector3f worldUp = new Vector3f(0f, 1f, 0f);
    private final Vector3f front = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Matrix4f view = new Matrix4f();

    /** Turn with the mouse; pitch is clamped so the view can't flip over. */
    public void addLook(float mouseDeltaX, float mouseDeltaY) {
        yaw += mouseDeltaX * mouseSensitivity;
        pitch += mouseDeltaY * mouseSensitivity;
        pitch = Math.max(-89f, Math.min(89f, pitch));
    }

    /**
     * Advance the simulation by {@code dt} seconds.
     *
     * @param forward     forward input, typically -1..1
     * @param right       strafe input, typically -1..1
     * @param jumpPressed whether jump was requested this frame
     * @param ground      terrain height field to clamp against
     */
    public void update(float dt, float forward, float right, boolean jumpPressed, Ground ground) {
        // 1) Horizontal movement on XZ from yaw (pitch ignored so we walk level).
        double y = Math.toRadians(yaw);
        forwardDir.set((float) Math.cos(y), 0f, (float) Math.sin(y)).normalize();
        forwardDir.cross(worldUp, rightDir).normalize();   // right = forward x up
        position.x += (forwardDir.x * forward + rightDir.x * right) * moveSpeed * dt;
        position.z += (forwardDir.z * forward + rightDir.z * right) * moveSpeed * dt;

        // 2) Gravity integration.
        velocityY -= gravity * dt;
        position.y += velocityY * dt;

        // 3) Clamp to the ground (position.y is at eye level).
        float groundY = ground.heightAt(position.x, position.z) + eyeHeight;
        if (position.y <= groundY) {
            position.y = groundY;
            velocityY = 0f;
            onGround = true;
        } else {
            onGround = false;
        }

        // 4) Jump only when standing on the ground.
        if (jumpPressed && onGround) {
            velocityY = jumpSpeed;
            onGround = false;
        }
    }

    /** View matrix built from the full 3D look direction (yaw + pitch). */
    public Matrix4f viewMatrix() {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        front.set(
                (float) (Math.cos(y) * Math.cos(p)),
                (float) Math.sin(p),
                (float) (Math.sin(y) * Math.cos(p))
        ).normalize();
        position.add(front, target);
        return view.identity().lookAt(position, target, worldUp);
    }

    public Vector3f position() {
        return position;
    }

    public boolean onGround() {
        return onGround;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public CharacterController setPosition(float x, float y, float z) {
        position.set(x, y, z);
        return this;
    }

    public CharacterController setEyeHeight(float eyeHeight) {
        this.eyeHeight = eyeHeight;
        return this;
    }

    public CharacterController setMoveSpeed(float unitsPerSecond) {
        this.moveSpeed = unitsPerSecond;
        return this;
    }

    public CharacterController setJumpSpeed(float jumpSpeed) {
        this.jumpSpeed = jumpSpeed;
        return this;
    }

    public CharacterController setGravity(float gravity) {
        this.gravity = gravity;
        return this;
    }
}
