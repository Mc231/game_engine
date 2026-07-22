package scenes;

import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;

/**
 * A textured, tumbling cube. Each vertex now carries a position plus a UV
 * texture coordinate; the fragment shader samples the image at that UV instead
 * of using a flat color.
 */
public class TexturedCubeScene implements Scene {

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
            uniform sampler2D uTexture;   // reads from the bound texture unit
            void main() {
                FragColor = texture(uTexture, texCoord);
            }
            """;

    // 36 vertices, each: position(3) + UV(2). Every face maps the full image
    // (UV corners 0,0 -> 1,1). UV (0,0) is bottom-left of the image.
    private static final float[] VERTICES = {
            // ---- front (z = +0.5) ----
            -0.5f, -0.5f,  0.5f,  0f, 0f,
             0.5f, -0.5f,  0.5f,  1f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 1f,
            -0.5f, -0.5f,  0.5f,  0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 1f,

            // ---- back (z = -0.5) ----
            -0.5f, -0.5f, -0.5f,  0f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,
             0.5f,  0.5f, -0.5f,  1f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 1f,
             0.5f, -0.5f, -0.5f,  1f, 0f,

            // ---- left (x = -0.5) ----
            -0.5f,  0.5f,  0.5f,  1f, 1f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,
            -0.5f,  0.5f,  0.5f,  1f, 1f,
            -0.5f, -0.5f, -0.5f,  0f, 0f,
            -0.5f, -0.5f,  0.5f,  1f, 0f,

            // ---- right (x = +0.5) ----
             0.5f,  0.5f,  0.5f,  1f, 1f,
             0.5f, -0.5f, -0.5f,  0f, 0f,
             0.5f,  0.5f, -0.5f,  0f, 1f,
             0.5f,  0.5f,  0.5f,  1f, 1f,
             0.5f, -0.5f,  0.5f,  1f, 0f,
             0.5f, -0.5f, -0.5f,  0f, 0f,

            // ---- top (y = +0.5) ----
            -0.5f,  0.5f, -0.5f,  0f, 1f,
            -0.5f,  0.5f,  0.5f,  0f, 0f,
             0.5f,  0.5f,  0.5f,  1f, 0f,
            -0.5f,  0.5f, -0.5f,  0f, 1f,
             0.5f,  0.5f,  0.5f,  1f, 0f,
             0.5f,  0.5f, -0.5f,  1f, 1f,

            // ---- bottom (y = -0.5) ----
            -0.5f, -0.5f, -0.5f,  0f, 1f,
             0.5f, -0.5f, -0.5f,  1f, 1f,
             0.5f, -0.5f,  0.5f,  1f, 0f,
            -0.5f, -0.5f, -0.5f,  0f, 1f,
             0.5f, -0.5f,  0.5f,  1f, 0f,
            -0.5f, -0.5f,  0.5f,  0f, 0f,
    };

    private ShaderProgram shader;
    private Mesh mesh;
    private Texture texture;

    private final Matrix4f model = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private float elapsed;

    @Override
    public void init(Window window) {
        shader = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        texture = new Texture("textures/crate.png");
        mesh = new Mesh(VERTICES, new int[]{3, 2});   // layout: position(3), uv(2)

        view.identity().translate(0f, 0f, -3f);
        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
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

        texture.bind(0);                    // bind to texture unit 0
        shader.setUniform("uTexture", 0);   // tell the sampler to read unit 0

        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", view);
        shader.setUniform("uModel", model);

        mesh.render();
        shader.unbind();
    }

    @Override
    public void dispose() {
        mesh.dispose();
        texture.dispose();
        shader.dispose();
    }
}
