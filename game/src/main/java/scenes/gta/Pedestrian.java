package scenes.gta;

import engine.AABB;
import engine.Collide;
import org.joml.Vector3f;

import java.util.Random;

/**
 * A background pedestrian: wanders to random roaming targets, and <em>flees</em>
 * directly away from the nearest present threat (the on-foot player, or a moving
 * car) within a reaction radius. Slides against building colliders, animates via
 * its own {@link Avatar} (per-instance walk phase, so the crowd isn't in lockstep).
 */
public class Pedestrian {

    private static final float RADIUS = 0.4f;
    private static final float PLAYER_FEAR = 6f;   // flee if the on-foot player is closer than this
    private static final float CAR_FEAR = 11f;     // a moving car is scarier (bigger radius)

    private final Avatar avatar;
    private final Random rng;
    final Vector3f pos = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Vector3f dir = new Vector3f();
    private float facing;
    private float speed;
    private final float walkSpeed;
    private final float fleeSpeed;

    public Pedestrian(Avatar avatar, float x, float z, Random rng) {
        this.avatar = avatar;
        this.rng = rng;
        pos.set(x, 0f, z);
        walkSpeed = 1.4f + rng.nextFloat() * 0.9f;
        fleeSpeed = 4.5f + rng.nextFloat() * 1.6f;
        pickTarget();
    }

    /** Send the pedestrian to a fresh spot (population recycling). */
    public void relocate(float x, float z) {
        pos.set(x, 0f, z);
        speed = 0f;
        pickTarget();
    }

    private void pickTarget() {
        float a = rng.nextFloat() * (float) (Math.PI * 2.0);
        float d = 8f + rng.nextFloat() * 22f;
        target.set(pos.x + (float) Math.sin(a) * d, 0f, pos.z + (float) Math.cos(a) * d);
    }

    /**
     * @param playerThreat on-foot player position, or null (e.g. while driving)
     * @param carThreat    moving car position, or null
     * @param walls        nearby building colliders
     */
    public void update(float dt, Vector3f playerThreat, Vector3f carThreat, AABB[] walls) {
        Vector3f threat = null;
        float bestSq = Float.MAX_VALUE;
        if (playerThreat != null) {
            float d2 = distSq(pos, playerThreat);
            if (d2 < PLAYER_FEAR * PLAYER_FEAR) {
                threat = playerThreat;
                bestSq = d2;
            }
        }
        if (carThreat != null) {
            float d2 = distSq(pos, carThreat);
            if (d2 < CAR_FEAR * CAR_FEAR && d2 < bestSq) {
                threat = carThreat;
            }
        }

        float sp;
        if (threat != null) {
            dir.set(pos.x - threat.x, 0f, pos.z - threat.z);
            sp = fleeSpeed;
        } else {
            dir.set(target.x - pos.x, 0f, target.z - pos.z);
            if (dir.lengthSquared() < 1.5f * 1.5f) {
                pickTarget();
            }
            sp = walkSpeed;
        }

        float len = dir.length();
        if (len > 1e-4f) {
            dir.div(len);
            Vector3f before = new Vector3f(pos);
            Collide.slideXZ(pos, RADIUS, dir.x * sp * dt, dir.z * sp * dt, walls);
            speed = pos.distance(before) / Math.max(dt, 1e-4f);
            float want = (float) Math.atan2(dir.x, dir.z);
            float da = (float) Math.atan2(Math.sin(want - facing), Math.cos(want - facing));
            facing += da * Math.min(dt * 10f, 1f);
        } else {
            speed = 0f;
        }

        avatar.animate(speed, dt);
    }

    public void render() {
        avatar.render(pos, facing);
    }

    private static float distSq(Vector3f a, Vector3f b) {
        float dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
