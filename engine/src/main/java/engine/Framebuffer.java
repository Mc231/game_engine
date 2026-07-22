package engine;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL14.GL_DEPTH_COMPONENT24;
import static org.lwjgl.opengl.GL30.*;

/**
 * An off-screen render target: a framebuffer with a sampleable RGB color
 * texture and a depth renderbuffer. Render the scene into it ({@link #bind}),
 * then sample the color texture in a later pass ({@link #bindColorTexture}) —
 * the basis for full-screen post-processing.
 */
public class Framebuffer implements Disposable {

    private final int fbo;
    private final int colorTex;
    private final int depthRbo;
    private final int width;
    private final int height;

    public Framebuffer(int width, int height) {
        this.width = width;
        this.height = height;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        // --- Color attachment: an RGB texture we can sample from later ---
        colorTex = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, colorTex);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0,
                GL_RGB, GL_UNSIGNED_BYTE, 0);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorTex, 0);

        // --- Depth attachment: a renderbuffer (we never sample depth here) ---
        depthRbo = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthRbo);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthRbo);

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
        }

        // Restore the default framebuffer so callers start from a clean slate.
        glBindRenderbuffer(GL_RENDERBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /** Bind this framebuffer + set the viewport to its size for off-screen rendering. */
    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }

    /** Restore rendering to the window's framebuffer at the given pixel size. */
    public void unbind(int screenWidth, int screenHeight) {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, screenWidth, screenHeight);
    }

    /** Bind the color texture to a texture unit for sampling in a later pass. */
    public void bindColorTexture(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, colorTex);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    @Override
    public void dispose() {
        glDeleteFramebuffers(fbo);
        glDeleteTextures(colorTex);
        glDeleteRenderbuffers(depthRbo);
    }
}
