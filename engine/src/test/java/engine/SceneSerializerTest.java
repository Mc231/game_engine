package engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneSerializerTest {

    /** Builds a two-entity scene: a tinted cube mesh and a point light. */
    private static SceneData sampleScene() {
        ComponentData cube = new ComponentData(
                "mesh", "cube", "textures/crate.png",
                new float[]{1f, 0.5f, 0.25f}, 32f, null, null);
        EntityData crate = new EntityData(
                "Crate", new float[]{1f, 2f, 3f}, new float[]{0f, 0f, 0f},
                new float[]{1f, 1f, 1f}, List.of(cube));

        ComponentData light = new ComponentData(
                "light", null, null, null, 0f, "point", new float[]{0.9f, 0.8f, 0.7f});
        EntityData lamp = new EntityData(
                "Lamp", new float[]{0f, 5f, 0f}, null, null, List.of(light));

        return new SceneData(List.of(crate, lamp));
    }

    @Test
    void roundTripPreservesMeshEntity() {
        SceneData parsed = SceneSerializer.fromJson(SceneSerializer.toJson(sampleScene()));

        assertEquals(2, parsed.entities().size());

        EntityData crate = parsed.entities().get(0);
        assertEquals("Crate", crate.name());
        assertEquals(1f, crate.position()[0]);
        assertEquals(2f, crate.position()[1]);

        ComponentData mesh = crate.components().get(0);
        assertEquals("mesh", mesh.type());
        assertEquals("cube", mesh.mesh());
        assertEquals("textures/crate.png", mesh.texture());
        assertEquals(32f, mesh.shininess());
        assertEquals(1f, mesh.tint()[0]);
        assertEquals(0.5f, mesh.tint()[1]);
    }

    @Test
    void roundTripPreservesLightEntity() {
        SceneData parsed = SceneSerializer.fromJson(SceneSerializer.toJson(sampleScene()));

        EntityData lamp = parsed.entities().get(1);
        assertEquals("Lamp", lamp.name());

        ComponentData light = lamp.components().get(0);
        assertEquals("light", light.type());
        assertEquals("point", light.lightType());
        assertEquals(0.9f, light.color()[0]);
        assertEquals(0.7f, light.color()[2]);

        // Nullable fields untouched by the light component stay null after round-trip.
        assertNull(light.mesh());
        assertNull(light.texture());
    }

    @Test
    void jsonContainsExpectedKeys() {
        String json = SceneSerializer.toJson(sampleScene());

        assertNotNull(json);
        assertTrue(json.contains("\"entities\""), "expected an 'entities' key");
        assertTrue(json.contains("\"lightType\""), "expected a 'lightType' key");
        assertTrue(json.contains("\"shininess\""), "expected a 'shininess' key");
    }

    @Test
    void nullTransformsSurviveAsNullForConsumerDefaults() {
        // Lamp was built with null rotation/scale; the serializer must not invent values.
        SceneData parsed = SceneSerializer.fromJson(SceneSerializer.toJson(sampleScene()));

        EntityData lamp = parsed.entities().get(1);
        assertNull(lamp.rotation());
        assertNull(lamp.scale());
    }
}
