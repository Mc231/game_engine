package scenes;

import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.Window;
import org.joml.Matrix4f;

/**
 * Demo content: a rainbow triangle spinning around its center. All the OpenGL
 * plumbing now lives in the engine — this class only describes *what* to draw.
 */
public class TriangleScene implements Scene {

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aColor;
            out vec3 vertexColor;
            uniform mat4 uTransform;
            void main() {
                gl_Position = uTransform * vec4(aPos, 1.0);
                vertexColor = aColor;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec3 vertexColor;
            out vec4 FragColor;
            void main() {
                FragColor = vec4(vertexColor, 1.0);
            }
            """;

    private ShaderProgram shader;
    private Mesh mesh;
    private final Matrix4f transform = new Matrix4f();
    private float elapsed;

    @Override
    public void init(Window window) {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);

        float[] vertices = {
                // position            // color
                 0.0f,  0.5f, 0.0f,    1.0f, 0.0f, 0.0f,   // top          -> red
                -0.5f, -0.5f, 0.0f,    0.0f, 1.0f, 0.0f,   // bottom-left  -> green
                 0.5f, -0.5f, 0.0f,    0.0f, 0.0f, 1.0f    // bottom-right -> blue
        };
        mesh = new Mesh(vertices, new int[]{3, 3});   // layout: position(3), color(3)
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
        transform.identity().rotate(elapsed, 0f, 0f, 1f);
    }

    @Override
    public void render() {
        shader.bind();
        shader.setUniform("uTransform", transform);
        mesh.render();
        shader.unbind();
    }

    @Override
    public void dispose() {
        mesh.dispose();
        shader.dispose();
    }
}
