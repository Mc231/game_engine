package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class OBJLoaderTest {

    @Test
    void parsesSingleTriangle() {
        String obj = """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vt 0 0
                vt 1 0
                vt 0 1
                vn 0 0 1
                f 1/1/1 2/2/1 3/3/1
                """;
        OBJLoader.MeshData data = OBJLoader.parse(obj);

        // 3 unique vertices, 8 floats each (pos3 + normal3 + uv2).
        assertEquals(3 * 8, data.vertices().length);
        assertArrayEquals(new int[]{0, 1, 2}, data.indices());

        // First vertex: pos (0,0,0), normal (0,0,1), uv (0,0).
        assertArrayEquals(new float[]{0, 0, 0, 0, 0, 1, 0, 0},
                slice(data.vertices(), 0, 8), 0f);
    }

    @Test
    void quadIsFanTriangulatedAndDeduplicated() {
        // A quad: two triangles sharing vertices 1 and 3.
        String obj = """
                v 0 0 0
                v 1 0 0
                v 1 1 0
                v 0 1 0
                vt 0 0
                vt 1 0
                vt 1 1
                vt 0 1
                vn 0 0 1
                f 1/1/1 2/2/1 3/3/1 4/4/1
                """;
        OBJLoader.MeshData data = OBJLoader.parse(obj);

        // 4 unique vertices (shared corners reused), 6 indices (2 triangles).
        assertEquals(4 * 8, data.vertices().length);
        assertArrayEquals(new int[]{0, 1, 2, 0, 2, 3}, data.indices());
    }

    @Test
    void handlesDoubleSlashFormatAndMissingUv() {
        // v//vn form: no texture coords.
        String obj = """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vn 0 1 0
                f 1//1 2//1 3//1
                """;
        OBJLoader.MeshData data = OBJLoader.parse(obj);

        assertEquals(3 * 8, data.vertices().length);
        // Second vertex: pos (1,0,0), normal (0,1,0), uv defaults to (0,0).
        assertArrayEquals(new float[]{1, 0, 0, 0, 1, 0, 0, 0},
                slice(data.vertices(), 8, 8), 0f);
    }

    @Test
    void ignoresCommentsAndBlankLines() {
        String obj = """
                # a comment

                v 0 0 0
                v 1 0 0
                v 0 1 0
                vn 0 0 1
                # another comment
                f 1//1 2//1 3//1
                """;
        OBJLoader.MeshData data = OBJLoader.parse(obj);
        assertEquals(3, data.indices().length);
    }

    private static float[] slice(float[] a, int from, int len) {
        float[] out = new float[len];
        System.arraycopy(a, from, out, 0, len);
        return out;
    }
}
