package engine;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A uniform spatial hash over the XZ plane for broad-phase queries. Static
 * colliders ({@link AABB}s) are bucketed into square cells once; {@link #nearby}
 * then returns only the colliders in the cells around a query point, so a city of
 * hundreds of buildings costs a handful of checks per frame instead of N.
 *
 * <p>Y is ignored — buildings are tall and the movers are on the ground — so this
 * is a 2D grid keyed by (cellX, cellZ). Pure and unit-testable (no GL).
 */
public class SpatialGrid {

    private final float cellSize;
    private final Map<Long, List<AABB>> cells = new HashMap<>();

    public SpatialGrid(float cellSize) {
        if (cellSize <= 0f) {
            throw new IllegalArgumentException("cellSize must be > 0");
        }
        this.cellSize = cellSize;
    }

    private static long key(int cx, int cz) {
        return (((long) cx) << 32) ^ (cz & 0xffffffffL);
    }

    private int cell(float v) {
        return (int) Math.floor(v / cellSize);
    }

    /** Bucket a collider into every cell its XZ footprint overlaps. */
    public void insert(AABB box) {
        int x0 = cell(box.min.x), x1 = cell(box.max.x);
        int z0 = cell(box.min.z), z1 = cell(box.max.z);
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                cells.computeIfAbsent(key(cx, cz), k -> new ArrayList<>()).add(box);
            }
        }
    }

    public void insertAll(Iterable<AABB> boxes) {
        for (AABB b : boxes) {
            insert(b);
        }
    }

    /**
     * Colliders within {@code radius} (XZ) of {@code (x, z)} — actually every
     * collider in any cell overlapping that square, de-duplicated. A conservative
     * broad-phase: callers still do the exact narrow-phase test.
     */
    public AABB[] nearby(float x, float z, float radius) {
        int x0 = cell(x - radius), x1 = cell(x + radius);
        int z0 = cell(z - radius), z1 = cell(z + radius);
        Set<AABB> out = new LinkedHashSet<>();
        for (int cx = x0; cx <= x1; cx++) {
            for (int cz = z0; cz <= z1; cz++) {
                List<AABB> bucket = cells.get(key(cx, cz));
                if (bucket != null) {
                    out.addAll(bucket);
                }
            }
        }
        return out.toArray(new AABB[0]);
    }

    /** Convenience overload keyed on a position vector. */
    public AABB[] nearby(Vector3f p, float radius) {
        return nearby(p.x, p.z, radius);
    }

    public float cellSize() {
        return cellSize;
    }
}
