package scenes;

import engine.Camera;
import engine.Component;
import engine.Entity;
import engine.Geometry;
import engine.Input;
import engine.Light;
import engine.LightComponent;
import engine.Material;
import engine.Mesh;
import engine.MeshRenderer;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import engine.World;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Entity–component + scene-graph demo, with all assets loaded through a
 * {@link ResourceManager}. A rotating hub carries four orbiting cubes (each
 * spinning on its own axis via a script component); one cube carries a small
 * "moon" child — a 3-level hierarchy. Two point lights are themselves entities
 * (a {@link LightComponent} copies their world position into the light).
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class EcsScene implements Scene {

    private static final int MAX_LIGHTS = 8;

    /** A tiny "script" component: spins its entity around the given axes. */
    private static final class Spin extends Component {
        private final float sx;
        private final float sy;
        private final float sz;
        private float ax;
        private float ay;
        private float az;

        Spin(float sx, float sy, float sz) {
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
        }

        @Override
        public void update(float dt) {
            ax += sx * dt;
            ay += sy * dt;
            az += sz * dt;
            entity.transform().setRotationEuler(ax, ay, az);
        }
    }

    private final ResourceManager resources = new ResourceManager();
    private final World world = new World();

    private ShaderProgram litShader;
    private ShaderProgram lampShader;
    private Mesh cubeMesh;
    private Camera camera;
    private Input input;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Vector3f[] pointColors = {
            new Vector3f(1.0f, 0.4f, 0.2f),
            new Vector3f(0.3f, 0.5f, 1.0f),
    };

    @Override
    public void init(Window window) {
        // Assets via the cache (loaded once, disposed centrally).
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        lampShader = resources.shader("shaders/lamp.vert", "shaders/lamp.frag");
        Texture texture = resources.texture("textures/crate.png");
        cubeMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});

        Material material = new Material(litShader, texture)
                .setShininess(48f).setSpecularStrength(0.6f).setAmbientStrength(0.1f);

        // --- Scene graph: a spinning hub with four orbiting, self-spinning cubes ---
        Entity hub = new Entity("hub").add(new Spin(0f, 0.4f, 0f));
        Vector3f[] offsets = {
                new Vector3f(3, 0, 0), new Vector3f(-3, 0, 0),
                new Vector3f(0, 0, 3), new Vector3f(0, 0, -3),
        };
        for (int i = 0; i < offsets.length; i++) {
            Entity cube = new Entity("cube" + i)
                    .add(new MeshRenderer(cubeMesh, material))
                    .add(new Spin(0.6f, 0.9f, 0f));
            cube.transform().setPosition(offsets[i]);
            hub.addChild(cube);

            // A "moon" grandchild on the first cube (3-level hierarchy).
            if (i == 0) {
                Entity moon = new Entity("moon").add(new MeshRenderer(cubeMesh, material));
                moon.transform().setPosition(1.4f, 0f, 0f).setScale(0.4f);
                cube.addChild(moon);
            }
        }
        world.add(hub);

        // --- A dim directional light + two orbiting point lights (as entities) ---
        Entity sun = new Entity("sun").add(new LightComponent(
                Light.directional(new Vector3f(-0.3f, -1f, -0.5f), new Vector3f(0.18f, 0.18f, 0.22f))));
        world.add(sun);

        Entity lightHub = new Entity("lightHub").add(new Spin(0f, 0.8f, 0f));
        for (int i = 0; i < pointColors.length; i++) {
            Entity lamp = new Entity("light" + i).add(new LightComponent(
                    Light.point(new Vector3f(), pointColors[i])));
            lamp.transform().setPosition(i == 0 ? 4.5f : -4.5f, 1.5f, 0f);
            lightHub.addChild(lamp);
        }
        world.add(lightHub);

        camera = new Camera().setPosition(0f, 5f, 12f);
        input = window.input();
        input.setMouseCaptured(true);

        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        camera.processInput(input, deltaSeconds);
        world.update(deltaSeconds);   // updates every component (spins, light sync)
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

        // Lit pass: frame-wide uniforms + all lights, then every mesh renderer.
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

        // Lamp markers at each point light's position.
        lampShader.bind();
        lampShader.setUniform("uProjection", projection);
        lampShader.setUniform("uView", view);
        for (LightComponent lc : lights) {
            if (lc.light().type != Light.Type.POINT) {
                continue;
            }
            model.identity().translate(lc.light().position).scale(0.2f);
            lampShader.setUniform("uModel", model);
            lampShader.setUniform("uTint", lc.light().color);
            cubeMesh.render();
        }
    }

    @Override
    public void dispose() {
        cubeMesh.dispose();
        resources.dispose();   // disposes the shaders + texture it loaded
    }
}
