package engine;

import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Produces per-instance model matrices scattered over a square area on a terrain,
 * optionally skipping an excluded region (e.g. a road). Deterministic for a given
 * seed and pure (no OpenGL).
 *
 * <pre>{@code
 * Matrix4f[] trees = Scatter.onArea(
 *         200, 250f, 42L, 0.8f, 1.4f,
 *         terrain::heightAt,
 *         (x, z) -> Math.abs(x) < 8f);   // skip an 8-wide road along z
 * }</pre>
 */
public final class Scatter {

    private Scatter() {
    }

    /** Samples terrain height at a world position. */
    @FunctionalInterface
    public interface HeightSampler {
        float heightAt(float x, float z);
    }

    /** Tests a world position; return {@code true} to SKIP that spot. */
    @FunctionalInterface
    public interface Exclude {
        boolean excluded(float x, float z);
    }

    /**
     * Scatters up to {@code count} items over {@code [-halfExtent, halfExtent]} on x and z.
     * Each spot gets a random yaw and a uniform scale in {@code [minScale, maxScale]};
     * its height comes from {@code ground}. Spots for which {@code exclude} returns true are
     * skipped, so the result length may be {@code < count}. Total attempts are capped at
     * {@code count * 8} to guarantee termination when the exclude is very dense.
     *
     * @param exclude may be null, in which case nothing is excluded
     * @return the placed matrices, in placement order (length &le; count)
     */
    public static Matrix4f[] onArea(int count, float halfExtent, long seed,
                                    float minScale, float maxScale,
                                    HeightSampler ground, Exclude exclude) {
        Random random = new Random(seed);
        List<Matrix4f> matrices = new ArrayList<>(count);
        int maxAttempts = count * 8;

        for (int attempt = 0, placed = 0; placed < count && attempt < maxAttempts; attempt++) {
            float x = uniform(random, -halfExtent, halfExtent);
            float z = uniform(random, -halfExtent, halfExtent);
            if (exclude != null && exclude.excluded(x, z)) {
                continue;
            }
            float y = ground.heightAt(x, z);
            float ry = uniform(random, 0f, (float) (2 * Math.PI));
            float s = uniform(random, minScale, maxScale);
            matrices.add(new Matrix4f().translate(x, y, z).rotateY(ry).scale(s));
            placed++;
        }
        return matrices.toArray(new Matrix4f[0]);
    }

    private static float uniform(Random random, float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
