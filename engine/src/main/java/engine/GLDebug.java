package engine;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glGetError;

/**
 * OpenGL debug helpers. {@link #enable()} wires the driver's debug-output
 * messages to stderr where the context supports it; {@link #checkError} is the
 * portable fallback (e.g. on macOS, which caps out at OpenGL 4.1 with no debug
 * output).
 */
public final class GLDebug {

    private GLDebug() {
    }

    /**
     * Register an OpenGL debug-output message callback if the current context
     * supports it (OpenGL 4.3 or the KHR_debug extension); otherwise a no-op.
     */
    public static void enable() {
        GLCapabilities caps = GL.getCapabilities();
        if (caps.OpenGL43 || caps.GL_KHR_debug) {
            // Callback lives for the process lifetime; no need to track/free it.
            GLUtil.setupDebugMessageCallback();
            Log.info("OpenGL debug output enabled");
        } else {
            Log.info("OpenGL debug output unavailable on this context");
        }
    }

    /**
     * Drain the GL error queue, logging each error code. Safe on any context
     * version, unlike {@link #enable()}.
     */
    public static void checkError(String tag) {
        int code;
        while ((code = glGetError()) != GL_NO_ERROR) {
            Log.error(tag + ": GL error 0x" + Integer.toHexString(code));
        }
    }
}
