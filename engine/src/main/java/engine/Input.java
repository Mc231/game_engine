package engine;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Per-frame input state for one window. Keys are polled on demand; mouse
 * movement is tracked as a delta refreshed once per frame by {@link #update()}
 * (the engine calls that for you).
 */
public class Input {

    private final long window;

    private final double[] xBuffer = new double[1];
    private final double[] yBuffer = new double[1];
    private double lastX;
    private double lastY;
    private float mouseDeltaX;
    private float mouseDeltaY;
    private boolean firstMouse = true;

    public Input(long window) {
        this.window = window;
    }

    private final Map<Integer, Boolean> pressedLastCall = new HashMap<>();

    /** True while the given GLFW key (e.g. {@code GLFW_KEY_W}) is held down. */
    public boolean isKeyDown(int key) {
        return glfwGetKey(window, key) == GLFW_PRESS;
    }

    /**
     * True only on the frame the key transitions from up to down (edge). Call
     * at most once per frame per key — it remembers the previous state per key.
     */
    public boolean isKeyPressed(int key) {
        boolean down = isKeyDown(key);
        boolean wasDown = pressedLastCall.getOrDefault(key, false);
        pressedLastCall.put(key, down);
        return down && !wasDown;
    }

    /**
     * Capture the mouse for FPS-style look (hidden + locked to the window) or
     * release it. Resets the delta so re-capturing doesn't cause a jump.
     */
    public void setMouseCaptured(boolean captured) {
        glfwSetInputMode(window, GLFW_CURSOR,
                captured ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        firstMouse = true;
    }

    /** Refreshes mouse deltas for this frame. Called by the engine each loop. */
    public void update() {
        glfwGetCursorPos(window, xBuffer, yBuffer);
        double x = xBuffer[0];
        double y = yBuffer[0];

        if (firstMouse) {          // avoid a huge jump on the very first frame
            lastX = x;
            lastY = y;
            firstMouse = false;
        }

        mouseDeltaX = (float) (x - lastX);
        mouseDeltaY = (float) (lastY - y);   // inverted: moving up is positive
        lastX = x;
        lastY = y;
    }

    public float mouseDeltaX() {
        return mouseDeltaX;
    }

    public float mouseDeltaY() {
        return mouseDeltaY;
    }
}
