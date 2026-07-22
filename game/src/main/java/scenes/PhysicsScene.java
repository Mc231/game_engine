package scenes;

import engine.AABB;
import engine.Camera;
import engine.Geometry;
import engine.Hud;
import engine.Input;
import engine.Intersect;
import engine.Light;
import engine.Material;
import engine.Mesh;
import engine.Ray;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Transform;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Raycasting / AABB picking demo. A ray is cast from the camera along its look
 * direction each frame; the nearest cube whose bounding box it hits is
 * highlighted. Demonstrates {@link Ray}, {@link AABB}, {@link Intersect}.
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse (crosshair picks)   Quit: Esc
 */
public class PhysicsScene implements Scene {

    private static final Vector3f[] POSITIONS = {
            new Vector3f(0, 0, 0), new Vector3f(3, 1, -2), new Vector3f(-3, -1, -1),
            new Vector3f(2, -2, -4), new Vector3f(-2, 2, -3), new Vector3f(4, 0, -6),
            new Vector3f(-4, 1, -5), new Vector3f(0, 3, -7), new Vector3f(1, -1, 2),
    };

    private ShaderProgram litShader;
    private Mesh cubeMesh;
    private Texture texture;
    private Material material;
    private Camera camera;
    private Input input;
    private Hud hud;

    private final Matrix4f projection = new Matrix4f();
    private final Transform[] transforms = new Transform[POSITIONS.length];
    private final AABB[] boxes = new AABB[POSITIONS.length];
    private final Vector3f cubeSize = new Vector3f(1f, 1f, 1f);
    private final Vector3f lightDir = new Vector3f(-0.3f, -1f, -0.5f).normalize();
    private int hitIndex = -1;
    private float hitDistance = -1f;
    private Window window;

    @Override
    public void init(Window window) {
        this.window = window;
        litShader = ShaderProgram.fromFiles("shaders/lit.vert", "shaders/lit.frag");
        cubeMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        texture = new Texture("textures/crate.png");
        material = new Material(litShader, texture);
        for (int i = 0; i < POSITIONS.length; i++) {
            transforms[i] = new Transform().setPosition(POSITIONS[i]);
            boxes[i] = AABB.fromCenterSize(POSITIONS[i], cubeSize);
        }
        camera = new Camera().setPosition(0f, 1f, 9f);
        input = window.input();
        input.setMouseCaptured(true);
        hud = new Hud();
        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        camera.processInput(input, deltaSeconds);

        // Cast a ray from the camera along its look direction; pick the nearest hit.
        Ray ray = new Ray(camera.position(), camera.front());
        hitIndex = -1;
        hitDistance = Float.MAX_VALUE;
        for (int i = 0; i < boxes.length; i++) {
            float t = Intersect.rayAABB(ray, boxes[i]);
            if (t >= 0f && t < hitDistance) {
                hitDistance = t;
                hitIndex = i;
            }
        }
        if (hitIndex < 0) {
            hitDistance = -1f;
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
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", camera.viewMatrix());
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uLightCount", 1);
        Light sun = Light.directional(lightDir, new Vector3f(1f, 1f, 1f));
        sun.apply(litShader, "uLights[0]");
        material.use();

        for (int i = 0; i < transforms.length; i++) {
            litShader.setUniform("uModel", transforms[i].matrix());
            litShader.setUniform("uTint", i == hitIndex
                    ? new Vector3f(1f, 0.4f, 0.3f)     // highlight the picked cube
                    : new Vector3f(1f, 1f, 1f));
            cubeMesh.render();
        }

        hud.begin(window.framebufferWidth(), window.framebufferHeight());
        hud.text(12, 12, 2f, "RAYCAST PICKING  -  aim the crosshair at a cube", 1f, 1f, 1f);
        hud.text(12, 40, 2f, hitIndex >= 0
                ? String.format("hit cube %d at distance %.1f", hitIndex, hitDistance)
                : "no hit", 1f, 0.95f, 0.5f);
        hud.end();
    }

    @Override
    public void dispose() {
        cubeMesh.dispose();
        texture.dispose();
        hud.dispose();
        litShader.dispose();
    }
}
