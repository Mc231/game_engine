package engine;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LightTest {

    @Test
    void directionalFactory() {
        Light l = Light.directional(new Vector3f(0, -1, 0), new Vector3f(1, 1, 1));
        assertEquals(Light.Type.DIRECTIONAL, l.type);
        assertEquals(new Vector3f(0, -1, 0), l.direction);
        assertEquals(new Vector3f(1, 1, 1), l.color);
    }

    @Test
    void pointFactoryHasDefaultAttenuation() {
        Light l = Light.point(new Vector3f(2, 3, 4), new Vector3f(1, 0, 0));
        assertEquals(Light.Type.POINT, l.type);
        assertEquals(new Vector3f(2, 3, 4), l.position);
        assertEquals(1.0f, l.constant, 0f);
        assertEquals(0.09f, l.linear, 0f);
        assertEquals(0.032f, l.quadratic, 0f);
    }

    @Test
    void spotFactory() {
        Light l = Light.spot(new Vector3f(1, 1, 1), new Vector3f(0, 0, -1), new Vector3f(1, 1, 0.9f));
        assertEquals(Light.Type.SPOT, l.type);
        assertEquals(new Vector3f(1, 1, 1), l.position);
        assertEquals(new Vector3f(0, 0, -1), l.direction);
    }

    @Test
    void setConeConvertsDegreesToCosines() {
        Light l = Light.spot(new Vector3f(), new Vector3f(0, 0, -1), new Vector3f(1, 1, 1))
                .setCone(30f, 45f);
        assertEquals((float) Math.cos(Math.toRadians(30)), l.cutOff, 1e-6f);
        assertEquals((float) Math.cos(Math.toRadians(45)), l.outerCutOff, 1e-6f);
        // Inner cone cosine is larger than outer (smaller angle → larger cosine).
        assertTrue(l.cutOff > l.outerCutOff);
    }
}
