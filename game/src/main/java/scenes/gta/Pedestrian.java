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

    private static final float DOWN_TIME = 4.5f;   // seconds knocked out before getting up

    private final Avatar avatar;
    private final Random rng;
    final Vector3f pos = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Vector3f dir = new Vector3f();
    private final Vector3f knock = new Vector3f();   // knockback velocity while down
    private float facing;
    private float animSpeed;      // smoothed speed that drives the walk animation (no per-frame jitter)
    private float blockedTimer;   // how long we've been unable to make progress (→ re-path)
    private boolean down;
    private boolean dead;
    private float downTimer;
    private float health = 100f;
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
        animSpeed = 0f;
        blockedTimer = 0f;
        down = false;
        dead = false;
        downTimer = 0f;
        health = 100f;
        knock.zero();
        pickTarget();
    }

    public boolean isDown() {
        return down;
    }

    public boolean isDead() {
        return dead;
    }

    /** A dead pedestrian whose linger has elapsed — the manager should recycle it. */
    public boolean readyToRecycle() {
        return dead && downTimer <= 0f;
    }

    /** Knock the pedestrian down, launched along {@code dir} scaled by impact speed. */
    public void hit(Vector3f dir, float impactSpeed) {
        if (down) {
            return;
        }
        down = true;
        downTimer = DOWN_TIME;
        setKnock(dir, Math.min(Math.abs(impactSpeed), 22f) * 0.4f);
    }

    /** Apply gunshot damage from direction {@code dir}: stagger, or die if health hits 0. */
    public void takeDamage(float amount, Vector3f dir) {
        if (dead) {
            return;
        }
        health -= amount;
        down = true;
        if (health <= 0f) {
            dead = true;
            downTimer = 6f;          // corpse lingers, then recycles
            setKnock(dir, 7f);
        } else {
            downTimer = Math.max(downTimer, 0.5f);   // brief stagger
            setKnock(dir, 3f);
        }
    }

    private void setKnock(Vector3f dir, float mag) {
        knock.set(dir.x, 0f, dir.z);
        if (knock.lengthSquared() > 1e-6f) {
            knock.normalize().mul(mag);
        }
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
        if (down) {
            // Slide from the impact (decaying); a stunned ped gets back up, a dead one lingers.
            Collide.slideXZ(pos, RADIUS, knock.x * dt, knock.z * dt, walls);
            knock.mul((float) Math.pow(0.06, dt));
            downTimer -= dt;
            if (downTimer <= 0f && !dead) {
                down = false;
                pickTarget();
            }
            animSpeed = 0f;
            return;
        }

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
        float intended = 0f;
        if (len > 1e-4f) {
            dir.div(len);
            Vector3f before = new Vector3f(pos);
            Collide.slideXZ(pos, RADIUS, dir.x * sp * dt, dir.z * sp * dt, walls);
            float moved = pos.distance(before) / Math.max(dt, 1e-4f);
            intended = sp;

            // Blocked by a building? Steer somewhere new instead of grinding the wall.
            if (moved < sp * 0.3f) {
                blockedTimer += dt;
                if (blockedTimer > 0.35f) {
                    pickTarget();
                    blockedTimer = 0f;
                }
            } else {
                blockedTimer = 0f;
            }

            float want = (float) Math.atan2(dir.x, dir.z);
            float da = (float) Math.atan2(Math.sin(want - facing), Math.cos(want - facing));
            facing += da * Math.min(dt * 10f, 1f);
        }

        // Drive the animation from a smoothed *intended* speed so wall contact
        // (which zeroes the measured speed for a frame) doesn't make the legs stutter.
        animSpeed += (intended - animSpeed) * Math.min(dt * 8f, 1f);
        avatar.animate(animSpeed, dt);
    }

    public void render() {
        avatar.render(pos, facing, down ? (float) (Math.PI / 2.0) : 0f);
    }

    private static float distSq(Vector3f a, Vector3f b) {
        float dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }
}
