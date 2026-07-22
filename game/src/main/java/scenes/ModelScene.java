package scenes;

import engine.Camera;
import engine.GameObject;
import engine.Input;
import engine.Light;
import engine.Material;
import engine.Mesh;
import engine.OBJLoader;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads a mesh from a .obj file and lights it with shaders read from disk.
 * Demonstrates {@link OBJLoader} + {@link ShaderProgram#fromFiles}.
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class ModelScene implements Scene {

    private static final int MAX_LIGHTS = 8;

    private static final Vector3f[] SPHERE_POSITIONS = {
            new Vector3f( 0f, 0f,  0f),
            new Vector3f( 2.5f, 0f, -1.5f),
            new Vector3f(-2.5f, 0f, -1.5f),
    };

    private ShaderProgram litShader;
    private ShaderProgram lampShader;
    private Mesh sphere;
    private Texture texture;
    private Camera camera;
    private Input input;

    private final List<GameObject> spheres = new ArrayList<>();
    private final List<Light> lights = new ArrayList<>();
    private final Light[] pointLights = new Light[2];
    private final GameObject[] lamps = new GameObject[2];

    private final Vector3f[] colors = {
            new Vector3f(1.0f, 0.4f, 0.2f),   // warm
            new Vector3f(0.3f, 0.5f, 1.0f),   // cool
    };

    private final Matrix4f projection = new Matrix4f();
    private float elapsed;

    @Override
    public void init(Window window) {
        litShader = ShaderProgram.fromFiles("shaders/lit.vert", "shaders/lit.frag");
        lampShader = ShaderProgram.fromFiles("shaders/lamp.vert", "shaders/lamp.frag");
        sphere = OBJLoader.load("models/sphere.obj");
        texture = new Texture("textures/crate.png");

        Material material = new Material(litShader, texture)
                .setShininess(48f).setSpecularStrength(0.7f).setAmbientStrength(0.08f);
        for (Vector3f pos : SPHERE_POSITIONS) {
            GameObject s = new GameObject(sphere, material);
            s.transform().setPosition(pos);
            spheres.add(s);
        }

        lights.add(Light.directional(new Vector3f(-0.3f, -1f, -0.5f), new Vector3f(0.2f, 0.2f, 0.22f)));
        for (int i = 0; i < pointLights.length; i++) {
            pointLights[i] = Light.point(new Vector3f(), colors[i]);
            lights.add(pointLights[i]);
            lamps[i] = new GameObject(sphere, new Material(lampShader)
                    .setTint(colors[i].x, colors[i].y, colors[i].z));
            lamps[i].transform().setScale(0.12f);
        }

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

        for (int i = 0; i < pointLights.length; i++) {
            float a = elapsed * 0.9f + i * (float) Math.PI;
            pointLights[i].position.set((float) Math.cos(a) * 3f, 1.2f, (float) Math.sin(a) * 3f);
            lamps[i].transform().setPosition(pointLights[i].position);
        }
    }

    @Override
    public void render() {
        Matrix4f view = camera.viewMatrix();

        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uLightCount", lights.size());
        for (int i = 0; i < lights.size() && i < MAX_LIGHTS; i++) {
            lights.get(i).apply(litShader, "uLights[" + i + "]");
        }
        for (GameObject s : spheres) {
            s.render();
        }

        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        for (GameObject lamp : lamps) {
            lamp.render();
        }
    }

    @Override
    public void dispose() {
        sphere.dispose();
        texture.dispose();
        litShader.dispose();
        lampShader.dispose();
    }
}
