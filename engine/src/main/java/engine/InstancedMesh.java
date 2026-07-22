package engine;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.glDrawElementsInstanced;
import static org.lwjgl.opengl.GL33.glVertexAttribDivisor;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * Indexed geometry drawn many times in a single draw call, one instance per
 * per-instance model matrix. Like {@link Mesh}, the base vertex layout is
 * described by {@code attributeSizes} (e.g. {@code {3, 3, 2}} = position, normal,
 * uv, interleaved) bound to locations {@code 0 .. attributeSizes.length - 1}.
 * <p>
 * A second, per-instance vertex buffer holds one {@code mat4} model matrix per
 * instance. A {@code mat4} occupies four consecutive {@code vec4} attribute
 * locations, starting at {@code base = attributeSizes.length}, each advanced once
 * per instance via {@code glVertexAttribDivisor}.
 * <p>
 * A shader using this mesh must declare the instance matrix at the location
 * following the base attributes and apply it to positions. For example, with a
 * base layout of {@code {3, 3, 2}} ({@code base == 3}):
 * <pre>{@code
 * layout(location = 0) in vec3 aPos;
 * layout(location = 1) in vec3 aNormal;
 * layout(location = 2) in vec2 aUv;
 * layout(location = 3) in mat4 aInstanceModel; // occupies locations 3,4,5,6
 * // ...
 * gl_Position = projection * view * aInstanceModel * vec4(aPos, 1.0);
 * }</pre>
 */
public class InstancedMesh implements Disposable {

    private static final int FLOATS_PER_MATRIX = 16;

    private final int vao;
    private final int vbo;              // interleaved base vertex data
    private final int instanceVbo;      // per-instance mat4 model matrices
    private final int ebo;
    private final int indexCount;
    private final int instanceCount;

    /**
     * @param vertices       interleaved base vertex data
     * @param attributeSizes float counts of each base attribute (locations 0..n-1)
     * @param indices        element indices (required, assumed non-null)
     * @param instances      one model matrix per instance
     */
    public InstancedMesh(float[] vertices, int[] attributeSizes, int[] indices, Matrix4f[] instances) {
        int floatsPerVertex = 0;
        for (int size : attributeSizes) {
            floatsPerVertex += size;
        }
        int strideBytes = floatsPerVertex * Float.BYTES;

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // --- Base vertex buffer ---
        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = memAllocFloat(vertices.length);
        try {
            vertexBuffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        } finally {
            memFree(vertexBuffer);
        }

        // --- Describe each base attribute from the layout ---
        int offsetFloats = 0;
        for (int i = 0; i < attributeSizes.length; i++) {
            glVertexAttribPointer(i, attributeSizes[i], GL_FLOAT, false,
                    strideBytes, (long) offsetFloats * Float.BYTES);
            glEnableVertexAttribArray(i);
            offsetFloats += attributeSizes[i];
        }

        // --- Index buffer ---
        ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        IntBuffer indexBuffer = memAllocInt(indices.length);
        try {
            indexBuffer.put(indices).flip();
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        } finally {
            memFree(indexBuffer);
        }
        indexCount = indices.length;

        // --- Per-instance buffer: one mat4 (4 x vec4) per instance ---
        instanceCount = instances.length;
        int base = attributeSizes.length;
        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        uploadInstances(instances, GL_DYNAMIC_DRAW);

        int matrixStrideBytes = FLOATS_PER_MATRIX * Float.BYTES;
        for (int k = 0; k < 4; k++) {
            int loc = base + k;
            glVertexAttribPointer(loc, 4, GL_FLOAT, false,
                    matrixStrideBytes, (long) k * 4 * Float.BYTES);
            glEnableVertexAttribArray(loc);
            glVertexAttribDivisor(loc, 1); // advance once per instance
        }

        // Unbind the VAO first so it "remembers" the element buffer binding.
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /** Draws all instances in a single call. */
    public void render() {
        glBindVertexArray(vao);
        glDrawElementsInstanced(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0, instanceCount);
        glBindVertexArray(0);
    }

    /**
     * Re-uploads the per-instance model matrices. The instance count is assumed
     * unchanged from construction.
     */
    public void updateInstances(Matrix4f[] instances) {
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        uploadInstances(instances, GL_DYNAMIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /** Packs the matrices column-major and uploads them to the bound instance VBO. */
    private void uploadInstances(Matrix4f[] instances, int usage) {
        FloatBuffer buffer = memAllocFloat(instances.length * FLOATS_PER_MATRIX);
        try {
            for (int i = 0; i < instances.length; i++) {
                instances[i].get(i * FLOATS_PER_MATRIX, buffer); // column-major, absolute
            }
            buffer.position(instances.length * FLOATS_PER_MATRIX).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, usage);
        } finally {
            memFree(buffer);
        }
    }

    public int instanceCount() {
        return instanceCount;
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(instanceVbo);
        glDeleteBuffers(ebo);
    }
}
