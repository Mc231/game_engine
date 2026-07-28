package scenes.gta;

import engine.AABB;
import engine.Geometry;
import engine.InstancedMesh;
import engine.Mesh;
import engine.SpatialGrid;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a deterministic grid city from a {@link Config}: streets and sidewalks
 * between blocks, blocks filled with instanced box buildings of varied footprint,
 * height, and tint, and one collider per building bucketed into a
 * {@link SpatialGrid}. The layout is centered on the origin, and with an even
 * block count the origin sits on a central road intersection (the spawn).
 *
 * <p>Runs in the scene's {@code init} (it creates GL meshes). Deterministic from
 * {@link Config#seed} — same seed, same city.
 */
public final class CityGenerator {

    private CityGenerator() {
    }

    /** Generation parameters (sensible defaults; tweak for size/density). */
    public static final class Config {
        public int blocksX = 4, blocksZ = 4;   // even → origin is a central intersection
        public float blockSize = 22f;
        public float roadWidth = 10f;
        public float sidewalk = 2.5f;          // margin between plot edge and street
        public float minHeight = 6f, maxHeight = 26f;
        public long seed = 1234L;
    }

    // A small palette of muted city tints; each building picks one (→ one draw batch).
    private static final Vector3f[] PALETTE = {
            new Vector3f(0.78f, 0.78f, 0.80f),
            new Vector3f(0.70f, 0.66f, 0.60f),
            new Vector3f(0.62f, 0.68f, 0.72f),
            new Vector3f(0.80f, 0.74f, 0.66f),
            new Vector3f(0.66f, 0.70f, 0.66f),
    };

    public static City generate(Config c) {
        Random rng = new Random(c.seed);
        float pitch = c.blockSize + c.roadWidth;
        float total = Math.max(c.blocksX, c.blocksZ) * pitch;
        float offX = -c.blocksX * pitch / 2f;
        float offZ = -c.blocksZ * pitch / 2f;

        List<Matrix4f> slabs = new ArrayList<>();
        List<List<Matrix4f>> byTint = new ArrayList<>();
        for (int i = 0; i < PALETTE.length; i++) {
            byTint.add(new ArrayList<>());
        }
        List<AABB> colliders = new ArrayList<>();

        for (int bi = 0; bi < c.blocksX; bi++) {
            for (int bj = 0; bj < c.blocksZ; bj++) {
                float bx = offX + (bi + 0.5f) * pitch;
                float bz = offZ + (bj + 0.5f) * pitch;

                // Sidewalk/lot slab covering the block (slightly raised = curb).
                slabs.add(new Matrix4f().translate(bx, 0.1f, bz).scale(c.blockSize, 0.2f, c.blockSize));

                // Subdivide the buildable interior into a 2×2 plot grid.
                float buildable = c.blockSize - 2f * c.sidewalk;
                float plot = buildable / 2f;
                for (int px = 0; px < 2; px++) {
                    for (int pz = 0; pz < 2; pz++) {
                        if (rng.nextFloat() < 0.18f) {
                            continue;   // occasional empty plot for variety
                        }
                        float cx = bx - buildable / 2f + (px + 0.5f) * plot;
                        float cz = bz - buildable / 2f + (pz + 0.5f) * plot;
                        float fw = plot * (0.62f + rng.nextFloat() * 0.26f);   // footprint
                        float fd = plot * (0.62f + rng.nextFloat() * 0.26f);
                        float h = c.minHeight + rng.nextFloat() * (c.maxHeight - c.minHeight);
                        float baseY = 0.2f;   // sit on top of the slab

                        byTint.get(rng.nextInt(PALETTE.length))
                                .add(new Matrix4f().translate(cx, baseY + h / 2f, cz).scale(fw, h, fd));
                        colliders.add(AABB.fromCenterSize(
                                new Vector3f(cx, baseY + h / 2f, cz), new Vector3f(fw, h, fd)));
                    }
                }
            }
        }

        // --- Build GL resources ---
        float[] cube = Geometry.cubeWithNormalsAndUV();
        int[] cubeIdx = new int[36];
        for (int i = 0; i < 36; i++) {
            cubeIdx[i] = i;
        }

        InstancedMesh sidewalkMesh = new InstancedMesh(cube, new int[]{3, 3, 2}, cubeIdx, slabs.toArray(new Matrix4f[0]));

        List<InstancedMesh> batches = new ArrayList<>();
        List<Vector3f> tints = new ArrayList<>();
        for (int i = 0; i < PALETTE.length; i++) {
            List<Matrix4f> group = byTint.get(i);
            if (group.isEmpty()) {
                continue;
            }
            batches.add(new InstancedMesh(cube, new int[]{3, 3, 2}, cubeIdx, group.toArray(new Matrix4f[0])));
            tints.add(PALETTE[i]);
        }

        float groundHalf = total / 2f + pitch;
        Mesh ground = new Mesh(Geometry.plane(groundHalf, groundHalf * 0.5f), new int[]{3, 3, 2});

        SpatialGrid grid = new SpatialGrid(pitch);
        grid.insertAll(colliders);

        // Spawn on the central road intersection; car just south of it on the road.
        Vector3f playerSpawn = new Vector3f(0f, 0f, 0f);
        Vector3f carSpawn = new Vector3f(0f, 0f, -pitch * 0.5f);

        return new City(ground, groundHalf, sidewalkMesh,
                batches.toArray(new InstancedMesh[0]), tints.toArray(new Vector3f[0]),
                colliders, grid, playerSpawn, carSpawn, 0f,
                pitch, c.blocksX, c.blocksZ, offX, offZ);
    }
}
