package engine;

/**
 * Builds a closed-loop road ribbon that conforms to a terrain height field.
 *
 * The road is an oval loop on the XZ plane whose two edge rails are draped onto
 * whatever surface a {@link HeightSampler} describes, plus a small vertical
 * {@code yOffset} to keep it from z-fighting with the ground.
 *
 * <p>Vertex layout is position(3) + normal(3) + uv(2), matching
 * {@code new int[]{3, 3, 2}}.</p>
 */
public final class Road {

    private Road() {
    }

    /** Samples terrain elevation at a world-space (x, z) position. */
    @FunctionalInterface
    public interface HeightSampler {
        float heightAt(float x, float z);
    }

    /**
     * Generates a closed oval road loop draped over a height field.
     *
     * <p>The loop is centered on the origin, spanning {@code radiusX} and
     * {@code radiusZ} on the XZ plane, {@code width} across, subdivided into
     * {@code segments} steps. Each step contributes a left and right edge vertex
     * whose height is {@code ground.heightAt(x, z) + yOffset}. UVs run 0..1
     * across the width and tile along the length by centerline arc length.</p>
     *
     * @return a newly allocated GPU {@link Mesh}; the caller owns it and must
     *         {@link Mesh#dispose()} it.
     */
    public static Mesh loop(float radiusX, float radiusZ, float width, int segments,
                            HeightSampler ground, float yOffset) {
        int floatsPerVertex = 3 + 3 + 2;
        float[] vertices = new float[segments * 2 * floatsPerVertex];
        int[] indices = new int[segments * 6];

        float half = width / 2f;
        float v = 0f;              // cumulative arc length / width
        float prevCx = 0f, prevCz = 0f;
        boolean hasPrev = false;

        int vi = 0;
        for (int i = 0; i < segments; i++) {
            double a = 2.0 * Math.PI * i / segments;
            float cos = (float) Math.cos(a);
            float sin = (float) Math.sin(a);

            float cx = cos * radiusX;
            float cz = sin * radiusZ;

            // Accumulate centerline arc length between successive centers.
            if (hasPrev) {
                float dx = cx - prevCx;
                float dz = cz - prevCz;
                v += (float) Math.sqrt(dx * dx + dz * dz) / width;
            }
            hasPrev = true;
            prevCx = cx;
            prevCz = cz;

            // Tangent along the loop, normalized.
            float tx = -sin * radiusX;
            float tz = cos * radiusZ;
            float tlen = (float) Math.sqrt(tx * tx + tz * tz);
            tx /= tlen;
            tz /= tlen;

            // Side direction = normalize(cross(tangent, up)), up = (0,1,0).
            float px = tz;
            float pz = -tx;
            float plen = (float) Math.sqrt(px * px + pz * pz);
            px /= plen;
            pz /= plen;

            // Left edge (u = 0).
            float lx = cx - px * half;
            float lz = cz - pz * half;
            float ly = ground.heightAt(lx, lz) + yOffset;
            vertices[vi++] = lx;
            vertices[vi++] = ly;
            vertices[vi++] = lz;
            vertices[vi++] = 0f;
            vertices[vi++] = 1f;
            vertices[vi++] = 0f;
            vertices[vi++] = 0f;
            vertices[vi++] = v;

            // Right edge (u = 1).
            float rx = cx + px * half;
            float rz = cz + pz * half;
            float ry = ground.heightAt(rx, rz) + yOffset;
            vertices[vi++] = rx;
            vertices[vi++] = ry;
            vertices[vi++] = rz;
            vertices[vi++] = 0f;
            vertices[vi++] = 1f;
            vertices[vi++] = 0f;
            vertices[vi++] = 1f;
            vertices[vi++] = v;
        }

        // Two triangles per step, wrapping closed at the end.
        int ti = 0;
        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;
            int li = 2 * i;
            int ri = 2 * i + 1;
            int ln = 2 * next;
            int rn = 2 * next + 1;

            indices[ti++] = li;
            indices[ti++] = ri;
            indices[ti++] = rn;

            indices[ti++] = li;
            indices[ti++] = rn;
            indices[ti++] = ln;
        }

        return new Mesh(vertices, new int[]{3, 3, 2}, indices);
    }
}
