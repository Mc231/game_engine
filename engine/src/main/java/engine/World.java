package engine;

import java.util.ArrayList;
import java.util.List;

/**
 * The set of root {@link Entity}s making up a scene. Updating the world updates
 * every root, which recurses into its children; {@link #collect(Class)} gathers
 * components of a given type from across the whole hierarchy.
 */
public class World {

    private final List<Entity> roots = new ArrayList<>();

    /** Add a root entity and return it. */
    public Entity add(Entity root) {
        roots.add(root);
        return root;
    }

    public List<Entity> roots() {
        return roots;
    }

    /** Update every root, recursing into all descendants. */
    public void update(float dt) {
        for (Entity root : roots) {
            root.update(dt);
        }
    }

    /** Every component assignable to {@code type}, across all entities in the world. */
    public <T extends Component> List<T> collect(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Entity root : roots) {
            collect(root, type, result);
        }
        return result;
    }

    private static <T extends Component> void collect(Entity entity, Class<T> type, List<T> out) {
        T c = entity.get(type);
        if (c != null) {
            out.add(c);
        }
        for (Entity child : entity.children()) {
            collect(child, type, out);
        }
    }
}
