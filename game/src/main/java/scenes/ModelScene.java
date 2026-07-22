package scenes;

import engine.Camera;
import engine.Geometry;
import engine.Input;
import engine.Light;
import engine.Mesh;
import engine.Model;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads a multi-material model (an .obj with several {@code usemtl} groups + an
 * .mtl library) via {@link Model} and lights it. Each cube of the model is a
 * separate mesh with its own material (red / green / blue from the .mtl), all
 * assets loaded through a {@link ResourceManager}.
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class ModelScene implements Scene {

    private static final int MAX_LIGHTS = 8;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram litShader;
    private ShaderProgram lampShader;
    private Model model;
    private Mesh lampMesh;
    private Camera camera;
    private Input input;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f lampMatrix = new Matrix4f();

    private final List<Light> lights = new ArrayList<>();
    private final Light[] pointLights = new Light[2];
    private final Vector3f[] pointColors = {
            new Vector3f(1.0f, 0.5f, 0.3f),
            new Vector3f(0.4f, 0.6f, 1.0f),
    };
    private float elapsed;

    @Override
    public void init(Window window) {
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        lampShader = resources.shader("shaders/lamp.vert", "shaders/lamp.frag");
        model = Model.load("models/shapes.obj", litShader, resources);
        lampMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});

        lights.add(Light.directional(new Vector3f(-0.4f, -1f, -0.5f), new Vector3f(0.25f, 0.25f, 0.28f)));
        for (int i = 0; i < pointLights.length; i++) {
            pointLights[i] = Light.point(new Vector3f(), pointColors[i]);
            lights.add(pointLights[i]);
        }

        camera = new Camera().setPosition(0f, 2f, 9f);
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
            pointLights[i].position.set((float) Math.cos(a) * 5f, 2f, (float) Math.sin(a) * 5f);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective(
                (float) Math.toRadians(45.0), (float) width / height, 0.1f, 100f);
    }

    @Override
    public void render() {
        Matrix4f view = camera.viewMatrix();

        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uLightCount", Math.min(lights.size(), MAX_LIGHTS));
        for (int i = 0; i < lights.size() && i < MAX_LIGHTS; i++) {
            lights.get(i).apply(litShader, "uLights[" + i + "]");
        }

        modelMatrix.identity().rotateY(elapsed * 0.4f);
        litShader.setUniform("uModel", modelMatrix);
        model.render();   // each part binds its own material (red/green/blue)

        // Lamp markers for the point lights.
        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        for (Light light : pointLights) {
            lampMatrix.identity().translate(light.position).scale(0.15f);
            lampShader.setUniform("uModel", lampMatrix);
            lampShader.setUniform("uTint", light.color);
            lampMesh.render();
        }
    }

    @Override
    public void dispose() {
        model.dispose();
        lampMesh.dispose();
        resources.dispose();
    }
}
