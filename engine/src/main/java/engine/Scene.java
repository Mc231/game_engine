package engine;

/**
 * The unit of content the {@link Engine} drives. Implement this to define what
 * gets created, updated, and drawn. Per frame the engine calls, in order:
 * zero or more {@code fixedUpdate} steps, then {@code update}, then
 * {@code render}. {@code init} runs once when the scene becomes active,
 * {@code dispose} once when it is replaced or on shutdown, and {@code resize}
 * whenever the framebuffer size changes.
 */
public interface Scene {

    /** Create GPU resources (shaders, meshes). Called once, after GL is ready. */
    void init(Window window);

    /** Advance per-frame logic (input, camera). {@code deltaSeconds} varies with frame rate. */
    void update(float deltaSeconds);

    /** Issue draw calls for the current frame. */
    void render();

    /** Release GPU resources. Called once on shutdown. */
    void dispose();

    /**
     * Fixed-rate simulation step (physics, deterministic logic). Called zero or
     * more times per frame with a constant {@code step}; default is a no-op.
     */
    default void fixedUpdate(float step) {
    }

    /**
     * Framebuffer resized to {@code width}×{@code height} pixels. Rebuild any
     * size-dependent state (e.g. the projection matrix). Default is a no-op.
     */
    default void resize(int width, int height) {
    }

    /** Display name (used in the window title when scene-switching). */
    default String name() {
        return getClass().getSimpleName();
    }
}
