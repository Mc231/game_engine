package engine;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

/**
 * A node in the scene graph: a named object with its own {@link Transform}, a list
 * of {@link Component}s, and optional parent/child links. World placement composes
 * a node's local matrix with its parent's, so moving a parent moves its children.
 */
public class Entity {

    private String name;
    private final Transform transform = new Transform();
    private final List<Component> components = new ArrayList<>();
    private Entity parent;
    private final List<Entity> children = new ArrayList<>();

    public Entity() {
        this("Entity");
    }

    public Entity(String name) {
        this.name = name;
    }

    public Transform transform() {
        return transform;
    }

    public String name() {
        return name;
    }

    public Entity parent() {
        return parent;
    }

    public List<Entity> children() {
        return children;
    }

    /** Attach a component to this entity and return this entity for chaining. */
    public Entity add(Component c) {
        c.attach(this);
        components.add(c);
        return this;
    }

    /** The first component assignable to {@code type}, or null if none is present. */
    public <T extends Component> T get(Class<T> type) {
        for (Component c : components) {
            if (type.isInstance(c)) {
                return type.cast(c);
            }
        }
        return null;
    }

    /** Parent {@code child} under this entity and return this entity for chaining. */
    public Entity addChild(Entity child) {
        child.parent = this;
        children.add(child);
        return this;
    }

    /**
     * This entity's world matrix: its local matrix composed under every ancestor.
     * A fresh matrix is allocated so the reused internal {@link Transform#matrix()}
     * is never aliased across the recursion.
     */
    public Matrix4f worldMatrix() {
        if (parent == null) {
            return new Matrix4f(transform.matrix());
        }
        return parent.worldMatrix().mul(transform.matrix());
    }

    /** Update every component, then recurse into every child. */
    public void update(float dt) {
        for (Component c : components) {
            c.update(dt);
        }
        for (Entity child : children) {
            child.update(dt);
        }
    }
}
