package engine;

import java.util.HashMap;
import java.util.Map;

/**
 * A named-action binding layer over {@link Input}. Maps human-readable action
 * names (e.g. {@code "jump"}) to one or more GLFW key codes, so game code can
 * query intent instead of raw keys.
 *
 * <pre>{@code
 * InputMap map = new InputMap()
 *         .bind("moveForward", GLFW_KEY_W, GLFW_KEY_UP)
 *         .bind("jump", GLFW_KEY_SPACE);
 *
 * if (map.isDown("moveForward", input)) { ... }
 * if (map.isPressed("jump", input))     { ... }
 * }</pre>
 */
public final class InputMap {

    private static final int[] NO_KEYS = new int[0];

    private final Map<String, int[]> bindings = new HashMap<>();

    /**
     * Associates an action with one or more GLFW key codes, replacing any
     * existing binding for that action.
     *
     * @return this map, for chaining
     */
    public InputMap bind(String action, int... glfwKeys) {
        bindings.put(action, glfwKeys.clone());
        return this;
    }

    /** True if any key bound to the action is currently down. Unknown → false. */
    public boolean isDown(String action, Input input) {
        for (int key : bindings.getOrDefault(action, NO_KEYS)) {
            if (input.isKeyDown(key)) {
                return true;
            }
        }
        return false;
    }

    /** True if any key bound to the action was pressed this frame. Unknown → false. */
    public boolean isPressed(String action, Input input) {
        for (int key : bindings.getOrDefault(action, NO_KEYS)) {
            if (input.isKeyPressed(key)) {
                return true;
            }
        }
        return false;
    }

    /** The key codes bound to the action, or an empty array if none. */
    public int[] keys(String action) {
        return bindings.getOrDefault(action, NO_KEYS).clone();
    }
}
