package engine;

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
 * A drawable chunk of geometry: a VAO backed by a vertex buffer (and optionally
 * an index buffer). The vertex layout is described by {@code attributeSizes},
 * e.g. {@code {3, 3}} means every vertex is a 3-float position followed by a
 * 3-float color, interleaved. Attributes are bound to locations 0, 1, 2, ...
 */
public class Mesh implements Disposable {

    private final int vao;
    private final int vbo;
    private final int ebo;          // 0 when the mesh has no index buffer
    private final int drawCount;    // vertices (glDrawArrays) or indices (glDrawElements)

    public Mesh(float[] vertices, int[] attributeSizes) {
        this(vertices, attributeSizes, null);
    }

    public Mesh(float[] vertices, int[] attributeSizes, int[] indices) {
        int floatsPerVertex = 0;
        for (int size : attributeSizes) {
            floatsPerVertex += size;
        }
        int strideBytes = floatsPerVertex * Float.BYTES;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // --- Vertex buffer ---
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        // Heap-native buffer (freed after upload) — handles meshes of any size.
        FloatBuffer vertexBuffer = memAllocFloat(vertices.length);
        try {
            vertexBuffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        } finally {
            memFree(vertexBuffer);
        }

        // --- Describe each attribute from the layout ---
        int offsetFloats = 0;
        for (int i = 0; i < attributeSizes.length; i++) {
            glVertexAttribPointer(i, attributeSizes[i], GL_FLOAT, false,
                    strideBytes, (long) offsetFloats * Float.BYTES);
            glEnableVertexAttribArray(i);
            offsetFloats += attributeSizes[i];
        }

        // --- Optional index buffer (draw shared vertices without duplicating them) ---
        if (indices != null) {
            ebo = glGenBuffers();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            IntBuffer indexBuffer = memAllocInt(indices.length);
            try {
                indexBuffer.put(indices).flip();
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
            } finally {
                memFree(indexBuffer);
            }
            drawCount = indices.length;
        } else {
            ebo = 0;
            drawCount = vertices.length / floatsPerVertex;
        }

        // Unbind the VAO first so it "remembers" the element buffer binding.
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    public void render() {
        glBindVertexArray(vao);
        if (ebo != 0) {
            glDrawElements(GL_TRIANGLES, drawCount, GL_UNSIGNED_INT, 0);
        } else {
            glDrawArrays(GL_TRIANGLES, 0, drawCount);
        }
        glBindVertexArray(0);
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        if (ebo != 0) {
            glDeleteBuffers(ebo);
        }
    }
}
