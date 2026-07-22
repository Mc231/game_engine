package scenes;

import engine.Camera;
import engine.Input;
import engine.LightComponent;
import engine.MeshRenderer;
import engine.ResourceManager;
import engine.Scene;
import engine.SceneBuilder;
import engine.SceneData;
import engine.SceneSerializer;
import engine.ShaderProgram;
import engine.Window;
import engine.World;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Loads a level from a JSON file (`levels/demo.json`) into a live ECS
 * {@link World} via {@link SceneSerializer} + {@link SceneBuilder}, then renders
 * it. The scene is authored as data — no geometry or placement is hardcoded here.
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class SerializedScene implements Scene {

    private static final int MAX_LIGHTS = 8;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram litShader;
    private World world;
    private Camera camera;
    private Input input;

    private final Matrix4f projection = new Matrix4f();

    @Override
    public void init(Window window) {
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");

        SceneData data = SceneSerializer.loadFromResource("levels/demo.json");
        world = SceneBuilder.build(data, litShader, resources);

        camera = new Camera().setPosition(0f, 3f, 12f);
        input = window.input();
        input.setMouseCaptured(true);

        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        camera.processInput(input, deltaSeconds);
        world.update(deltaSeconds);   // LightComponents sync to their entity positions
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

        List<LightComponent> lights = world.collect(LightComponent.class);
        litShader.setUniform("uLightCount", Math.min(lights.size(), MAX_LIGHTS));
        for (int i = 0; i < lights.size() && i < MAX_LIGHTS; i++) {
            lights.get(i).light().apply(litShader, "uLights[" + i + "]");
        }

        for (MeshRenderer r : world.collect(MeshRenderer.class)) {
            r.material().use();
            r.material().shader().setUniform("uModel", r.entity().worldMatrix());
            r.mesh().render();
        }
    }

    @Override
    public void dispose() {
        resources.dispose();   // owns every shader/texture/mesh the level used
    }
}
