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
    private float baseYaw = 0f;        // added to yaw so the cam can trail a heading (e.g. a car)
    private float pitch = 0.5f;        // radians above the horizon
    private float distance = 6f;

    // Tunables.
    private float minPitch = 0.12f, maxPitch = 1.35f;
    private float minDistance = 1.2f;
    private float sensitivity = 0.005f;   // radians per pixel
    private float targetHeight = 1.4f;    // look at target + this height (chest)
    private float shoulder = 0f;          // lateral offset of the look point (over-the-shoulder aim)
    private float collisionMargin = 0.35f;
    private float followLerp = 12f;       // higher = snappier follow

    // Aim mode: a free-pitching over-the-shoulder view (so shots can angle up/down).
    private boolean aiming = false;
    private float aimPitch = 0.1f;        // radians; + = aim up (its own mouse-Y axis)
    private static final float AIM_MIN = -0.7f, AIM_MAX = 1.1f;

    // Working state.
    private final Vector3f position = new Vector3f();
    private final Vector3f lookAt = new Vector3f();
    private final Vector3f desired = new Vector3f();
    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Matrix4f view = new Matrix4f();
    private final Vector3f fwd = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f aimDir = new Vector3f();
    private final Vector3f tmpDir = new Vector3f();
    private boolean initialized = false;

    /** Orbit/aim with mouse deltas. In aim mode the Y axis pitches the aim freely. */
    public void addLook(float mouseDeltaX, float mouseDeltaY) {
        yaw += mouseDeltaX * sensitivity;
        if (aiming) {
            aimPitch = Math.max(AIM_MIN, Math.min(AIM_MAX, aimPitch + mouseDeltaY * sensitivity));
        } else {
            pitch = Math.max(minPitch, Math.min(maxPitch, pitch + mouseDeltaY * sensitivity));
        }
    }

    /** Toggle over-the-shoulder aim mode (free vertical aim). */
    public OrbitCamera setAim(boolean aiming) {
        this.aiming = aiming;
        return this;
    }

    /**
     * Recompute the camera for {@code target} this frame, pulling in if a wall
     * blocks the view. Pass {@code dt} for smoothing; {@code walls} may be null.
     */
    public void update(Vector3f target, float dt, AABB[] walls) {
        float a = yaw + baseYaw;

        if (aiming) {
            // View/shot direction = horizontal "into screen" forward tilted by aimPitch.
            float cq = (float) Math.cos(aimPitch), sq = (float) Math.sin(aimPitch);
            aimDir.set(-(float) Math.sin(a) * cq, sq, -(float) Math.cos(a) * cq).normalize();

            // Pivot at the shooter's shoulder.
            Vector3f r = rightXZ();
            lookAt.set(target.x + r.x * shoulder, target.y + targetHeight, target.z + r.z * shoulder);

            // The camera sits BEHIND (horizontal, yaw only) at head height — it does
            // NOT move with the aim pitch, so aiming up/down rotates the view instead
            // of flinging the camera underground or into the sky.
            float bx = (float) Math.sin(a), bz = (float) Math.cos(a);   // horizontal "behind"
            float dist = distance;
            if (walls != null && walls.length > 0) {
                tmpDir.set(bx, 0.2f, bz);
                Ray ray = new Ray(lookAt, tmpDir);
                float nearest = distance;
                for (AABB w : walls) {
                    float t = Intersect.rayAABB(ray, w);
                    if (t >= 0f && t < nearest) {
                        nearest = t;
                    }
                }
                dist = Math.max(minDistance, nearest - collisionMargin);
            }
            desired.set(lookAt.x + bx * dist, lookAt.y + 0.3f, lookAt.z + bz * dist);
            if (!initialized) {
                position.set(desired);
                initialized = true;
            } else {
                position.lerp(desired, Math.min(dt * followLerp, 1f));
            }
            lookAt.set(position).add(aimDir);   // look ALONG aimDir → crosshair = aimDir
            return;
        }

        lookAt.set(target.x, target.y + targetHeight, target.z);
        if (shoulder != 0f) {                 // shift the look point sideways
            Vector3f r = rightXZ();
            lookAt.x += r.x * shoulder;
            lookAt.z += r.z * shoulder;
        }

        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);
        float ox = (float) Math.sin(a) * cp;   // offset direction from look point
        float oz = (float) Math.cos(a) * cp;

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

    /** The point the camera is centered on (screen center = crosshair). */
    public Vector3f lookAt() {
        return lookAt;
    }

    /**
     * Normalized 3D direction from the camera through the crosshair (screen
     * center). Cast a shot from {@link #position()} along this so bullets land
     * where the reticle points, regardless of the shoulder offset.
     */
    public Vector3f aimDirection() {
        return aimDir.set(lookAt).sub(position).normalize();
    }

    /** Unit "forward" move axis on XZ: into the screen, away from the camera. */
    public Vector3f forwardXZ() {
        float a = yaw + baseYaw;
        return fwd.set(-(float) Math.sin(a), 0f, -(float) Math.cos(a));
    }

    /** Unit "right" move axis on XZ ({@code forward × up}). */
    public Vector3f rightXZ() {
        float a = yaw + baseYaw;
        float fx = -(float) Math.sin(a), fz = -(float) Math.cos(a);
        return right.set(-fz, 0f, fx);
    }

    public float yaw() {
        return yaw;
    }

    /**
     * Base orbit angle added to the mouse-driven yaw, so the camera trails a
     * heading (e.g. {@code car.heading() + PI} to sit behind a car). Use 0 for a
     * free world-relative orbit (on foot).
     */
    public OrbitCamera setBaseYaw(float radians) {
        this.baseYaw = radians;
        return this;
    }

    /** Recenter the mouse-orbit offset (e.g. when entering a vehicle). */
    public OrbitCamera resetYaw() {
        this.yaw = 0f;
        return this;
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

    /** Lateral offset of the look point along camera-right (0 = centered; used for aim mode). */
    public OrbitCamera setShoulder(float shoulder) {
        this.shoulder = shoulder;
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
