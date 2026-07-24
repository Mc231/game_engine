package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A third-person orbit / chase camera. It swings around a moving target at a
 * fixed distance, controlled by yaw (horizontal) and pitch (vertical) angles,
 * and smoothly follows the target each frame.
 *
 * <p>Two features make it usable for a GTA-style game:
 * <ul>
 *   <li><b>Wall pull-in</b> — {@link #update} raycasts from the look point toward
 *       the ideal camera spot against a set of wall {@link AABB}s; if a wall is
 *       nearer than the orbit distance, the camera is pulled in so it never ends
 *       up behind (or inside) geometry.</li>
 *   <li><b>Camera-relative movement basis</b> — {@link #forwardXZ}/{@link #rightXZ}
 *       give the horizontal move axes a character should use so that "forward"
 *       always means "into the screen, away from the camera".</li>
 * </ul>
 *
 * Angle convention matches the rest of the engine: forward on XZ is
 * {@code (sin a, cos a)}, so a model rotated by {@code rotateY(a)} points along it.
 */
public class OrbitCamera {

    // Orbit state.
    private float yaw = 0f;            // radians; 0 = camera behind target's -Z
    private float pitch = 0.5f;        // radians above the horizon
    private float distance = 6f;

    // Tunables.
    private float minPitch = 0.12f, maxPitch = 1.35f;
    private float minDistance = 1.2f;
    private float sensitivity = 0.005f;   // radians per pixel
    private float targetHeight = 1.4f;    // look at target + this height (chest)
    private float collisionMargin = 0.35f;
    private float followLerp = 12f;       // higher = snappier follow

    // Working state.
    private final Vector3f position = new Vector3f();
    private final Vector3f lookAt = new Vector3f();
    private final Vector3f desired = new Vector3f();
    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Matrix4f view = new Matrix4f();
    private final Vector3f fwd = new Vector3f();
    private final Vector3f right = new Vector3f();
    private boolean initialized = false;

    /** Orbit the camera with mouse deltas (pitch is clamped). */
    public void addLook(float mouseDeltaX, float mouseDeltaY) {
        yaw += mouseDeltaX * sensitivity;
        pitch += mouseDeltaY * sensitivity;
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
    }

    /**
     * Recompute the camera for {@code target} this frame, pulling in if a wall
     * blocks the view. Pass {@code dt} for smoothing; {@code walls} may be null.
     */
    public void update(Vector3f target, float dt, AABB[] walls) {
        lookAt.set(target.x, target.y + targetHeight, target.z);

        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);
        float ox = (float) Math.sin(yaw) * cp;   // offset direction from look point
        float oz = (float) Math.cos(yaw) * cp;

        float dist = distance;
        if (walls != null && walls.length > 0) {
            desired.set(lookAt.x + ox * distance, lookAt.y + sp * distance, lookAt.z + oz * distance);
            Ray ray = new Ray(lookAt, desired.sub(lookAt, new Vector3f()));
            float nearest = distance;
            for (AABB w : walls) {
                float t = Intersect.rayAABB(ray, w);
                if (t >= 0f && t < nearest) {
                    nearest = t;
                }
            }
            dist = Math.max(minDistance, nearest - collisionMargin);
        }

        desired.set(lookAt.x + ox * dist, lookAt.y + sp * dist, lookAt.z + oz * dist);
        if (!initialized) {
            position.set(desired);
            initialized = true;
        } else {
            position.lerp(desired, Math.min(dt * followLerp, 1f));
        }
    }

    /** View matrix looking from the camera to the (chest-height) look point. */
    public Matrix4f viewMatrix() {
        return view.identity().lookAt(position, lookAt, up);
    }

    /** Camera world position (for {@code uViewPos}). */
    public Vector3f position() {
        return position;
    }

    /** Unit "forward" move axis on XZ: into the screen, away from the camera. */
    public Vector3f forwardXZ() {
        return fwd.set(-(float) Math.sin(yaw), 0f, -(float) Math.cos(yaw));
    }

    /** Unit "right" move axis on XZ ({@code forward × up}). */
    public Vector3f rightXZ() {
        float fx = -(float) Math.sin(yaw), fz = -(float) Math.cos(yaw);
        return right.set(-fz, 0f, fx);
    }

    public float yaw() {
        return yaw;
    }

    public OrbitCamera setDistance(float distance) {
        this.distance = distance;
        return this;
    }

    public OrbitCamera setPitch(float pitch) {
        this.pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        return this;
    }

    public OrbitCamera setTargetHeight(float targetHeight) {
        this.targetHeight = targetHeight;
        return this;
    }

    public OrbitCamera setSensitivity(float radiansPerPixel) {
        this.sensitivity = radiansPerPixel;
        return this;
    }

    public OrbitCamera setFollowLerp(float perSecond) {
        this.followLerp = perSecond;
        return this;
    }
}
