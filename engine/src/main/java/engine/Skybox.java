package engine;

import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.GL_DEPTH_FUNC;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_LEQUAL;
import static org.lwjgl.opengl.GL11.GL_LESS;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glDepthFunc;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glGetInteger;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

/**
 * Renders an environment cubemap as a background that surrounds the camera.
 * The skybox OWNS its {@link CubemapTexture} and disposes it. Draw it once per
 * frame (typically last) via {@link #render}; it keeps the sky centred on the
 * camera and writes depth at the far plane so scene geometry always occludes it.
 */
public class Skybox implements Disposable {

    // A unit cube spanning -1..1, 36 vertices (6 faces * 2 triangles * 3 verts).
    private static final float[] CUBE_VERTICES = {
            -1f,  1f, -1f, -1f, -1f, -1f,  1f, -1f, -1f,
             1f, -1f, -1f,  1f,  1f, -1f, -1f,  1f, -1f,

            -1f, -1f,  1f, -1f, -1f, -1f, -1f,  1f, -1f,
            -1f,  1f, -1f, -1f,  1f,  1f, -1f, -1f,  1f,

             1f, -1f, -1f,  1f, -1f,  1f,  1f,  1f,  1f,
             1f,  1f,  1f,  1f,  1f, -1f,  1f, -1f, -1f,

            -1f, -1f,  1f, -1f,  1f,  1f,  1f,  1f,  1f,
             1f,  1f,  1f,  1f, -1f,  1f, -1f, -1f,  1f,

            -1f,  1f, -1f,  1f,  1f, -1f,  1f,  1f,  1f,
             1f,  1f,  1f, -1f,  1f,  1f, -1f,  1f, -1f,

            -1f, -1f, -1f, -1f, -1f,  1f,  1f, -1f, -1f,
             1f, -1f, -1f, -1f, -1f,  1f,  1f, -1f,  1f
    };

    private static final String VERTEX_SOURCE =
            "#version 330 core\n"
                    + "layout(location=0) in vec3 aPos; out vec3 dir;"
                    + " uniform mat4 uView; uniform mat4 uProjection;"
                    + " void main(){ dir=aPos; vec4 p=uProjection*uView*vec4(aPos,1.0);"
                    + " gl_Position=p.xyww; }";

    private static final String FRAGMENT_SOURCE =
            "#version 330 core\n"
                    + "in vec3 dir; out vec4 FragColor; uniform samplerCube uSkybox;"
                    + " void main(){ FragColor=texture(uSkybox,dir); }";

    private final CubemapTexture cubemap;
    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;

    /** Take ownership of {@code cubemap} and build the cube geometry + shader. */
    public Skybox(CubemapTexture cubemap) {
        this.cubemap = cubemap;
        this.shader = new ShaderProgram(VERTEX_SOURCE, FRAGMENT_SOURCE);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, CUBE_VERTICES, GL_STATIC_DRAW);
        // Position only: location 0, 3 floats, tightly packed.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    /**
     * Draw the skybox. The view matrix's translation is stripped so the sky
     * stays centred on the camera; depth testing uses {@code GL_LEQUAL} so the
     * far-plane-clamped fragments pass, then is restored to {@code GL_LESS}.
     */
    public void render(Matrix4f view, Matrix4f projection) {
        int previousDepthFunc = glGetInteger(GL_DEPTH_FUNC);
        glDepthFunc(GL_LEQUAL);

        shader.bind();
        shader.setUniform("uProjection", projection);

        // Remove translation so only rotation affects the sky direction.
        Matrix4f skyView = new Matrix4f(view);
        skyView.m30(0f).m31(0f).m32(0f);
        shader.setUniform("uView", skyView);

        cubemap.bind(0);
        shader.setUniform("uSkybox", 0);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 36);
        glBindVertexArray(0);

        glDepthFunc(previousDepthFunc == 0 ? GL_LESS : previousDepthFunc);
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        shader.dispose();
        cubemap.dispose();
    }
}
