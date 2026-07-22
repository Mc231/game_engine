package engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent entry point for a game: configure the window and register scenes in
 * one place, then {@link #run()}. A thin convenience over {@link WindowConfig}
 * + {@link Engine}, so a game is defined without touching the engine internals.
 *
 * <pre>{@code
 * Application.create()
 *     .title("My Game").size(1280, 720)
 *     .scene(new MenuScene())
 *     .scene(new PlayScene())
 *     .run();
 * }</pre>
 */
public final class Application {

    private final WindowConfig.Builder config = WindowConfig.builder();
    private final List<Scene> scenes = new ArrayList<>();

    private Application() {
    }

    public static Application create() {
        return new Application();
    }

    public Application title(String title) {
        config.title(title);
        return this;
    }

    public Application size(int width, int height) {
        config.size(width, height);
        return this;
    }

    public Application vsync(boolean vsync) {
        config.vsync(vsync);
        return this;
    }

    public Application resizable(boolean resizable) {
        config.resizable(resizable);
        return this;
    }

    public Application glVersion(int major, int minor) {
        config.glVersion(major, minor);
        return this;
    }

    public Application clearColor(float r, float g, float b, float a) {
        config.clearColor(r, g, b, a);
        return this;
    }

    /** Register a scene. Scenes are selectable at runtime in registration order. */
    public Application scene(Scene scene) {
        scenes.add(scene);
        return this;
    }

    public Application scenes(Scene... toAdd) {
        for (Scene s : toAdd) {
            scenes.add(s);
        }
        return this;
    }

    public void run() {
        if (scenes.isEmpty()) {
            throw new IllegalStateException("No scenes registered");
        }
        new Engine(config.build(), List.copyOf(scenes)).run();
    }
}
