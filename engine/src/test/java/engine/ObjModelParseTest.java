package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ObjModelParseTest {

    @Test
    void splitsFacesByMaterialInFirstAppearanceOrder() {
        String obj = """
                mtllib x.mtl
                v 0 0 0
                v 1 0 0
                v 0 1 0
                v 1 1 0
                vn 0 0 1
                usemtl red
                f 1//1 2//1 3//1
                usemtl blue
                f 2//1 4//1 3//1
                """;
        OBJLoader.ModelData model = OBJLoader.parseModel(obj);

        assertEquals("x.mtl", model.mtlLib());
        assertEquals(2, model.parts().size());

        OBJLoader.Part red = model.parts().get(0);
        OBJLoader.Part blue = model.parts().get(1);
        assertEquals("red", red.materialName());
        assertEquals("blue", blue.materialName());

        // Each part is an independent mesh: 3 unique vertices (8 floats each) and 3 indices.
        assertEquals(3 * 8, red.mesh().vertices().length);
        assertEquals(3, red.mesh().indices().length);
        assertEquals(3 * 8, blue.mesh().vertices().length);
        assertEquals(3, blue.mesh().indices().length);
        // Index numbering restarts at 0 per part.
        assertEquals(0, blue.mesh().indices()[0]);
    }

    @Test
    void groupsSameMaterialTogetherAcrossSections() {
        // "red" appears twice; it must remain a single part, and its dedup
        // should share the vertices so the second face reuses the first's data.
        String obj = """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                v 1 1 0
                vn 0 0 1
                usemtl red
                f 1//1 2//1 3//1
                usemtl blue
                f 1//1 2//1 4//1
                usemtl red
                f 1//1 3//1 4//1
                """;
        OBJLoader.ModelData model = OBJLoader.parseModel(obj);

        assertEquals(2, model.parts().size());
        assertEquals("red", model.parts().get(0).materialName());
        assertEquals("blue", model.parts().get(1).materialName());

        // red has faces (1,2,3) then (1,3,4): unique verts 1,2,3,4 => 4*8 floats, 6 indices.
        OBJLoader.Part red = model.parts().get(0);
        assertEquals(4 * 8, red.mesh().vertices().length);
        assertEquals(6, red.mesh().indices().length);
    }

    @Test
    void facesBeforeAnyUseMtlProduceNullMaterialPart() {
        String obj = """
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vn 0 0 1
                f 1//1 2//1 3//1
                """;
        OBJLoader.ModelData model = OBJLoader.parseModel(obj);

        assertNull(model.mtlLib());
        assertEquals(1, model.parts().size());
        assertNull(model.parts().get(0).materialName());
        assertEquals(3 * 8, model.parts().get(0).mesh().vertices().length);
        assertEquals(3, model.parts().get(0).mesh().indices().length);
    }

    @Test
    void ignoresGroupObjectAndSmoothingLines() {
        String obj = """
                o thing
                g group1
                s 1
                v 0 0 0
                v 1 0 0
                v 0 1 0
                vn 0 0 1
                usemtl red
                f 1//1 2//1 3//1
                """;
        OBJLoader.ModelData model = OBJLoader.parseModel(obj);

        assertEquals(1, model.parts().size());
        assertEquals("red", model.parts().get(0).materialName());
        assertEquals(3, model.parts().get(0).mesh().indices().length);
    }
}
