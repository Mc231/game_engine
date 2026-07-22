package scenes;

import engine.Camera;
import engine.Input;
import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A field of textured cubes you fly through with a first-person camera.
 *   Move: W/A/S/D    Up/Down: Space / Left-Shift    Look: mouse    Quit: Esc
 *
 * The only difference from a single static cube is that the view matrix now
 * comes from a {@link Camera} driven by {@link Input}, instead of a hardcoded
 * translate. Each cube is drawn with its own model matrix.
 */
public class CameraCubeScene implements Scene {

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoord;
            out vec2 texCoord;
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() {
                gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
                texCoord = aTexCoord;
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec2 texCoord;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            void main() {
                FragColor = texture(uTexture, texCoord);
            }
            """;

    // 36 vertices: position(3) + UV(2), one full texture per face.
    private static final float[] VERTICES = {
            -0.5f, -0.5f,  0.5f,  0f, 0f,   0.5f, -0.5f,  0.5f,  1f, 0f,   0.5f,  0.5f,  0.5f,  1f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f,   0.5f,  0.5f,  0.5f,  1f, 1f,  -0.5f,  0.5f,  0.5f,  0f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,  -0.5f,  0.5f, -0.5f,  0f, 1f,   0.5f,  0.5f, -0.5f,  1f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,   0.5f,  0.5f, -0.5f,  1f, 1f,   0.5f, -0.5f, -0.5f,  1f, 0f,
            -0.5f,  0.5f,  0.5f,  1f, 1f,  -0.5f,  0.5f, -0.5f,  0f, 1f,  -0.5f, -0.5f, -0.5f,  0f, 0f,
            -0.5f,  0.5f,  0.5f,  1f, 1f,  -0.5f, -0.5f, -0.5f,  0f, 0f,  -0.5f, -0.5f,  0.5f,  1f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 1f,   0.5f, -0.5f, -0.5f,  0f, 0f,   0.5f,  0.5f, -0.5f,  0f, 1f,
             0.5f,  0.5f,  0.5f,  1f, 1f,   0.5f, -0.5f,  0.5f,  1f, 0f,   0.5f, -0.5f, -0.5f,  0f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,  -0.5f,  0.5f,  0.5f,  0f, 0f,   0.5f,  0.5f,  0.5f,  1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,   0.5f,  0.5f,  0.5f,  1f, 0f,   0.5f,  0.5f, -0.5f,  1f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 1f,   0.5f, -0.5f, -0.5f,  1f, 1f,   0.5f, -0.5f,  0.5f,  1f, 0f,
            -0.5f, -0.5f, -0.5f,  0f, 1f,   0.5f, -0.5f,  0.5f,  1f, 0f,  -0.5f, -0.5f,  0.5f,  0f, 0f,
    };

    // Where each cube sits in the world.
    private static final Vector3f[] CUBE_POSITIONS = {
            new Vector3f( 0.0f,  0.0f,  0.0f),
            new Vector3f( 2.0f,  1.5f, -2.0f),
            new Vector3f(-2.5f, -1.0f, -1.5f),
            new Vector3f( 1.5f, -2.0f, -3.0f),
            new Vector3f(-1.8f,  2.2f, -4.0f),
            new Vector3f( 3.0f,  0.5f, -5.0f),
            new Vector3f(-3.2f, -1.5f, -6.0f),
            new Vector3f( 0.5f,  2.8f, -7.0f),
    };

    private ShaderProgram shader;
    private Mesh mesh;
    private Texture texture;
    private Camera camera;
    private Input input;

    private final Matrix4f model = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private float elapsed;

    @Override
    public void init(Window window) {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        texture = new Texture("textures/crate.png");
        mesh = new Mesh(VERTICES, new int[]{3, 2});

        camera = new Camera();
        input = window.input();
        input.setMouseCaptured(true);   // FPS-style mouse look

        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
        camera.processInput(input, deltaSeconds);
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
        texture.bind(0);
        shader.setUniform("uTexture", 0);

        // View + projection are the same for every cube this frame.
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", camera.viewMatrix());

        for (int i = 0; i < CUBE_POSITIONS.length; i++) {
            model.identity()
                    .translate(CUBE_POSITIONS[i])
                    .rotateY(elapsed * 0.3f + i)   // gentle per-cube spin
                    .rotateX(elapsed * 0.2f + i);
            shader.setUniform("uModel", model);
            mesh.render();
        }

        shader.unbind();
    }

    @Override
    public void dispose() {
        mesh.dispose();
        texture.dispose();
        shader.dispose();
    }
}
