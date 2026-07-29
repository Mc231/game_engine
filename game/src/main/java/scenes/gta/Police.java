package scenes.gta;

import engine.AABB;
import engine.Collide;
import org.joml.Vector3f;

/**
 * An armed police officer that chases the target (player or car) and fires when
 * stopped within range with line of sight. Has health and a dead state (reusing
 * the flat-avatar knockdown). {@link #update} returns the damage dealt this frame.
 */
public class Police {

    private static final float RADIUS = 0.4f;
    private static final float RUN_SPEED = 5.6f;
    private static final float STOP_RANGE = 11f;   // stop and shoot inside this

    private final Avatar avatar;
    final Vector3f pos = new Vector3f();
    private final Vector3f dir = new Vector3f();
    private final Weapon gun = new Weapon("SIDEARM", 1.4f, 11f, 26f, Integer.MAX_VALUE);
    private float facing;
    private float animSpeed;   // smoothed, so wall contact doesn't stutter the walk
    private float health = 100f;
    private boolean dead;
    private float deadTimer;

    public Police(Avatar avatar, float x, float z) {
        this.avatar = avatar;
        pos.set(x, 0f, z);
    }

    public boolean isDead() {
        return dead;
    }

    public boolean readyToRemove() {
        return dead && deadTimer <= 0f;
    }

    public void takeDamage(float amount, Vector3f from) {
        if (dead) {
            return;
        }
        health -= amount;
        if (health <= 0f) {
            dead = true;
            deadTimer = 5f;
        }
    }

    /** Advance; returns damage dealt to the target this frame. {@code fire} plays a shot sound when true. */
    public float update(float dt, Vector3f target, AABB[] walls, boolean hasLoS) {
        gun.update(dt);
        if (dead) {
            deadTimer -= dt;
            animSpeed = 0f;
            avatar.animate(0f, dt);
            return 0f;
        }

        dir.set(target.x - pos.x, 0f, target.z - pos.z);
        float dist = dir.length();
        if (dist > 1e-4f) {
            dir.div(dist);
            float want = (float) Math.atan2(dir.x, dir.z);
            facing += wrapPi(want - facing) * Math.min(dt * 8f, 1f);
        }

        float damage = 0f;
        float intended = 0f;
        if (dist > STOP_RANGE) {
            Collide.slideXZ(pos, RADIUS, dir.x * RUN_SPEED * dt, dir.z * RUN_SPEED * dt, walls);
            intended = RUN_SPEED;
        } else if (hasLoS && gun.tryFire()) {
            damage = gun.damage();
        }
        animSpeed += (intended - animSpeed) * Math.min(dt * 8f, 1f);
        avatar.animate(animSpeed, dt);
        return damage;
    }

    public void render() {
        avatar.render(pos, facing, dead ? (float) (Math.PI / 2.0) : 0f);
    }

    public Avatar avatar() {
        return avatar;
    }

    private static float wrapPi(float a) {
        return (float) Math.atan2(Math.sin(a), Math.cos(a));
    }
}
