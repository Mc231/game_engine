package engine;

/**
 * The unit of content the {@link Engine} drives. Implement this to define what
 * gets created, updated, and drawn. The engine calls these in order:
 * {@code init} once, then {@code update}+{@code render} every frame, then
 * {@code dispose} on shutdown.
 */
public interface Scene {

    /** Create GPU resources (shaders, meshes). Called once, after GL is ready. */
    void init(Window window);

    /** Advance simulation. {@code deltaSeconds} is the time since the last frame. */
    void update(float deltaSeconds);

    /** Issue draw calls for the current frame. */
    void render();

    /** Release GPU resources. Called once on shutdown. */
    void dispose();

    /** Display name (used in the window title when scene-switching). */
    default String name() {
        return getClass().getSimpleName();
    }
}
