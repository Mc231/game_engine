package scenes;

import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.Window;
import org.joml.Matrix4f;

/**
 * A real 3D scene: a colored cube tumbling in perspective.
 *
 * The classic transform pipeline runs in the vertex shader:
 *   clip = Projection * View * Model * position
 *     - Model:      places/rotates the object in the world (here: spin it).
 *     - View:       the camera — we push the world 3 units away from the eye.
 *     - Projection: perspective (far things shrink) + corrects aspect ratio.
 */
public class CubeScene implements Scene {

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aColor;
            out vec3 vertexColor;
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() {
                gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
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

    // 36 vertices = 6 faces * 2 triangles * 3 corners. Each vertex: position(3) + color(3).
    // The cube spans -0.5..0.5 on every axis; each face gets one solid color.
    private static final float[] VERTICES = {
            // ---- front (z = +0.5) : red ----
            -0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 0f,
            -0.5f, -0.5f,  0.5f,  1f, 0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 0f,
            -0.5f,  0.5f,  0.5f,  1f, 0f, 0f,

            // ---- back (z = -0.5) : green ----
            -0.5f, -0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
            -0.5f, -0.5f, -0.5f,  0f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f, 0f,
             0.5f, -0.5f, -0.5f,  0f, 1f, 0f,

            // ---- left (x = -0.5) : blue ----
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f,  0.5f, -0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,

            // ---- right (x = +0.5) : yellow ----
             0.5f,  0.5f,  0.5f,  1f, 1f, 0f,
             0.5f, -0.5f, -0.5f,  1f, 1f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 1f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 1f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 1f, 0f,
             0.5f, -0.5f, -0.5f,  1f, 1f, 0f,

            // ---- top (y = +0.5) : magenta ----
            -0.5f,  0.5f, -0.5f,  1f, 0f, 1f,
            -0.5f,  0.5f,  0.5f,  1f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 1f,
            -0.5f,  0.5f, -0.5f,  1f, 0f, 1f,
             0.5f,  0.5f,  0.5f,  1f, 0f, 1f,
             0.5f,  0.5f, -0.5f,  1f, 0f, 1f,

            // ---- bottom (y = -0.5) : cyan ----
            -0.5f, -0.5f, -0.5f,  0f, 1f, 1f,
             0.5f, -0.5f, -0.5f,  0f, 1f, 1f,
             0.5f, -0.5f,  0.5f,  0f, 1f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 1f, 1f,
             0.5f, -0.5f,  0.5f,  0f, 1f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 1f, 1f,
    };

    private ShaderProgram shader;
    private Mesh mesh;

    private final Matrix4f model = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private float elapsed;

    @Override
    public void init(Window window) {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        mesh = new Mesh(VERTICES, new int[]{3, 3});

        // Camera: sit at the origin looking down -Z, so push the world back 3 units.
        view.identity().translate(0f, 0f, -3f);

        // Perspective: 45° vertical field of view, correct aspect, near/far planes.
        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
        // Tumble: different speeds on X and Y so all faces come into view.
        model.identity()
                .rotateX(elapsed * 0.7f)
                .rotateY(elapsed);
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;                 // ignore minimized/degenerate
        projection.identity().perspective(
                (float) Math.toRadians(45.0), (float) width / height, 0.1f, 100f);
    }

    @Override
    public void render() {
        shader.bind();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", view);
        shader.setUniform("uModel", model);
        mesh.render();
        shader.unbind();
    }

    @Override
    public void dispose() {
        mesh.dispose();
        shader.dispose();
    }
}
