package engine;

import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.stb.STBImage.stbi_failure_reason;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load_from_memory;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * An OpenGL cubemap texture built from six classpath images, one per face.
 * Unlike a 2D {@link Texture}, cubemap faces are NOT flipped vertically on load
 * (the cubemap convention already matches image space). Bind it to a texture
 * unit before drawing; the shader's {@code samplerCube} uniform must point at
 * the same unit.
 */
public class CubemapTexture implements Disposable {

    private final int id;

    /**
     * Load six face images into a cubemap. Faces are uploaded in the fixed
     * cubemap order +X, -X, +Y, -Y, +Z, -Z.
     *
     * @param right  +X face (classpath path, e.g. "skybox/right.png")
     * @param left   -X face
     * @param top    +Y face
     * @param bottom -Y face
     * @param front  +Z face
     * @param back   -Z face
     */
    public CubemapTexture(String right, String left, String top, String bottom, String front, String back) {
        String[] faces = {right, left, top, bottom, front, back};

        id = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);

        // Cubemap sampling expects the raw, unflipped image orientation.
        stbi_set_flip_vertically_on_load(false);

        for (int i = 0; i < 6; i++) {
            ByteBuffer fileBytes = readResource(faces[i]);
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                // Force 4 channels (RGBA) so every face has a uniform layout.
                ByteBuffer pixels = stbi_load_from_memory(fileBytes, w, h, channels, 4);
                if (pixels == null) {
                    throw new RuntimeException("Failed to load cubemap face '" + faces[i]
                            + "': " + stbi_failure_reason());
                }

                glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_RGBA,
                        w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

                stbi_image_free(pixels);
            } finally {
                memFree(fileBytes);
            }
        }

        // Smooth sampling; clamp all three axes so face seams don't wrap.
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
    }

    /** Activate a texture unit (0, 1, ...) and bind this cubemap to it. */
    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_CUBE_MAP, id);
    }

    private static ByteBuffer readResource(String path) {
        try (InputStream in = CubemapTexture.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Cubemap resource not found on classpath: " + path);
            }
            byte[] bytes = in.readAllBytes();
            // stb needs native (off-heap) memory, not a Java byte[].
            ByteBuffer buffer = memAlloc(bytes.length);
            buffer.put(bytes).flip();
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException("Failed to read cubemap resource: " + path, e);
        }
    }

    @Override
    public void dispose() {
        glDeleteTextures(id);
    }
}
