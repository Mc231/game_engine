package engine;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsTest {

    @Test
    void defaults() {
        Settings s = Settings.defaults();
        assertEquals(1000, s.width);
        assertEquals(700, s.height);
        assertTrue(s.vsync);
        assertFalse(s.fullscreen);
        assertEquals(0.1f, s.mouseSensitivity, 0f);
        assertEquals(1.0f, s.masterVolume, 0f);
    }

    @Test
    void fromPropertiesReadsAllKeys() {
        Properties props = new Properties();
        props.setProperty("width", "1920");
        props.setProperty("height", "1080");
        props.setProperty("vsync", "false");
        props.setProperty("fullscreen", "true");
        props.setProperty("mouseSensitivity", "0.5");
        props.setProperty("masterVolume", "0.75");

        Settings s = Settings.fromProperties(props);
        assertEquals(1920, s.width);
        assertEquals(1080, s.height);
        assertFalse(s.vsync);
        assertTrue(s.fullscreen);
        assertEquals(0.5f, s.mouseSensitivity, 0f);
        assertEquals(0.75f, s.masterVolume, 0f);
    }

    @Test
    void missingKeysFallBackToDefaults() {
        Settings s = Settings.fromProperties(new Properties());
        assertEquals(1000, s.width);
        assertEquals(700, s.height);
        assertTrue(s.vsync);
        assertFalse(s.fullscreen);
        assertEquals(0.1f, s.mouseSensitivity, 0f);
        assertEquals(1.0f, s.masterVolume, 0f);
    }

    @Test
    void malformedValuesFallBackToDefaultsAndDoNotThrow() {
        Properties props = new Properties();
        props.setProperty("width", "abc");
        props.setProperty("height", "12.5");
        props.setProperty("mouseSensitivity", "not-a-float");
        props.setProperty("masterVolume", "");
        props.setProperty("vsync", "maybe");

        Settings s = Settings.fromProperties(props);
        assertEquals(1000, s.width);
        assertEquals(700, s.height);
        assertEquals(0.1f, s.mouseSensitivity, 0f);
        assertEquals(1.0f, s.masterVolume, 0f);
        assertTrue(s.vsync);
    }
}
