package engine;

import org.lwjgl.glfw.GLFWErrorCallback;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Wraps a GLFW window and its OpenGL context. Owns creation, the ESC-to-close
 * handler, buffer swapping, event polling, and teardown.
 */
public class Window implements Disposable {

    private final WindowConfig config;
    private long handle = NULL;
    private Input input;

    public Window(WindowConfig config) {
        this.config = config;
    }

    /** Initializes GLFW, creates the window, and makes its context current. */
    public void create() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, config.resizable ? GLFW_TRUE : GLFW_FALSE);

        // Core profile + forward-compat: required for modern shaders on macOS.
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, config.glMajor);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, config.glMinor);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        handle = glfwCreateWindow(config.width, config.height, config.title, NULL, NULL);
        if (handle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        glfwSetKeyCallback(handle, (win, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(win, true);
            }
        });

        glfwMakeContextCurrent(handle);
        glfwSwapInterval(config.vsync ? 1 : 0);
        glfwShowWindow(handle);

        input = new Input(handle);
    }

    /** Input state for this window (valid after {@link #create()}). */
    public Input input() {
        return input;
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public long handle() {
        return handle;
    }

    public int width() {
        return config.width;
    }

    public int height() {
        return config.height;
    }

    /** Framebuffer size in pixels (may exceed window size on HiDPI/retina). */
    public int framebufferWidth() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(handle, w, h);
        return w[0];
    }

    public int framebufferHeight() {
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(handle, w, h);
        return h[0];
    }

    public void setTitle(String title) {
        glfwSetWindowTitle(handle, title);
    }

    /** Convenient aspect ratio (width / height) for projection matrices later. */
    public float aspectRatio() {
        return (float) config.width / config.height;
    }

    @Override
    public void dispose() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
            handle = NULL;
        }
        glfwTerminate();
        GLFWErrorCallback previous = glfwSetErrorCallback(null);
        if (previous != null) {
            previous.free();
        }
    }
}
