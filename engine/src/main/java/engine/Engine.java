package engine;

import org.lwjgl.opengl.GL;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_0;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_BRACKET;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_BRACKET;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Owns the {@link Window} and runs the main loop over one or more {@link Scene}s.
 * Only the current scene is initialized; number keys 1..9 (and 0 for a 10th)
 * switch scenes, disposing the old one and initializing the new one.
 *
 * The loop uses a fixed-timestep accumulator: simulation ({@link Scene#fixedUpdate})
 * runs at a constant {@link #FIXED_DT} independent of frame rate, while
 * {@link Scene#update} (input, camera) and {@link Scene#render} run once per
 * frame. Framebuffer resizes are dispatched to {@link Scene#resize}.
 */
public class Engine {

    /** Fixed simulation step: 60 Hz. */
    private static final float FIXED_DT = 1f / 60f;
    /** Clamp huge frame gaps (breakpoints, stalls) to avoid a spiral of death. */
    private static final double MAX_FRAME_TIME = 0.25;

    private final WindowConfig config;
    private final List<Scene> scenes;
    private final Window window;
    private final Time time = new Time();

    private int current = 0;
    private String sceneLabel = "";
    private int lastFbWidth = -1;
    private int lastFbHeight = -1;
    private double lastTitleUpdate = 0;

    public Engine(WindowConfig config, Scene scene) {
        this(config, List.of(scene));
    }

    public Engine(WindowConfig config, List<Scene> scenes) {
        if (scenes.isEmpty()) {
            throw new IllegalArgumentException("Engine needs at least one scene");
        }
        this.config = config;
        this.scenes = scenes;
        this.window = new Window(config);
    }

    public void run() {
        try {
            window.create();
            GL.createCapabilities();
            GLDebug.enable();               // OpenGL debug output where supported
            glEnable(GL_DEPTH_TEST);
            activate(0, true);
            loop();
        } finally {
            scenes.get(current).dispose();
            window.dispose();
        }
    }

    private void activate(int index, boolean firstTime) {
        if (index < 0 || index >= scenes.size() || (!firstTime && index == current)) {
            return;
        }
        if (!firstTime) {
            scenes.get(current).dispose();
        }
        window.input().setMouseCaptured(false);   // scenes re-capture if they want
        current = index;
        Scene scene = scenes.get(current);
        scene.init(window);

        // Match the scene to the current framebuffer size right away.
        lastFbWidth = window.framebufferWidth();
        lastFbHeight = window.framebufferHeight();
        if (lastFbHeight > 0) {
            scene.resize(lastFbWidth, lastFbHeight);
        }

        sceneLabel = config.title + "  [" + (current + 1) + "/" + scenes.size() + "] " + scene.name();
        window.setTitle(sceneLabel);
    }

    private void loop() {
        glClearColor(config.clearR, config.clearG, config.clearB, config.clearA);

        double lastTime = glfwGetTime();
        lastTitleUpdate = lastTime;
        double accumulator = 0;

        while (!window.shouldClose()) {
            double now = glfwGetTime();
            double frameTime = Math.min(now - lastTime, MAX_FRAME_TIME);
            lastTime = now;
            time.update(now);

            window.pollEvents();
            window.input().update();

            for (int i = 0; i < scenes.size() && i < 9; i++) {
                if (window.input().isKeyPressed(GLFW_KEY_1 + i)) {
                    activate(i, false);
                }
            }
            if (scenes.size() >= 10 && window.input().isKeyPressed(GLFW_KEY_0)) {
                activate(9, false);   // the 0 key selects the 10th scene
            }
            // [ and ] cycle through all scenes (works past the 10 number keys).
            if (window.input().isKeyPressed(GLFW_KEY_RIGHT_BRACKET)) {
                activate((current + 1) % scenes.size(), false);
            }
            if (window.input().isKeyPressed(GLFW_KEY_LEFT_BRACKET)) {
                activate((current - 1 + scenes.size()) % scenes.size(), false);
            }

            Scene scene = scenes.get(current);

            // Dispatch framebuffer resizes.
            int fbWidth = window.framebufferWidth();
            int fbHeight = window.framebufferHeight();
            if ((fbWidth != lastFbWidth || fbHeight != lastFbHeight) && fbHeight > 0) {
                scene.resize(fbWidth, fbHeight);
                lastFbWidth = fbWidth;
                lastFbHeight = fbHeight;
            }

            // Fixed-timestep simulation: catch up in constant steps.
            accumulator += frameTime;
            while (accumulator >= FIXED_DT) {
                scene.fixedUpdate(FIXED_DT);
                accumulator -= FIXED_DT;
            }

            // Variable per-frame update + render.
            scene.update((float) frameTime);

            glViewport(0, 0, fbWidth, fbHeight);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            scene.render();

            window.swapBuffers();

            // Refresh the FPS readout in the title about once per second.
            if (now - lastTitleUpdate >= 1.0) {
                window.setTitle(sceneLabel + "  —  " + time.fps() + " fps");
                lastTitleUpdate = now;
            }
        }
    }
}
