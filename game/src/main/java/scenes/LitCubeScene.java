package scenes;

import engine.Geometry;

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
 * Phong-lit textured cubes with a fly camera and an orbiting light.
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 *
 * Phong lighting = ambient + diffuse + specular:
 *   - ambient:  constant fill so shadowed faces aren't pure black.
 *   - diffuse:  brighter the more a face points AT the light (angle-based).
 *   - specular: a shiny highlight that depends on the VIEWER's position.
 *
 * The small white cube marks the light's position (drawn with a simple
 * unlit shader that just outputs the light color).
 */
public class LitCubeScene implements Scene {

    // Lit objects: read position + normal + uv.
    private static final String LIT_VERTEX = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            layout (location = 2) in vec2 aTexCoord;
            out vec3 fragPos;
            out vec3 normal;
            out vec2 texCoord;
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() {
                vec4 world = uModel * vec4(aPos, 1.0);
                fragPos = world.xyz;
                // Normal matrix: keeps normals correct under rotation/scale.
                normal = mat3(transpose(inverse(uModel))) * aNormal;
                texCoord = aTexCoord;
                gl_Position = uProjection * uView * world;
            }
            """;

    private static final String LIT_FRAGMENT = """
            #version 330 core
            in vec3 fragPos;
            in vec3 normal;
            in vec2 texCoord;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            uniform vec3 uLightPos;
            uniform vec3 uLightColor;
            uniform vec3 uViewPos;
            void main() {
                vec3 baseColor = texture(uTexture, texCoord).rgb;
                vec3 N = normalize(normal);
                vec3 lightDir = normalize(uLightPos - fragPos);

                // ambient
                vec3 ambient = 0.15 * uLightColor;

                // diffuse: how directly the surface faces the light
                float diff = max(dot(N, lightDir), 0.0);
                vec3 diffuse = diff * uLightColor;

                // specular: highlight toward the viewer
                vec3 viewDir = normalize(uViewPos - fragPos);
                vec3 reflectDir = reflect(-lightDir, N);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
                vec3 specular = 0.5 * spec * uLightColor;

                vec3 result = (ambient + diffuse + specular) * baseColor;
                FragColor = vec4(result, 1.0);
            }
            """;

    // Lamp marker: only needs position; outputs a flat color.
    private static final String LAMP_VERTEX = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() {
                gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0);
            }
            """;

    private static final String LAMP_FRAGMENT = """
            #version 330 core
            out vec4 FragColor;
            uniform vec3 uLightColor;
            void main() {
                FragColor = vec4(uLightColor, 1.0);
            }
            """;

    private static final Vector3f[] CUBE_POSITIONS = {
            new Vector3f( 0.0f,  0.0f,  0.0f),
            new Vector3f( 2.0f,  1.5f, -2.0f),
            new Vector3f(-2.5f, -1.0f, -1.5f),
            new Vector3f( 1.5f, -2.0f, -3.0f),
            new Vector3f(-1.8f,  2.2f, -4.0f),
            new Vector3f( 2.5f,  0.5f, -5.0f),
    };

    private ShaderProgram litShader;
    private ShaderProgram lampShader;
    private Mesh mesh;
    private Texture texture;
    private Camera camera;
    private Input input;

    private final Matrix4f model = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private final Vector3f lightPos = new Vector3f();
    private final Vector3f lightColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private float elapsed;

    @Override
    public void init(Window window) {
        litShader = new ShaderProgram(LIT_VERTEX, LIT_FRAGMENT);
        lampShader = new ShaderProgram(LAMP_VERTEX, LAMP_FRAGMENT);
        texture = new Texture("textures/crate.png");
        mesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});

        camera = new Camera();
        input = window.input();
        input.setMouseCaptured(true);

        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
        camera.processInput(input, deltaSeconds);

        // Light orbits the origin on the XZ plane.
        float radius = 4.0f;
        lightPos.set(
                (float) Math.cos(elapsed) * radius,
                1.5f,
                (float) Math.sin(elapsed) * radius);
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;                 // ignore minimized/degenerate
        projection.identity().perspective(
                (float) Math.toRadians(45.0), (float) width / height, 0.1f, 100f);
    }

    @Override
    public void render() {
        Matrix4f view = camera.viewMatrix();

        // --- Lit cubes ---
        litShader.bind();
        texture.bind(0);
        litShader.setUniform("uTexture", 0);
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uLightPos", lightPos);
        litShader.setUniform("uLightColor", lightColor);
        litShader.setUniform("uViewPos", camera.position());
        for (Vector3f pos : CUBE_POSITIONS) {
            model.identity().translate(pos).rotateY(elapsed * 0.2f);
            litShader.setUniform("uModel", model);
            mesh.render();
        }
        litShader.unbind();

        // --- Lamp marker at the light's position ---
        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        lampShader.setUniform("uLightColor", lightColor);
        model.identity().translate(lightPos).scale(0.2f);
        lampShader.setUniform("uModel", model);
        mesh.render();
        lampShader.unbind();
    }

    @Override
    public void dispose() {
        mesh.dispose();
        texture.dispose();
        litShader.dispose();
        lampShader.dispose();
    }
}
