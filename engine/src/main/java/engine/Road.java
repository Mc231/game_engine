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

    /**
     * Generates a smooth, curvy closed-loop road that passes through a set of
     * waypoints using a Catmull-Rom spline.
     *
     * <p>The {@code waypoints} are treated as a closed loop (control indices wrap
     * modulo {@code n}). Only the x and z of each waypoint shape the path; the
     * emitted vertex height is {@code ground.heightAt(x, z) + yOffset}. Each span
     * from waypoint {@code i} to {@code i+1} is subdivided into
     * {@code segmentsPerSpan} rings (the next span's start ring is skipped to
     * avoid duplicate rings), for {@code n * segmentsPerSpan} rings total. Each
     * ring emits a left ({@code u = 0}) and right ({@code u = 1}) edge vertex,
     * {@code width} apart, with an up normal. UVs run 0..1 across the width and
     * tile along the length by centerline arc length.</p>
     *
     * @param waypoints        loop control points; at least 3 are expected
     * @param width            road width across
     * @param segmentsPerSpan  ring subdivisions per waypoint span
     * @param ground           terrain height sampler
     * @param yOffset          vertical lift above the sampled ground
     * @return a newly allocated GPU {@link Mesh}; the caller owns it and must
     *         {@link Mesh#dispose()} it.
     */
    public static Mesh spline(org.joml.Vector3f[] waypoints, float width, int segmentsPerSpan,
                              HeightSampler ground, float yOffset) {
        int n = waypoints.length;
        int totalRings = n * segmentsPerSpan;
        int floatsPerVertex = 3 + 3 + 2;
        float[] vertices = new float[totalRings * 2 * floatsPerVertex];
        int[] indices = new int[totalRings * 6];

        float half = width / 2f;
        float v = 0f;              // cumulative arc length / width
        float prevCx = 0f, prevCz = 0f;
        boolean hasPrev = false;

        int vi = 0;
        for (int i = 0; i < n; i++) {
            org.joml.Vector3f p0 = waypoints[(i - 1 + n) % n];
            org.joml.Vector3f p1 = waypoints[i];
            org.joml.Vector3f p2 = waypoints[(i + 1) % n];
            org.joml.Vector3f p3 = waypoints[(i + 2) % n];

            for (int j = 0; j < segmentsPerSpan; j++) {
                float t = (float) j / segmentsPerSpan;
                float t2 = t * t;
                float t3 = t2 * t;

                // Catmull-Rom position on XZ.
                float cx = 0.5f * (2f * p1.x
                        + (-p0.x + p2.x) * t
                        + (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2
                        + (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3);
                float cz = 0.5f * (2f * p1.z
                        + (-p0.z + p2.z) * t
                        + (2f * p0.z - 5f * p1.z + 4f * p2.z - p3.z) * t2
                        + (-p0.z + 3f * p1.z - 3f * p2.z + p3.z) * t3);

                // Accumulate centerline arc length between successive centers.
                if (hasPrev) {
                    float dx = cx - prevCx;
                    float dz = cz - prevCz;
                    v += (float) Math.sqrt(dx * dx + dz * dz) / width;
                }
                hasPrev = true;
                prevCx = cx;
                prevCz = cz;

                // Catmull-Rom tangent (derivative) on XZ, normalized.
                float tx = 0.5f * ((-p0.x + p2.x)
                        + (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * 2f * t
                        + (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * 3f * t2);
                float tz = 0.5f * ((-p0.z + p2.z)
                        + (2f * p0.z - 5f * p1.z + 4f * p2.z - p3.z) * 2f * t
                        + (-p0.z + 3f * p1.z - 3f * p2.z + p3.z) * 3f * t2);
                float tlen = (float) Math.sqrt(tx * tx + tz * tz);
                tx /= tlen;
                tz /= tlen;

                // Perpendicular on XZ, normalized.
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
        }

        // Two triangles per ring, wrapping closed at the end.
        int ti = 0;
        for (int k = 0; k < totalRings; k++) {
            int next = (k + 1) % totalRings;
            int lk = 2 * k;
            int rk = 2 * k + 1;
            int ln = 2 * next;
            int rn = 2 * next + 1;

            indices[ti++] = lk;
            indices[ti++] = rk;
            indices[ti++] = rn;

            indices[ti++] = lk;
            indices[ti++] = rn;
            indices[ti++] = ln;
        }

        return new Mesh(vertices, new int[]{3, 3, 2}, indices);
    }
}
