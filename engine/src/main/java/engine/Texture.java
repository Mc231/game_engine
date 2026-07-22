package engine;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * A 2D OpenGL texture loaded from a classpath image (PNG/JPG/...) via stb_image.
 * Bind it to a texture unit before drawing; the shader's sampler uniform must
 * point at the same unit.
 */
public class Texture implements Disposable {

    private final int textureId;
    private final int width;
    private final int height;

    /** @param resourcePath classpath-relative path, e.g. "textures/crate.png". */
    public Texture(String resourcePath) {
        ByteBuffer fileBytes = readResource(resourcePath);
        try (MemoryStack stack = stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // OpenGL's texture origin is bottom-left; image files are top-left.
            stbi_set_flip_vertically_on_load(true);

            // Decode to raw pixels, forcing 4 channels (RGBA).
            ByteBuffer pixels = stbi_load_from_memory(fileBytes, w, h, channels, 4);
            if (pixels == null) {
                throw new RuntimeException("Failed to load texture '" + resourcePath
                        + "': " + stbi_failure_reason());
            }
            width = w.get(0);
            height = h.get(0);

            textureId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, textureId);

            // Wrapping: repeat the texture past the [0,1] UV range.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            // Filtering: smooth when scaled up; mipmapped when scaled down.
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glGenerateMipmap(GL_TEXTURE_2D);

            stbi_image_free(pixels);
            glBindTexture(GL_TEXTURE_2D, 0);
        } finally {
            memFree(fileBytes);
        }
    }

    /** Activate a texture unit (0, 1, ...) and bind this texture to it. */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream in = Texture.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Texture resource not found on classpath: " + path);
            }
            byte[] bytes = in.readAllBytes();
            // stb needs native (off-heap) memory, not a Java byte[].
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read texture resource: " + path, e);
        }
    }

    @Override
    public void dispose() {
        glDeleteTextures(textureId);
    }
}
