package engine;

/**
 * Seeded 2D Perlin noise with fractal (fBm) summation. Deterministic for a
 * given seed, so the same terrain is generated every run.
 */
public class Noise {

    private final int[] perm = new int[512];

    public Noise(long seed) {
        int[] p = new int[256];
        for (int i = 0; i < 256; i++) {
            p[i] = i;
        }
        // Fisher–Yates shuffle driven by a simple LCG (no Math.random → reproducible).
        long s = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
        for (int i = 255; i > 0; i--) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int j = (int) ((s >>> 33) % (i + 1));
            int t = p[i];
            p[i] = p[j];
            p[j] = t;
        }
        for (int i = 0; i < 512; i++) {
            perm[i] = p[i & 255];
        }
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double a, double b, double t) {
        return a + t * (b - a);
    }

    private static double grad(int hash, double x, double y) {
        int h = hash & 7;
        double u = h < 4 ? x : y;
        double v = h < 4 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    /** Raw Perlin noise, output roughly in [-1, 1]. */
    public double noise(double x, double y) {
        int xi = (int) Math.floor(x) & 255;
        int yi = (int) Math.floor(y) & 255;
        x -= Math.floor(x);
        y -= Math.floor(y);
        double u = fade(x);
        double v = fade(y);

        int aa = perm[perm[xi] + yi];
        int ab = perm[perm[xi] + yi + 1];
        int ba = perm[perm[xi + 1] + yi];
        int bb = perm[perm[xi + 1] + yi + 1];

        return lerp(
                lerp(grad(aa, x, y), grad(ba, x - 1, y), u),
                lerp(grad(ab, x, y - 1), grad(bb, x - 1, y - 1), u),
                v);
    }

    /**
     * Fractal Brownian motion: sum {@code octaves} layers of noise, each at
     * higher frequency ({@code lacunarity}) and lower amplitude ({@code gain}).
     * Returns roughly [-1, 1].
     */
    public double fbm(double x, double y, int octaves, double lacunarity, double gain) {
        double amplitude = 1.0;
        double frequency = 1.0;
        double sum = 0.0;
        double norm = 0.0;
        for (int i = 0; i < octaves; i++) {
            sum += amplitude * noise(x * frequency, y * frequency);
            norm += amplitude;
            amplitude *= gain;
            frequency *= lacunarity;
        }
        return sum / norm;
    }
}
