package engine;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBEasyFont;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * A minimal immediate-mode 2D text overlay for a HUD. Text is rasterised by
 * {@code STBEasyFont} into quads; since the OpenGL 3.3 core profile has no
 * {@code GL_QUADS}, each quad is expanded into two triangles via an index
 * buffer and drawn with {@code glDrawElements}.
 * <p>
 * Usage: {@link #begin(int, int)}, one or more {@link #text}, then {@link #end()}.
 */
public class Hud implements Disposable {

    /** STBEasyFont packs each vertex as {@code float x, float y, float z, byte[4] color} — 16 bytes. */
    private static final int VERTEX_STRIDE_BYTES = 16;

    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final int ebo;

    // Reused scratch buffer that STBEasyFont fills with quad geometry.
    private final ByteBuffer fontBuffer = BufferUtils.createByteBuffer(99999);

    public Hud() {
        String vertSrc =
                "#version 330 core\n" +
                "in vec2 aPos;\n" +
                "uniform mat4 uProjection;\n" +
                "void main(){ gl_Position = uProjection * vec4(aPos, 0.0, 1.0); }\n";
        String fragSrc =
                "#version 330 core\n" +
                "out vec4 FragColor;\n" +
                "uniform vec3 uColor;\n" +
                "void main(){ FragColor = vec4(uColor, 1.0); }\n";
        shader = new ShaderProgram(vertSrc, fragSrc);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();
    }

    /**
     * Begin a HUD pass: disable depth testing, bind the shader with a top-left
     * origin (y-down) orthographic projection sized to the screen, and bind the VAO.
     */
    public void begin(int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        shader.bind();
        shader.setUniform("uProjection", new Matrix4f().ortho(0, screenWidth, screenHeight, 0, -1, 1));
        glBindVertexArray(vao);
    }

    /**
     * Draw a string at ({@code x}, {@code y}) in screen pixels (top-left origin).
     * {@code scale} enlarges the glyphs about the text origin; {@code r,g,b} is
     * the fill colour in the range [0, 1]. Must be called between {@link #begin}
     * and {@link #end}.
     */
    public void text(float x, float y, float scale, String s, float r, float g, float b) {
        fontBuffer.clear();
        int quads = STBEasyFont.stb_easy_font_print(x, y, s, (ByteBuffer) null, fontBuffer);
        if (quads <= 0) {
            return;
        }

        int vertexCount = quads * 4;
        float[] positions = new float[vertexCount * 2];
        int[] indices = new int[quads * 6];

        for (int q = 0; q < quads; q++) {
            for (int v = 0; v < 4; v++) {
                int offset = (q * 4 + v) * VERTEX_STRIDE_BYTES;
                float vx = fontBuffer.getFloat(offset);
                float vy = fontBuffer.getFloat(offset + 4);
                // Scale about the text origin so scale=1 is a no-op.
                float px = x + (vx - x) * scale;
                float py = y + (vy - y) * scale;
                int p = (q * 4 + v) * 2;
                positions[p] = px;
                positions[p + 1] = py;
            }
            int baseVertex = q * 4;
            int baseIndex = q * 6;
            indices[baseIndex] = baseVertex;
            indices[baseIndex + 1] = baseVertex + 1;
            indices[baseIndex + 2] = baseVertex + 2;
            indices[baseIndex + 3] = baseVertex;
            indices[baseIndex + 4] = baseVertex + 2;
            indices[baseIndex + 5] = baseVertex + 3;
        }

        // Upload positions.
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = memAllocFloat(positions.length);
        try {
            vertexBuffer.put(positions).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
        } finally {
            memFree(vertexBuffer);
        }

        // Upload indices.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = memAllocInt(indices.length);
        try {
            indexBuffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);
        } finally {
            memFree(indexBuffer);
        }

        // Position attribute → location 0, 2 floats per vertex, tightly packed.
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        shader.setUniform("uColor", new Vector3f(r, g, b));
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
    }

    /** End the HUD pass and re-enable depth testing. */
    public void end() {
        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        shader.dispose();
    }
}
