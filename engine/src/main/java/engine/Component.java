package engine;

/**
 * A unit of behaviour or data attached to an {@link Entity}. Subclasses override
 * {@link #onAttach()} to react to being added and {@link #update(float)} to run
 * per-frame logic. The owning entity is available via {@link #entity()}.
 */
public abstract class Component {

    protected Entity entity;

    /** Bind this component to its owner, then run {@link #onAttach()}. Called by {@link Entity#add}. */
    public final void attach(Entity e) {
        this.entity = e;
        onAttach();
    }

    /** Hook invoked once, right after this component is attached to its entity. */
    protected void onAttach() {}

    /** Per-frame update; {@code deltaSeconds} is the elapsed time since the last frame. */
    public void update(float deltaSeconds) {}

    /** The entity this component is attached to, or null if not yet attached. */
    public Entity entity() {
        return entity;
    }
}
