package engine;

import org.lwjgl.opengl.GL;

import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_0;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Owns the {@link Window} and runs the main loop over one or more {@link Scene}s.
 * Only the current scene is initialized; number keys 1..9 switch scenes,
 * disposing the old one and initializing the new one.
 */
public class Engine {

    private final WindowConfig config;
    private final List<Scene> scenes;
    private final Window window;
    private int current = 0;

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
        window.setTitle(config.title + "  [" + (current + 1) + "/" + scenes.size() + "] " + scene.name());
    }

    private void loop() {
        glClearColor(config.clearR, config.clearG, config.clearB, config.clearA);

        double lastTime = glfwGetTime();
        while (!window.shouldClose()) {
            double now = glfwGetTime();
            float deltaSeconds = (float) (now - lastTime);
            lastTime = now;

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

            // Reset viewport every frame so scenes that change it (e.g. shadows) recover.
            glViewport(0, 0, window.framebufferWidth(), window.framebufferHeight());
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            scenes.get(current).update(deltaSeconds);
            scenes.get(current).render();

            window.swapBuffers();
        }
    }
}
