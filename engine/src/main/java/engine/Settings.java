package engine;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Immutable user settings (resolution, vsync, input, audio) loaded from a
 * {@code .properties} source. {@link #fromProperties(Properties)} is the pure,
 * testable seam: every key falls back to its default when absent or unparseable,
 * and parsing never throws.
 */
public final class Settings {

    /** Default values, mirroring the documented engine defaults. */
    public static final int DEFAULT_WIDTH = 1000;
    public static final int DEFAULT_HEIGHT = 700;
    public static final boolean DEFAULT_VSYNC = true;
    public static final boolean DEFAULT_FULLSCREEN = false;
    public static final float DEFAULT_MOUSE_SENSITIVITY = 0.1f;
    public static final float DEFAULT_MASTER_VOLUME = 1.0f;

    public final int width;
    public final int height;
    public final boolean vsync;
    public final boolean fullscreen;
    public final float mouseSensitivity;
    public final float masterVolume;

    public Settings(int width, int height, boolean vsync, boolean fullscreen,
                    float mouseSensitivity, float masterVolume) {
        this.width = width;
        this.height = height;
        this.vsync = vsync;
        this.fullscreen = fullscreen;
        this.mouseSensitivity = mouseSensitivity;
        this.masterVolume = masterVolume;
    }

    /** A {@code Settings} with every field at its documented default. */
    public static Settings defaults() {
        return new Settings(DEFAULT_WIDTH, DEFAULT_HEIGHT, DEFAULT_VSYNC,
                DEFAULT_FULLSCREEN, DEFAULT_MOUSE_SENSITIVITY, DEFAULT_MASTER_VOLUME);
    }

    /**
     * Builds settings from the given properties. Each key is parsed defensively:
     * a missing or malformed value falls back to that key's default and never throws.
     */
    public static Settings fromProperties(Properties props) {
        return new Settings(
                parseInt(props, "width", DEFAULT_WIDTH),
                parseInt(props, "height", DEFAULT_HEIGHT),
                parseBoolean(props, "vsync", DEFAULT_VSYNC),
                parseBoolean(props, "fullscreen", DEFAULT_FULLSCREEN),
                parseFloat(props, "mouseSensitivity", DEFAULT_MOUSE_SENSITIVITY),
                parseFloat(props, "masterVolume", DEFAULT_MASTER_VOLUME));
    }

    /**
     * Loads a {@code .properties} classpath resource and delegates to
     * {@link #fromProperties(Properties)}. Returns {@link #defaults()} when the
     * resource is missing or cannot be read; never throws.
     */
    public static Settings load(String classpathResource) {
        try (InputStream in = Settings.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) {
                Log.info("Settings resource not found, using defaults: " + classpathResource);
                return defaults();
            }
            Properties props = new Properties();
            props.load(in);
            return fromProperties(props);
        } catch (IOException e) {
            Log.warn("Failed to read settings resource, using defaults: " + classpathResource);
            return defaults();
        }
    }

    private static int parseInt(Properties props, String key, int fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static float parseFloat(Properties props, String key, float fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static boolean parseBoolean(Properties props, String key, boolean fallback) {
        String value = props.getProperty(key);
        if (value == null) {
            return fallback;
        }
        value = value.trim();
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        return fallback;
    }
}
