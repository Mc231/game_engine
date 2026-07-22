package engine;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.memAllocFloat;
import static org.lwjgl.system.MemoryUtil.memFree;

/**
 * A full-screen post-processing pass. Takes the color texture of an off-screen
 * {@link Framebuffer} and draws it over the whole screen through a fragment
 * shader that can apply a selectable effect (see the effect constants).
 */
public class PostProcessor implements Disposable {

    public static final int NONE = 0;
    public static final int GRAYSCALE = 1;
    public static final int INVERT = 2;
    public static final int VIGNETTE = 3;
    /** Number of distinct effects (handy for cycling through them). */
    public static final int EFFECT_COUNT = 4;

    private static final String VERTEX_SOURCE =
            "#version 330 core\n" +
            "layout(location=0) in vec2 aPos; layout(location=1) in vec2 aUv; out vec2 uv;" +
            " void main(){ uv=aUv; gl_Position=vec4(aPos,0.0,1.0); }";

    private static final String FRAGMENT_SOURCE =
            "#version 330 core\n" +
            "in vec2 uv; out vec4 FragColor; uniform sampler2D uScene; uniform int uEffect;" +
            " void main(){ vec3 c=texture(uScene,uv).rgb;" +
            " if(uEffect==1){ float g=dot(c,vec3(0.299,0.587,0.114)); c=vec3(g);}" +
            " else if(uEffect==2){ c=1.0-c;}" +
            " else if(uEffect==3){ float d=distance(uv,vec2(0.5)); c*=smoothstep(0.85,0.35,d);}" +
            " FragColor=vec4(c,1.0);} ";

    private final int vao;
    private final int vbo;
    private final ShaderProgram shader;

    public PostProcessor() {
        // Two triangles covering the whole NDC quad; each vertex is a vec2
        // position (location 0) followed by a vec2 uv (location 1).
        float[] vertices = {
                //  x,    y,    u,   v
                -1f, -1f, 0f, 0f,
                 1f, -1f, 1f, 0f,
                 1f,  1f, 1f, 1f,

                -1f, -1f, 0f, 0f,
                 1f,  1f, 1f, 1f,
                -1f,  1f, 0f, 1f,
        };

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        FloatBuffer vertexBuffer = memAllocFloat(vertices.length);
        try {
            vertexBuffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        } finally {
            memFree(vertexBuffer);
        }

        int strideBytes = 4 * Float.BYTES;
        glVertexAttribPointer(0, 2, GL_FLOAT, false, strideBytes, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, strideBytes, (long) 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        shader = new ShaderProgram(VERTEX_SOURCE, FRAGMENT_SOURCE);
    }

    /**
     * Draw {@code scene}'s color texture full-screen with the given effect
     * (one of the effect constants). Assumes the caller has already bound the
     * default framebuffer and set the viewport.
     */
    public void render(Framebuffer scene, int effect) {
        // The quad is a flat overlay; depth testing would only get in the way.
        glDisable(GL_DEPTH_TEST);

        shader.bind();
        scene.bindColorTexture(0);
        shader.setUniform("uScene", 0);
        shader.setUniform("uEffect", effect);

        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);

        glEnable(GL_DEPTH_TEST);
    }

    @Override
    public void dispose() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        shader.dispose();
    }
}
