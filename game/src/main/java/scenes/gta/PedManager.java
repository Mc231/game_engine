package scenes.gta;

import engine.AABB;
import engine.Disposable;
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

    public void update(float dt, Vector3f playerThreat, Vector3f carThreat, Vector3f playerPos) {
        for (Pedestrian p : peds) {
            if (distSq(p.pos, playerPos) > DESPAWN * DESPAWN) {
                freeSpawn(playerPos, spawnPt);
                p.relocate(spawnPt.x, spawnPt.z);
            }
            p.update(dt, playerThreat, carThreat, city.wallsNear(p.pos, 12f));
        }
    }

    public void render() {
        for (Pedestrian p : peds) {
            p.render();
        }
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
