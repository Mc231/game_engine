package scenes.prison;

import engine.AABB;
import engine.Vision;
import org.joml.Vector3f;

/**
 * A patrolling guard: walks a waypoint loop facing its movement direction, and
 * can see the player if they're within range, inside its vision cone, and not
 * hidden behind a wall ({@link Vision}).
 *
 * Facing convention: forward = (sin facing, 0, cos facing), so a +Z model
 * rotated by {@code rotateY(facing)} points where the guard looks.
 */
public class Guard {

    public final Vector3f position = new Vector3f();
    public final float range = 15f;
    public final float halfFovDeg = 30f;

    private final Vector3f[] waypoints;
    private final float speed = 2.4f;
    private final float eyeY = 1.55f;
    private int target;
    private float facing;
    private boolean alerted;
    public float walkPhase;   // drives the procedural walk animation

    private final Vector3f forward = new Vector3f();

    public Guard(Vector3f[] waypoints) {
        this.waypoints = waypoints;
        reset();
    }

    public void reset() {
        position.set(waypoints[0].x, 0f, waypoints[0].z);
        target = 1 % waypoints.length;
        Vector3f t = waypoints[target];
        facing = (float) Math.atan2(t.x - position.x, t.z - position.z);
        alerted = false;
        walkPhase = 0f;
    }

    public void update(float dt) {
        Vector3f t = waypoints[target];
        float dx = t.x - position.x;
        float dz = t.z - position.z;
        float d = (float) Math.sqrt(dx * dx + dz * dz);
        if (d < 0.35f) {
            target = (target + 1) % waypoints.length;
        } else {
            dx /= d;
            dz /= d;
            position.x += dx * speed * dt;
            position.z += dz * speed * dt;
            facing = (float) Math.atan2(dx, dz);
            walkPhase += speed * dt * 3.2f;   // stride cadence
        }
    }

    public boolean canSee(Vector3f playerPos, AABB[] walls) {
        forward.set((float) Math.sin(facing), 0f, (float) Math.cos(facing));
        Vector3f eye = new Vector3f(position.x, eyeY, position.z);
        Vector3f tgt = new Vector3f(playerPos.x, eyeY, playerPos.z);
        float cosHalf = (float) Math.cos(Math.toRadians(halfFovDeg));
        alerted = Vision.canSee(eye, forward, cosHalf, range, tgt, walls);
        return alerted;
    }

    public float facing() {
        return facing;
    }

    public boolean alerted() {
        return alerted;
    }
}
