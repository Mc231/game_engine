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
    private final Map<String, int[]> padBindings = new HashMap<>();

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

    /**
     * Associates an action with one or more {@code GLFW_GAMEPAD_BUTTON_*}
     * codes, replacing any existing pad binding for that action. Independent
     * of {@link #bind}, so an action may carry both keyboard and pad bindings.
     *
     * @return this map, for chaining
     */
    public InputMap bindPad(String action, int... gamepadButtons) {
        padBindings.put(action, gamepadButtons.clone());
        return this;
    }

    /**
     * True if the action is active on either device: any bound key is down, or
     * any bound pad button is held. Unknown → false.
     */
    public boolean isDown(String action, Input input, Gamepad pad) {
        if (isDown(action, input)) {
            return true;
        }
        for (int button : padBindings.getOrDefault(action, NO_KEYS)) {
            if (pad.button(button)) {
                return true;
            }
        }
        return false;
    }

    /**
     * True if the action was triggered this frame on either device: any bound
     * key was pressed, or any bound pad button transitioned up→down. Unknown → false.
     */
    public boolean isPressed(String action, Input input, Gamepad pad) {
        if (isPressed(action, input)) {
            return true;
        }
        for (int button : padBindings.getOrDefault(action, NO_KEYS)) {
            if (pad.buttonPressed(button)) {
                return true;
            }
        }
        return false;
    }

    /** The pad button codes bound to the action, or an empty array if none. */
    public int[] padKeys(String action) {
        return padBindings.getOrDefault(action, NO_KEYS).clone();
    }
}
