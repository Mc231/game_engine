package scenes.gta;

import engine.AABB;
import engine.Disposable;
import engine.Intersect;
import engine.Ray;
import engine.ShaderProgram;
import engine.Texture;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Maintains a pool of {@link Pedestrian}s around the player: spawns them on free
 * ground nearby and recycles any that wander too far, so a small active set gives
 * the impression of a populated city (population streaming). Each ped owns its own
 * {@link Avatar} (varied palette) — a few dozen small meshes is cheap.
 */
public class PedManager implements Disposable {

    private static final float SPAWN_MIN = 18f, SPAWN_MAX = 70f;
    private static final float DESPAWN = 95f;   // recycle beyond this distance from the player

    private final List<Pedestrian> peds = new ArrayList<>();
    private final List<Avatar> avatars = new ArrayList<>();
    private final City city;
    private final Random rng;
    private final Vector3f spawnPt = new Vector3f();

    public PedManager(ShaderProgram shader, Texture white, City city, int count, long seed) {
        this.city = city;
        this.rng = new Random(seed);
        for (int i = 0; i < count; i++) {
            Avatar a = new Avatar(shader, white, Avatar.civilian(rng));
            avatars.add(a);
            freeSpawn(city.playerSpawn, spawnPt);
            peds.add(new Pedestrian(a, spawnPt.x, spawnPt.z, rng));
        }
    }

    private static final float HIT_RADIUS = 2.2f;   // car body + ped
    private static final float HIT_SPEED = 2.5f;    // min car speed to knock a ped down

    /**
     * @param anchor        position to keep the population around (player or car)
     * @param carPos        the player's car (for run-over detection)
     * @param carForward    the car's forward direction (knockback)
     * @param carSpeed      the car's signed speed
     */
    public int update(float dt, Vector3f playerThreat, Vector3f carThreat, Vector3f anchor,
                      Vector3f carPos, Vector3f carForward, float carSpeed) {
        boolean carDangerous = Math.abs(carSpeed) > HIT_SPEED;
        int newHits = 0;
        for (Pedestrian p : peds) {
            if (p.readyToRecycle() || distSq(p.pos, anchor) > DESPAWN * DESPAWN) {
                freeSpawn(anchor, spawnPt);
                p.relocate(spawnPt.x, spawnPt.z);
            }
            p.update(dt, playerThreat, carThreat, city.wallsNear(p.pos, 12f));

            if (carDangerous && !p.isDown() && distSq(p.pos, carPos) < HIT_RADIUS * HIT_RADIUS) {
                p.hit(carForward, carSpeed);
                newHits++;
            }
        }
        return newHits;
    }

    public void render() {
        for (Pedestrian p : peds) {
            p.render();
        }
    }

    /**
     * Hitscan the nearest live pedestrian along the ray, respecting building
     * blockers; applies {@code damage} to it. Returns true if a ped was hit.
     */
    public boolean shoot(Vector3f origin, Vector3f dir, float range, float damage, AABB[] walls) {
        Ray ray = new Ray(origin, dir);

        float wallT = range;   // nearest building along the shot blocks it
        if (walls != null) {
            for (AABB w : walls) {
                float t = Intersect.rayAABB(ray, w);
                if (t >= 0f && t < wallT) {
                    wallT = t;
                }
            }
        }

        Pedestrian best = null;
        float bestT = range;
        for (Pedestrian p : peds) {
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
            best.takeDamage(damage, dir);
            return true;
        }
        return false;
    }

    /** Current pedestrian positions (fresh array), e.g. as traffic hazards. */
    public Vector3f[] positions() {
        Vector3f[] out = new Vector3f[peds.size()];
        for (int i = 0; i < peds.size(); i++) {
            out[i] = peds.get(i).pos;
        }
        return out;
    }

    /** Pick a point ringed around {@code around} that isn't inside a building. */
    private void freeSpawn(Vector3f around, Vector3f out) {
        for (int attempt = 0; attempt < 12; attempt++) {
            float a = rng.nextFloat() * (float) (Math.PI * 2.0);
            float d = SPAWN_MIN + rng.nextFloat() * (SPAWN_MAX - SPAWN_MIN);
            float x = around.x + (float) Math.sin(a) * d;
            float z = around.z + (float) Math.cos(a) * d;
            if (!insideBuilding(x, z)) {
                out.set(x, 0f, z);
                return;
            }
        }
        out.set(around.x, 0f, around.z);   // give up: drop near the anchor
    }

    private boolean insideBuilding(float x, float z) {
        AABB[] near = city.wallsNear(x, z, 2f);
        for (AABB b : near) {
            if (x >= b.min.x && x <= b.max.x && z >= b.min.z && z <= b.max.z) {
                return true;
            }
        }
        return false;
    }

    private static float distSq(Vector3f a, Vector3f b) {
        float dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    @Override
    public void dispose() {
        for (Avatar a : avatars) {
            a.dispose();
        }
    }
}
