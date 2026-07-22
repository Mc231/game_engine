package scenes;

import engine.Geometry;

import engine.Camera;
import engine.GameObject;
import engine.Input;
import engine.Light;
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

import static org.lwjgl.glfw.GLFW.GLFW_KEY_F;

/**
 * Multiple typed lights in one shader: a dim DIRECTIONAL fill, three orbiting
 * colored POINT lights (each with a matching lamp marker), and a SPOT light
 * attached to the camera (a flashlight, toggle with F).
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse
 *   Flashlight: F   Quit: Esc
 */
public class LightsScene implements Scene {

    private static final int MAX_LIGHTS = 8;

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

            #define MAX_LIGHTS 8
            struct Light {
                int type;              // 0 = directional, 1 = point, 2 = spot
                vec3 position;
                vec3 direction;
                vec3 color;
                float constant;
                float linear;
                float quadratic;
                float cutOff;          // cos(inner angle)
                float outerCutOff;     // cos(outer angle)
            };
            uniform Light uLights[MAX_LIGHTS];
            uniform int uLightCount;

            uniform sampler2D uTexture;
            uniform vec3 uViewPos;
            uniform vec3 uTint;
            uniform float uAmbientStrength;
            uniform float uSpecularStrength;
            uniform float uShininess;

            vec3 calcLight(Light light, vec3 N, vec3 viewDir) {
                vec3 lightDir;
                float attenuation = 1.0;
                float intensity = 1.0;

                if (light.type == 0) {                 // directional
                    lightDir = normalize(-light.direction);
                } else {                               // point or spot
                    vec3 toLight = light.position - fragPos;
                    float dist = length(toLight);
                    lightDir = toLight / dist;
                    attenuation = 1.0 / (light.constant + light.linear * dist
                                         + light.quadratic * dist * dist);
                    if (light.type == 2) {             // spot cone
                        float theta = dot(lightDir, normalize(-light.direction));
                        float epsilon = light.cutOff - light.outerCutOff;
                        intensity = clamp((theta - light.outerCutOff) / epsilon, 0.0, 1.0);
                    }
                }

                float diff = max(dot(N, lightDir), 0.0);
                vec3 reflectDir = reflect(-lightDir, N);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), uShininess);

                vec3 diffuse = diff * light.color;
                vec3 specular = uSpecularStrength * spec * light.color;
                return (diffuse + specular) * attenuation * intensity;
            }

            void main() {
                vec3 baseColor = texture(uTexture, texCoord).rgb * uTint;
                vec3 N = normalize(normal);
                vec3 viewDir = normalize(uViewPos - fragPos);

                vec3 lighting = vec3(uAmbientStrength);
                for (int i = 0; i < uLightCount; i++) {
                    lighting += calcLight(uLights[i], N, viewDir);
                }
                FragColor = vec4(lighting * baseColor, 1.0);
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
    private final List<Light> lights = new ArrayList<>();
    private Light dirLight;
    private final Light[] pointLights = new Light[3];
    private final GameObject[] pointLamps = new GameObject[3];
    private Light spotLight;
    private boolean flashlightOn = true;

    private final Vector3f[] pointColors = {
            new Vector3f(1.0f, 0.2f, 0.2f),   // red
            new Vector3f(0.2f, 1.0f, 0.3f),   // green
            new Vector3f(0.3f, 0.4f, 1.0f),   // blue
    };

    private final Matrix4f projection = new Matrix4f();
    private float elapsed;

    @Override
    public void init(Window window) {
        litShader = new ShaderProgram(LIT_VERTEX, LIT_FRAGMENT);
        lampShader = new ShaderProgram(LAMP_VERTEX, LAMP_FRAGMENT);
        texture = new Texture("textures/crate.png");
        mesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});

        // A few materials (low ambient so the colored lights show clearly).
        Material[] palette = {
                new Material(litShader, texture).setShininess(32f).setSpecularStrength(0.6f).setAmbientStrength(0.05f),
                new Material(litShader, texture).setShininess(96f).setSpecularStrength(0.9f).setAmbientStrength(0.05f),
                new Material(litShader, texture).setShininess(8f).setSpecularStrength(0.2f).setAmbientStrength(0.05f),
        };
        for (int i = 0; i < CUBE_POSITIONS.length; i++) {
            GameObject cube = new GameObject(mesh, palette[i % palette.length]);
            cube.transform().setPosition(CUBE_POSITIONS[i]);
            cubes.add(cube);
        }

        // --- Lights ---
        dirLight = Light.directional(new Vector3f(-0.3f, -1.0f, -0.5f), new Vector3f(0.18f, 0.18f, 0.22f));
        lights.add(dirLight);

        for (int i = 0; i < pointLights.length; i++) {
            pointLights[i] = Light.point(new Vector3f(), pointColors[i]);
            lights.add(pointLights[i]);
            // Each point light gets a small lamp cube tinted its color.
            pointLamps[i] = new GameObject(mesh, new Material(lampShader)
                    .setTint(pointColors[i].x, pointColors[i].y, pointColors[i].z));
            pointLamps[i].transform().setScale(0.15f);
        }

        spotLight = Light.spot(new Vector3f(), new Vector3f(0, 0, -1), new Vector3f(1f, 1f, 0.95f))
                .setCone(12.5f, 18f);
        lights.add(spotLight);

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

        if (input.isKeyPressed(GLFW_KEY_F)) {
            flashlightOn = !flashlightOn;
        }

        // Orbit the three point lights at different radii/phases/heights.
        float[] radius = {3.0f, 3.6f, 2.6f};
        float[] height = {0.6f, 1.8f, -1.2f};
        for (int i = 0; i < pointLights.length; i++) {
            float a = elapsed * 0.8f + i * (float) (2.0 * Math.PI / 3.0);
            pointLights[i].position.set(
                    (float) Math.cos(a) * radius[i], height[i], (float) Math.sin(a) * radius[i] - 2.0f);
            pointLamps[i].transform().setPosition(pointLights[i].position);
        }

        // Flashlight follows the camera.
        spotLight.position.set(camera.position());
        spotLight.direction.set(camera.front());
        if (flashlightOn) {
            spotLight.color.set(1f, 1f, 0.95f);
        } else {
            spotLight.color.set(0f, 0f, 0f);
        }

        // Spin the cubes a little.
        for (int i = 0; i < cubes.size(); i++) {
            cubes.get(i).transform().setRotationEuler(0f, elapsed * 0.15f + i, 0f);
        }
    }

    @Override
    public void render() {
        Matrix4f view = camera.viewMatrix();

        // Frame-wide uniforms for the lit shader, including the whole light array.
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uLightCount", lights.size());
        for (int i = 0; i < lights.size() && i < MAX_LIGHTS; i++) {
            lights.get(i).apply(litShader, "uLights[" + i + "]");
        }
        for (GameObject cube : cubes) {
            cube.render();
        }

        // Lamp markers for the point lights.
        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        for (GameObject lamp : pointLamps) {
            lamp.render();
        }
    }

    @Override
    public void dispose() {
        mesh.dispose();
        texture.dispose();
        litShader.dispose();
        lampShader.dispose();
    }
}
