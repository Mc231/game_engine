package scenes.gta;

import engine.AABB;
import engine.Disposable;
import engine.Intersect;
import engine.Ray;
import engine.ShaderProgram;
import engine.Sound;
import engine.Texture;
import engine.Vision;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Spawns and drives police to match the wanted level: officers appear around the
 * target (up to a cap), chase and shoot, respawn as reinforcements when killed,
 * and are cleared on bust. Each officer owns its own {@link Avatar} (police
 * palette), created on spawn and disposed on removal (a handful at most).
 */
public class PoliceManager implements Disposable {

    private static final int MAX = 6;
    private static final float SPAWN_MIN = 28f, SPAWN_MAX = 55f;
    private static final float LOS_HALF_COS = (float) Math.cos(Math.toRadians(85));

    private final ShaderProgram shader;
    private final Texture white;
    private final City city;
    private final Sound gunSound;
    private final Random rng = new Random(4242L);
    private final List<Police> officers = new ArrayList<>();
    private final Vector3f spawnPt = new Vector3f();
    private boolean seesTarget;

    public PoliceManager(ShaderProgram shader, Texture white, City city, Sound gunSound) {
        this.shader = shader;
        this.white = white;
        this.city = city;
        this.gunSound = gunSound;
    }

    /** True if any live officer currently has line of sight to the target (drives wanted decay). */
    public boolean seesTarget() {
        return seesTarget;
    }

    /** Update the force to match {@code stars}; returns total damage dealt to the player. */
    public float update(float dt, int stars, Vector3f target) {
        // Remove finished corpses (dispose their avatars).
        for (Iterator<Police> it = officers.iterator(); it.hasNext(); ) {
            Police p = it.next();
            if (p.readyToRemove()) {
                p.avatar().dispose();
                it.remove();
            }
        }

        int alive = 0;
        for (Police p : officers) {
            if (!p.isDead()) {
                alive++;
            }
        }
        int wantCount = Math.min(stars, MAX);
        for (int i = alive; i < wantCount; i++) {
            spawn(target);
        }

        seesTarget = false;
        float damage = 0f;
        for (Police p : officers) {
            AABB[] walls = city.wallsNear(p.pos, 14f);
            boolean los = !p.isDead() && hasLineOfSight(p.pos, target);
            if (los) {
                seesTarget = true;
            }
            float dmg = p.update(dt, target, walls, los);
            if (dmg > 0f) {
                gunSound.play();
            }
            damage += dmg;
        }
        return damage;
    }

    /** Player shoots police: hitscan the nearest live officer, blocked by buildings. */
    public boolean shoot(Vector3f origin, Vector3f dir, float range, float damage, AABB[] walls) {
        Ray ray = new Ray(origin, dir);
        float wallT = range;
        if (walls != null) {
            for (AABB w : walls) {
                float t = Intersect.rayAABB(ray, w);
                if (t >= 0f && t < wallT) {
                    wallT = t;
                }
            }
        }
        Police best = null;
        float bestT = range;
        for (Police p : officers) {
            if (p.isDead()) {
                continue;
            }
            AABB box = AABB.fromCenterSize(new Vector3f(p.pos.x, 1f, p.pos.z), new Vector3f(1.1f, 2f, 1.1f));
            float t = Intersect.rayAABB(ray, box);
            if (t >= 0f && t < bestT && t < wallT) {
                bestT = t;
                best = p;
            }
        }
        if (best != null) {
            best.takeDamage(damage, origin);
            return true;
        }
        return false;
    }

    public void render() {
        for (Police p : officers) {
            p.render();
        }
    }

    /** Live officer positions (for the minimap). */
    public List<Vector3f> positions() {
        List<Vector3f> out = new ArrayList<>();
        for (Police p : officers) {
            if (!p.isDead()) {
                out.add(p.pos);
            }
        }
        return out;
    }

    /** Remove all officers (e.g. on bust / respawn). */
    public void clearAll() {
        for (Police p : officers) {
            p.avatar().dispose();
        }
        officers.clear();
        seesTarget = false;
    }

    private void spawn(Vector3f target) {
        for (int attempt = 0; attempt < 12; attempt++) {
            float a = rng.nextFloat() * (float) (Math.PI * 2.0);
            float d = SPAWN_MIN + rng.nextFloat() * (SPAWN_MAX - SPAWN_MIN);
            float x = target.x + (float) Math.sin(a) * d;
            float z = target.z + (float) Math.cos(a) * d;
            if (!insideBuilding(x, z)) {
                officers.add(new Police(new Avatar(shader, white, Avatar.police()), x, z));
                return;
            }
        }
    }

    private boolean insideBuilding(float x, float z) {
        for (AABB b : city.wallsNear(x, z, 2f)) {
            if (x >= b.min.x && x <= b.max.x && z >= b.min.z && z <= b.max.z) {
                return true;
            }
        }
        return false;
    }

    private boolean hasLineOfSight(Vector3f from, Vector3f to) {
        Vector3f fwd = new Vector3f(to.x - from.x, 0f, to.z - from.z);
        if (fwd.lengthSquared() < 1e-4f) {
            return true;
        }
        Vector3f eye = new Vector3f(from.x, 1.5f, from.z);
        Vector3f tgt = new Vector3f(to.x, 1.4f, to.z);
        return Vision.canSee(eye, fwd, LOS_HALF_COS, 30f, tgt, city.wallsNear(from, 32f));
    }

    @Override
    public void dispose() {
        clearAll();
    }
}
