package engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A minimal Wavefront .OBJ loader. Reads positions (v), texture coords (vt),
 * and normals (vn), then expands faces into a vertex buffer laid out as
 * position(3) + normal(3) + uv(2), with an index buffer.
 *
 * Parsing ({@link #parse}) is separated from GPU upload ({@link #load}) so the
 * parser can be unit-tested without an OpenGL context.
 *
 * Supports triangles and convex polygons (fan-triangulated) and face formats
 * {@code v/vt/vn} and {@code v//vn}. {@link #parse} merges everything into one
 * mesh; {@link #parseModel} splits faces by {@code usemtl} into per-material
 * {@link Part}s (see {@link Model} + {@link MtlLoader} for the GPU side).
 */
public final class OBJLoader {

    /** Parsed geometry ready for {@code new Mesh(vertices, {3,3,2}, indices)}. */
    public record MeshData(float[] vertices, int[] indices) {
    }

    /** One contiguous run of geometry associated with a material ({@code materialName} may be null). */
    public record Part(String materialName, MeshData mesh) {
    }

    /** A whole .obj split into per-material {@link Part}s. {@code mtlLib} may be null. */
    public record ModelData(java.util.List<Part> parts, String mtlLib) {
    }

    private OBJLoader() {
    }

    /**
     * Per-part vertex accumulator: its own interleaved vertex buffer, index list,
     * and dedup map (index numbering starts at 0). Shared by {@link #parse} and
     * {@link #parseModel} so the vertex/dedup logic lives in exactly one place.
     */
    private static final class Builder {
        final List<Float> vertexData = new ArrayList<>();
        final List<Integer> indices = new ArrayList<>();
        final Map<String, Integer> uniqueVertices = new HashMap<>();

        MeshData build() {
            float[] vertices = new float[vertexData.size()];
            for (int i = 0; i < vertices.length; i++) {
                vertices[i] = vertexData.get(i);
            }
            int[] indexArray = new int[indices.size()];
            for (int i = 0; i < indexArray.length; i++) {
                indexArray[i] = indices.get(i);
            }
            return new MeshData(vertices, indexArray);
        }
    }

    /** Load an .obj from the classpath into a GPU {@link Mesh}. */
    public static Mesh load(String resourcePath) {
        MeshData data = parse(readResource(resourcePath));
        return new Mesh(data.vertices(), new int[]{3, 3, 2}, data.indices());
    }

    /** Parse .obj text into interleaved vertex data + indices. No OpenGL needed. */
    public static MeshData parse(String objText) {
        List<float[]> positions = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        Builder builder = new Builder();

        for (String raw : objText.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "v" -> positions.add(new float[]{
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                case "vt" -> texCoords.add(new float[]{
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
                case "vn" -> normals.add(new float[]{
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                case "f" -> triangulateFace(parts, positions, texCoords, normals, builder);
                default -> { /* ignore unsupported lines */ }
            }
        }

        return builder.build();
    }

    /**
     * Parse .obj text into per-material {@link Part}s. Positions/texcoords/normals
     * are shared across the whole file; faces are grouped by the currently active
     * {@code usemtl} material (null before the first {@code usemtl}). Each distinct
     * material gets its own independent {@link Builder}, and parts are ordered by
     * first appearance. The first {@code mtllib} token is captured into
     * {@link ModelData#mtlLib()} (null if absent).
     */
    public static ModelData parseModel(String objText) {
        List<float[]> positions = new ArrayList<>();
        List<float[]> texCoords = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();

        // Insertion-ordered so parts come out in first-appearance order.
        Map<String, Builder> byMaterial = new LinkedHashMap<>();
        String mtlLib = null;
        String currentMaterial = null;

        for (String raw : objText.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\s+");
            switch (parts[0]) {
                case "v" -> positions.add(new float[]{
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                case "vt" -> texCoords.add(new float[]{
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2])});
                case "vn" -> normals.add(new float[]{
                        Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3])});
                case "mtllib" -> {
                    if (mtlLib == null && parts.length > 1) {
                        mtlLib = parts[1];
                    }
                }
                case "usemtl" -> currentMaterial = parts.length > 1 ? parts[1] : null;
                case "f" -> {
                    // LinkedHashMap forbids null keys, so key faces-before-usemtl separately.
                    Builder builder = byMaterial.computeIfAbsent(
                            currentMaterial == null ? NULL_MATERIAL_KEY : currentMaterial,
                            k -> new Builder());
                    triangulateFace(parts, positions, texCoords, normals, builder);
                }
                default -> { /* ignore g, o, s and other unsupported lines */ }
            }
        }

        List<Part> result = new ArrayList<>();
        for (Map.Entry<String, Builder> e : byMaterial.entrySet()) {
            String name = NULL_MATERIAL_KEY.equals(e.getKey()) ? null : e.getKey();
            result.add(new Part(name, e.getValue().build()));
        }
        return new ModelData(result, mtlLib);
    }

    /** Sentinel key for the material-less part (LinkedHashMap disallows null keys). */
    private static final String NULL_MATERIAL_KEY = "\0__no_material__\0";

    /** Fan-triangulate a face line: (0,1,2), (0,2,3), ... into the given builder. */
    private static void triangulateFace(String[] parts,
                                        List<float[]> positions, List<float[]> texCoords, List<float[]> normals,
                                        Builder builder) {
        for (int i = 2; i < parts.length - 1; i++) {
            addVertex(parts[1], positions, texCoords, normals, builder);
            addVertex(parts[i], positions, texCoords, normals, builder);
            addVertex(parts[i + 1], positions, texCoords, normals, builder);
        }
    }

    private static void addVertex(String ref,
                                  List<float[]> positions, List<float[]> texCoords, List<float[]> normals,
                                  Builder builder) {
        Integer existing = builder.uniqueVertices.get(ref);
        if (existing != null) {
            builder.indices.add(existing);
            return;
        }

        // ref is "v", "v/vt", "v//vn", or "v/vt/vn" (1-based, negatives allowed).
        String[] p = ref.split("/", -1);
        float[] pos = positions.get(resolve(p[0], positions.size()));
        float[] uv = (p.length > 1 && !p[1].isEmpty()) ? texCoords.get(resolve(p[1], texCoords.size())) : new float[]{0f, 0f};
        float[] nrm = (p.length > 2 && !p[2].isEmpty()) ? normals.get(resolve(p[2], normals.size())) : new float[]{0f, 0f, 0f};

        builder.vertexData.add(pos[0]);
        builder.vertexData.add(pos[1]);
        builder.vertexData.add(pos[2]);
        builder.vertexData.add(nrm[0]);
        builder.vertexData.add(nrm[1]);
        builder.vertexData.add(nrm[2]);
        builder.vertexData.add(uv[0]);
        builder.vertexData.add(uv[1]);

        int index = builder.uniqueVertices.size();
        builder.uniqueVertices.put(ref, index);
        builder.indices.add(index);
    }

    /** OBJ indices are 1-based; negatives count back from the end. */
    private static int resolve(String token, int count) {
        int i = Integer.parseInt(token);
        return i < 0 ? count + i : i - 1;
    }

    private static String readResource(String path) {
        try (InputStream in = OBJLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Model resource not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read model: " + path, e);
        }
    }
}
