package engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
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
 * {@code v/vt/vn} and {@code v//vn}. It does not handle materials or groups.
 */
public final class OBJLoader {

    /** Parsed geometry ready for {@code new Mesh(vertices, {3,3,2}, indices)}. */
    public record MeshData(float[] vertices, int[] indices) {
    }

    private OBJLoader() {
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

        List<Float> vertexData = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        Map<String, Integer> uniqueVertices = new HashMap<>();

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
                case "f" -> {
                    // Fan-triangulate: (0,1,2), (0,2,3), ...
                    for (int i = 2; i < parts.length - 1; i++) {
                        addVertex(parts[1], positions, texCoords, normals, vertexData, indices, uniqueVertices);
                        addVertex(parts[i], positions, texCoords, normals, vertexData, indices, uniqueVertices);
                        addVertex(parts[i + 1], positions, texCoords, normals, vertexData, indices, uniqueVertices);
                    }
                }
                default -> { /* ignore unsupported lines */ }
            }
        }

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

    private static void addVertex(String ref,
                                  List<float[]> positions, List<float[]> texCoords, List<float[]> normals,
                                  List<Float> vertexData, List<Integer> indices, Map<String, Integer> unique) {
        Integer existing = unique.get(ref);
        if (existing != null) {
            indices.add(existing);
            return;
        }

        // ref is "v", "v/vt", "v//vn", or "v/vt/vn" (1-based, negatives allowed).
        String[] p = ref.split("/", -1);
        float[] pos = positions.get(resolve(p[0], positions.size()));
        float[] uv = (p.length > 1 && !p[1].isEmpty()) ? texCoords.get(resolve(p[1], texCoords.size())) : new float[]{0f, 0f};
        float[] nrm = (p.length > 2 && !p[2].isEmpty()) ? normals.get(resolve(p[2], normals.size())) : new float[]{0f, 0f, 0f};

        vertexData.add(pos[0]);
        vertexData.add(pos[1]);
        vertexData.add(pos[2]);
        vertexData.add(nrm[0]);
        vertexData.add(nrm[1]);
        vertexData.add(nrm[2]);
        vertexData.add(uv[0]);
        vertexData.add(uv[1]);

        int index = unique.size();
        unique.put(ref, index);
        indices.add(index);
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
