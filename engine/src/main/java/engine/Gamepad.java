package engine;

import org.lwjgl.glfw.GLFWGamepadState;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Per-frame state for a single GLFW gamepad (mapped controller). Poll it once
 * per frame with {@link #update()}; then query held buttons, just-pressed
 * button edges, and analog axes.
 *
 * <pre>{@code
 * Gamepad pad = new Gamepad();          // GLFW_JOYSTICK_1
 *
 * pad.update();                         // once per frame
 * if (pad.button(GLFW_GAMEPAD_BUTTON_A))        { ... }
 * if (pad.buttonPressed(GLFW_GAMEPAD_BUTTON_A)) { ... }   // edge only
 * float lx = pad.axis(GLFW_GAMEPAD_AXIS_LEFT_X);
 * }</pre>
 */
public class Gamepad {

    /** GLFW exposes exactly 15 gamepad buttons ({@code GLFW_GAMEPAD_BUTTON_LAST + 1}). */
    private static final int BUTTON_COUNT = GLFW_GAMEPAD_BUTTON_LAST + 1;

    private final int joystick;

    /** Long-lived; reused every frame. Never freed per frame. */
    private final GLFWGamepadState state = GLFWGamepadState.create();

    private final boolean[] pressedLastFrame = new boolean[BUTTON_COUNT];

    /** Binds to the first joystick slot ({@code GLFW_JOYSTICK_1}). */
    public Gamepad() {
        this(GLFW_JOYSTICK_1);
    }

    /** Binds to a specific joystick slot ({@code GLFW_JOYSTICK_1..16}). */
    public Gamepad(int joystick) {
        this.joystick = joystick;
    }

    /** True if a mapped gamepad is currently connected in this slot. */
    public boolean isPresent() {
        return glfwJoystickIsGamepad(joystick);
    }

    /**
     * Refreshes the snapshot and records button edges for {@link #buttonPressed}.
     * Called once per frame; clears state when no gamepad is present.
     */
    public void update() {
        for (int i = 0; i < BUTTON_COUNT; i++) {
            pressedLastFrame[i] = button(i);
        }
        if (!isPresent()) {
            return;                       // leave the snapshot as-is; queries guard on isPresent()
        }
        glfwGetGamepadState(joystick, state);
    }

    /** True while the given {@code GLFW_GAMEPAD_BUTTON_*} is held. */
    public boolean button(int gamepadButton) {
        if (!isPresent()) {
            return false;
        }
        return state.buttons(gamepadButton) == GLFW_PRESS;
    }

    /**
     * True only on the frame the button transitions from up to down (edge).
     * Relies on {@link #update()} being called once per frame.
     */
    public boolean buttonPressed(int gamepadButton) {
        return button(gamepadButton) && !pressedLastFrame[gamepadButton];
    }

    /** Current value of the given {@code GLFW_GAMEPAD_AXIS_*}, or 0 if absent. */
    public float axis(int gamepadAxis) {
        if (!isPresent()) {
            return 0f;
        }
        return state.axes(gamepadAxis);
    }
}
