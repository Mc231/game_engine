package scenes;

import engine.Geometry;

import engine.Camera;
import engine.GameObject;
import engine.Input;
import engine.Material;
import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Same Phong-lit, fly-through scene as {@link LitCubeScene}, but built from
 * {@link GameObject}s with {@link engine.Transform}s. The scene now just holds
 * a list of objects and updates their transforms — no hand-written matrices,
 * no per-object boilerplate in render().
 */
public class GameObjectScene implements Scene {

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
                vec3 ambient = 0.15 * uLightColor;
                float diff = max(dot(N, lightDir), 0.0);
                vec3 diffuse = diff * uLightColor;
                vec3 viewDir = normalize(uViewPos - fragPos);
                vec3 reflectDir = reflect(-lightDir, N);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
                vec3 specular = 0.5 * spec * uLightColor;
                FragColor = vec4((ambient + diffuse + specular) * baseColor, 1.0);
            }
            """;

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

    private final List<GameObject> cubes = new ArrayList<>();
    private GameObject lamp;

    private final Matrix4f projection = new Matrix4f();
    private final Vector3f lightPos = new Vector3f();
    private final Vector3f lightColor = new Vector3f(1f, 1f, 1f);
    private float elapsed;

    @Override
    public void init(Window window) {
        litShader = new ShaderProgram(LIT_VERTEX, LIT_FRAGMENT);
        lampShader = new ShaderProgram(LAMP_VERTEX, LAMP_FRAGMENT);
        texture = new Texture("textures/crate.png");
        mesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});

        // One material (shader + texture) shared by every cube.
        Material litMaterial = new Material(litShader, texture);
        for (Vector3f pos : CUBE_POSITIONS) {
            GameObject cube = new GameObject(mesh, litMaterial);
            cube.transform().setPosition(pos);
            cubes.add(cube);
        }
        // The lamp shares the same mesh, uses the flat lamp material, small scale.
        lamp = new GameObject(mesh, new Material(lampShader));
        lamp.transform().setScale(0.2f);

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

        // Spin each cube (a little variation per index).
        for (int i = 0; i < cubes.size(); i++) {
            cubes.get(i).transform().setRotationEuler(0f, elapsed * 0.2f + i, 0f);
        }

        // Orbit the light, and move the lamp to match.
        float radius = 4.0f;
        lightPos.set((float) Math.cos(elapsed) * radius, 1.5f, (float) Math.sin(elapsed) * radius);
        lamp.transform().setPosition(lightPos);
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

        // Lit objects: set the shared uniforms once, then let each object draw itself.
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uLightPos", lightPos);
        litShader.setUniform("uLightColor", lightColor);
        litShader.setUniform("uViewPos", camera.position());
        for (GameObject cube : cubes) {
            cube.render();
        }
        litShader.unbind();

        // Lamp marker.
        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        lampShader.setUniform("uLightColor", lightColor);
        lamp.render();
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
