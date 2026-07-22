package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A first-person "fly" camera. It stores a position and a look direction
 * (yaw/pitch), moves with WASD + Space/Shift, and turns with the mouse. Call
 * {@link #processInput} each frame, then {@link #viewMatrix()} for rendering.
 */
public class Camera {

    private final Vector3f position = new Vector3f(0f, 0f, 6f);

    // Look angles in degrees. yaw = -90 points down -Z (into the screen).
    private float yaw = -90f;
    private float pitch = 0f;

    private float moveSpeed = 3.0f;         // world units per second
    private float mouseSensitivity = 0.1f;  // degrees per pixel

    // Derived orientation vectors (recomputed from yaw/pitch).
    private final Vector3f front = new Vector3f(0f, 0f, -1f);
    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Vector3f right = new Vector3f(1f, 0f, 0f);
    private final Vector3f worldUp = new Vector3f(0f, 1f, 0f);

    private final Vector3f temp = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Matrix4f view = new Matrix4f();

    public Camera() {
        updateVectors();
    }

    public void processInput(Input input, float deltaSeconds) {
        // --- Mouse look ---
        yaw += input.mouseDeltaX() * mouseSensitivity;
        pitch += input.mouseDeltaY() * mouseSensitivity;
        pitch = Math.max(-89f, Math.min(89f, pitch));   // no flipping over the top
        updateVectors();

        // --- Keyboard movement (frame-rate independent via deltaSeconds) ---
        float velocity = moveSpeed * deltaSeconds;
        if (input.isKeyDown(GLFW_KEY_W)) position.add(front.mul(velocity, temp));
        if (input.isKeyDown(GLFW_KEY_S)) position.sub(front.mul(velocity, temp));
        if (input.isKeyDown(GLFW_KEY_D)) position.add(right.mul(velocity, temp));
        if (input.isKeyDown(GLFW_KEY_A)) position.sub(right.mul(velocity, temp));
        if (input.isKeyDown(GLFW_KEY_SPACE)) position.add(worldUp.mul(velocity, temp));
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) position.sub(worldUp.mul(velocity, temp));
    }

    /** Rebuild front/right/up from the current yaw & pitch. */
    private void updateVectors() {
        double y = Math.toRadians(yaw);
        double p = Math.toRadians(pitch);
        front.set(
                (float) (Math.cos(y) * Math.cos(p)),
                (float) Math.sin(p),
                (float) (Math.sin(y) * Math.cos(p))
        ).normalize();
        front.cross(worldUp, right).normalize();   // right = front x worldUp
        right.cross(front, up).normalize();         // up    = right x front
    }

    /** View matrix that transforms world space into this camera's space. */
    public Matrix4f viewMatrix() {
        position.add(front, target);                // a point straight ahead
        return view.identity().lookAt(position, target, up);
    }

    public Vector3f position() {
        return position;
    }

    public Camera setPosition(float x, float y, float z) {
        position.set(x, y, z);
        return this;
    }

    public Camera setMoveSpeed(float unitsPerSecond) {
        this.moveSpeed = unitsPerSecond;
        return this;
    }

    /** The direction the camera is looking (normalized). */
    public Vector3f front() {
        return front;
    }
}
