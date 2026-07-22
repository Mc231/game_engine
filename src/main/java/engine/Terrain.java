package engine;

/**
 * A procedurally generated heightmap terrain. Builds a grid of
 * {@code resolution × resolution} vertices centered on the origin, spanning
 * {@code size × size} on the XZ plane, with heights from fractal Perlin noise
 * and per-vertex normals computed from neighboring heights.
 *
 * Vertex layout is position(3) + normal(3) — use with {@code new int[]{3, 3}}
 * (the {@link #mesh()} is built that way).
 */
public class Terrain implements Disposable {

    private final Mesh mesh;
    public final float size;
    public final float maxHeight;

    public Terrain(int resolution, float size, float maxHeight, long seed) {
        this.size = size;
        this.maxHeight = maxHeight;

        Noise noise = new Noise(seed);
        int n = resolution;
        float half = size / 2f;
        float step = size / (n - 1);

        // 1) Heights for the whole grid.
        float[] heights = new float[n * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float x = -half + j * step;
                float z = -half + i * step;
                heights[i * n + j] = heightAt(noise, x, z, maxHeight);
            }
        }

        // 2) Interleaved position + normal per vertex.
        float[] vertices = new float[n * n * 6];
        int v = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                float x = -half + j * step;
                float z = -half + i * step;
                float y = heights[i * n + j];

                // Normal from central differences of neighboring heights.
                float hl = heights[i * n + clamp(j - 1, n)];
                float hr = heights[i * n + clamp(j + 1, n)];
                float hd = heights[clamp(i - 1, n) * n + j];
                float hu = heights[clamp(i + 1, n) * n + j];
                float nx = hl - hr;
                float ny = 2f * step;
                float nz = hd - hu;
                float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);

                vertices[v++] = x;
                vertices[v++] = y;
                vertices[v++] = z;
                vertices[v++] = nx / len;
                vertices[v++] = ny / len;
                vertices[v++] = nz / len;
            }
        }

        // 3) Two triangles per grid cell.
        int[] indices = new int[(n - 1) * (n - 1) * 6];
        int t = 0;
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - 1; j++) {
                int a = i * n + j;
                int b = (i + 1) * n + j;
                int c = (i + 1) * n + j + 1;
                int d = i * n + j + 1;
                indices[t++] = a;
                indices[t++] = b;
                indices[t++] = c;
                indices[t++] = a;
                indices[t++] = c;
                indices[t++] = d;
            }
        }

        mesh = new Mesh(vertices, new int[]{3, 3}, indices);
    }

    /** Elevation at world (x, z): fBm remapped to 0..1 then sharpened into peaks. */
    private static float heightAt(Noise noise, float x, float z, float maxHeight) {
        double n = noise.fbm(x * 0.008, z * 0.008, 6, 2.0, 0.5);   // -1..1
        double t = (n + 1.0) * 0.5;                                 // 0..1
        t = Math.pow(t, 2.2);                                       // flat valleys, sharp peaks
        return (float) (t * maxHeight);
    }

    private static int clamp(int v, int n) {
        return v < 0 ? 0 : (v >= n ? n - 1 : v);
    }

    public Mesh mesh() {
        return mesh;
    }

    @Override
    public void dispose() {
        mesh.dispose();
    }
}
