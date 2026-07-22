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
 * Shows off {@link Material}: one lit shader + one cube mesh, but several
 * materials with different tint / shininess / specular strength, so cubes look
 * distinct (glossy red, matte blue, plain crate) without any new shaders.
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class MaterialScene implements Scene {

    // Lit shader that reads material parameters as uniforms.
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

            // --- material parameters ---
            uniform vec3 uTint;
            uniform float uAmbientStrength;
            uniform float uSpecularStrength;
            uniform float uShininess;

            void main() {
                vec3 baseColor = texture(uTexture, texCoord).rgb * uTint;
                vec3 N = normalize(normal);
                vec3 lightDir = normalize(uLightPos - fragPos);

                vec3 ambient = uAmbientStrength * uLightColor;

                float diff = max(dot(N, lightDir), 0.0);
                vec3 diffuse = diff * uLightColor;

                vec3 viewDir = normalize(uViewPos - fragPos);
                vec3 reflectDir = reflect(-lightDir, N);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), uShininess);
                vec3 specular = uSpecularStrength * spec * uLightColor;

                FragColor = vec4((ambient + diffuse + specular) * baseColor, 1.0);
            }
            """;

    // Lamp marker: flat color from its material's tint.
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
            uniform vec3 uTint;
            void main() {
                FragColor = vec4(uTint, 1.0);
            }
            """;

    private static final Vector3f[] CUBE_POSITIONS = {
            new Vector3f( 0.0f,  0.0f,  0.0f),
            new Vector3f( 2.2f,  1.2f, -2.0f),
            new Vector3f(-2.4f, -0.8f, -1.5f),
            new Vector3f( 1.4f, -1.8f, -3.0f),
            new Vector3f(-1.6f,  2.0f, -4.0f),
            new Vector3f( 2.6f,  0.4f, -5.0f),
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

        // Three materials, ALL sharing litShader + the crate texture, differing
        // only in parameters. This is the whole point of a Material.
        Material[] palette = {
                new Material(litShader, texture)                       // plain crate
                        .setShininess(32f).setSpecularStrength(0.5f),
                new Material(litShader, texture)                       // glossy red
                        .setTint(1.0f, 0.7f, 0.65f).setShininess(128f).setSpecularStrength(0.9f),
                new Material(litShader, texture)                       // matte blue
                        .setTint(0.65f, 0.8f, 1.0f).setShininess(6f).setSpecularStrength(0.12f),
        };

        for (int i = 0; i < CUBE_POSITIONS.length; i++) {
            GameObject cube = new GameObject(mesh, palette[i % palette.length]);
            cube.transform().setPosition(CUBE_POSITIONS[i]);
            cubes.add(cube);
        }

        // Lamp: flat white via its tint.
        lamp = new GameObject(mesh, new Material(lampShader).setTint(1f, 1f, 1f));
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

        for (int i = 0; i < cubes.size(); i++) {
            cubes.get(i).transform().setRotationEuler(0f, elapsed * 0.2f + i, 0f);
        }

        float radius = 4.0f;
        lightPos.set((float) Math.cos(elapsed) * radius, 1.5f, (float) Math.sin(elapsed) * radius);
        lamp.transform().setPosition(lightPos);
    }

    @Override
    public void render() {
        Matrix4f view = camera.viewMatrix();

        // Frame-wide uniforms for the lit shader (shared by every lit material).
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uLightPos", lightPos);
        litShader.setUniform("uLightColor", lightColor);
        litShader.setUniform("uViewPos", camera.position());
        for (GameObject cube : cubes) {
            cube.render();   // material.use() sets this cube's tint/shininess/etc.
        }

        // Lamp uses its own shader; only needs camera matrices.
        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        lamp.render();
    }

    @Override
    public void dispose() {
        mesh.dispose();
        texture.dispose();
        litShader.dispose();
        lampShader.dispose();
    }
}
