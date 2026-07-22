package engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MtlLoaderTest {

    @Test
    void parsesTwoMaterialsWithColorsShininessAndTexture() {
        String mtl = """
                newmtl red
                Kd 1 0 0
                Ns 96
                map_Kd textures/red.png
                newmtl blue
                Kd 0 0 1
                """;
        Map<String, MtlLoader.MaterialDef> mats = MtlLoader.parse(mtl);

        assertEquals(2, mats.size());

        MtlLoader.MaterialDef red = mats.get("red");
        assertEquals(new org.joml.Vector3f(1f, 0f, 0f), red.diffuse());
        assertEquals(96f, red.shininess(), 0f);
        assertEquals("textures/red.png", red.diffuseTexture());

        MtlLoader.MaterialDef blue = mats.get("blue");
        assertEquals(new org.joml.Vector3f(0f, 0f, 1f), blue.diffuse());
        // Defaults for a block with only Kd.
        assertEquals(32f, blue.shininess(), 0f);
        assertNull(blue.diffuseTexture());
    }

    @Test
    void nonPositiveShininessIsClampedToOne() {
        String mtl = """
                newmtl zero
                Ns 0
                newmtl negative
                Ns -5
                """;
        Map<String, MtlLoader.MaterialDef> mats = MtlLoader.parse(mtl);

        assertEquals(1f, mats.get("zero").shininess(), 0f);
        assertEquals(1f, mats.get("negative").shininess(), 0f);
    }

    @Test
    void mapKdUsesLastWhitespaceToken() {
        // Some exporters emit options before the filename.
        String mtl = """
                newmtl tex
                map_Kd -o 0 0 wood.png
                """;
        Map<String, MtlLoader.MaterialDef> mats = MtlLoader.parse(mtl);

        assertEquals("wood.png", mats.get("tex").diffuseTexture());
    }

    @Test
    void preservesInsertionOrder() {
        String mtl = """
                newmtl first
                newmtl second
                newmtl third
                """;
        Map<String, MtlLoader.MaterialDef> mats = MtlLoader.parse(mtl);

        assertEquals(java.util.List.of("first", "second", "third"),
                new java.util.ArrayList<>(mats.keySet()));
    }
}
