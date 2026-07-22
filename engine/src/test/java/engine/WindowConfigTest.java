package engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowConfigTest {

    @Test
    void defaults() {
        WindowConfig c = WindowConfig.builder().build();
        assertEquals(800, c.width);
        assertEquals(600, c.height);
        assertEquals("LWJGL", c.title);
        assertTrue(c.resizable);
        assertTrue(c.vsync);
        assertEquals(3, c.glMajor);
        assertEquals(3, c.glMinor);
        assertEquals(0.1f, c.clearR, 0f);
        assertEquals(1f, c.clearA, 0f);
    }

    @Test
    void buildersOverrideDefaults() {
        WindowConfig c = WindowConfig.builder()
                .size(1280, 720)
                .title("Game")
                .resizable(false)
                .vsync(false)
                .glVersion(4, 1)
                .clearColor(0.2f, 0.3f, 0.4f, 0.5f)
                .build();
        assertEquals(1280, c.width);
        assertEquals(720, c.height);
        assertEquals("Game", c.title);
        assertFalse(c.resizable);
        assertFalse(c.vsync);
        assertEquals(4, c.glMajor);
        assertEquals(1, c.glMinor);
        assertEquals(0.2f, c.clearR, 0f);
        assertEquals(0.3f, c.clearG, 0f);
        assertEquals(0.4f, c.clearB, 0f);
        assertEquals(0.5f, c.clearA, 0f);
    }

    @Test
    void eachBuilderIsIndependent() {
        WindowConfig a = WindowConfig.builder().size(100, 100).build();
        WindowConfig b = WindowConfig.builder().build();
        assertEquals(100, a.width);
        assertEquals(800, b.width);
    }
}
