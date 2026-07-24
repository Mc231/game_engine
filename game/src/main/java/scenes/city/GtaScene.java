package scenes.city;

import engine.AABB;
import engine.Geometry;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.Light;
import engine.Material;
import engine.Mesh;
import engine.OrbitCamera;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Grand Theft LWJGL (codename MiniCity) — Phase 0: third-person on-foot.
 *
 * <p>Control a low-poly {@link Avatar} in third person around a small walled test
 * block: mouse orbits the {@link OrbitCamera}, WASD moves camera-relative, Shift
 * runs, and the camera pulls in so it never clips a wall. This scene is the
 * growth point for every later phase (vehicles, city, peds, combat...).
 *
 *   Move: W/A/S/D   Run: Shift   Look: mouse
 */
public class GtaScene implements Scene {

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram litShader;
    private Mesh cubeMesh;
    private Mesh floorMesh;
    private Material floorMat;
    private Material wallMat;
    private Material curbMat;
    private Avatar avatar;

    private final List<Matrix4f> wallModels = new ArrayList<>();
    private final List<AABB> wallBoxes = new ArrayList<>();
    private AABB[] walls;

    private ThirdPersonController player;
    private OrbitCamera camera;
    private Input input;
    private InputMap actions;
    private Hud hud;
    private Window window;

    private final Light[] lights = new Light[2];
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Matrix4f floorModel = new Matrix4f();
    private final Vector3f tmp = new Vector3f();

    @Override
    public void init(Window window) {
        this.window = window;
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        cubeMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        floorMesh = new Mesh(Geometry.plane(40f, 40f), new int[]{3, 3, 2});

        Texture floorTex = resources.texture("textures/floor.jpg");
        Texture white = resources.texture("textures/white.png");
        floorMat = new Material(litShader, floorTex).setTint(0.7f, 0.72f, 0.72f).setAmbientStrength(0.35f);
        wallMat = new Material(litShader, white).setTint(0.55f, 0.57f, 0.62f).setAmbientStrength(0.35f);
        curbMat = new Material(litShader, white).setTint(0.75f, 0.72f, 0.4f).setAmbientStrength(0.4f);

        avatar = new Avatar(litShader, white, Avatar.civilian());

        buildTestBlock();
        walls = wallBoxes.toArray(new AABB[0]);

        lights[0] = Light.directional(new Vector3f(-0.4f, -1f, -0.3f), new Vector3f(0.9f, 0.88f, 0.82f));
        lights[1] = Light.point(new Vector3f(0f, 8f, 0f), new Vector3f(0.4f, 0.42f, 0.5f));

        player = new ThirdPersonController(0f, 0f, 0f);
        camera = new OrbitCamera().setDistance(6.5f).setPitch(0.5f).setTargetHeight(1.4f);

        input = window.input();
        input.setMouseCaptured(true);
        actions = new InputMap()
                .bind("forward", GLFW_KEY_W, GLFW_KEY_UP)
                .bind("back", GLFW_KEY_S, GLFW_KEY_DOWN)
                .bind("left", GLFW_KEY_A, GLFW_KEY_LEFT)
                .bind("right", GLFW_KEY_D, GLFW_KEY_RIGHT)
                .bind("run", GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT);
        hud = new Hud();

        projection.identity().perspective((float) Math.toRadians(65.0), window.aspectRatio(), 0.1f, 400f);
    }

    /** A flat courtyard: perimeter walls plus a few interior blocks to test camera pull-in. */
    private void buildTestBlock() {
        float H = 4f, half = 20f, t = 0.6f;
        addWall(0f, H / 2f, -half, half * 2f, H, t);   // north
        addWall(0f, H / 2f, half, half * 2f, H, t);    // south
        addWall(-half, H / 2f, 0f, t, H, half * 2f);   // west
        addWall(half, H / 2f, 0f, t, H, half * 2f);    // east
        // Interior obstacles (also good for camera collision + wall slide).
        addWall(-8f, 1.5f, -6f, 4f, 3f, 4f);
        addWall(9f, 1.5f, 7f, 5f, 3f, 3f);
        addWall(3f, 1f, 12f, 3f, 2f, 3f);
    }

    private void addWall(float cx, float cy, float cz, float sx, float sy, float sz) {
        wallModels.add(new Matrix4f().translate(cx, cy, cz).scale(sx, sy, sz));
        wallBoxes.add(AABB.fromCenterSize(new Vector3f(cx, cy, cz), new Vector3f(sx, sy, sz)));
    }

    @Override
    public void update(float deltaSeconds) {
        camera.addLook(input.mouseDeltaX(), input.mouseDeltaY());
        float forward = (actions.isDown("forward", input) ? 1f : 0f) - (actions.isDown("back", input) ? 1f : 0f);
        float strafe = (actions.isDown("right", input) ? 1f : 0f) - (actions.isDown("left", input) ? 1f : 0f);
        boolean run = actions.isDown("run", input);
        player.update(deltaSeconds, forward, strafe, run, camera, walls);
        avatar.animate(player.speed(), deltaSeconds);
        camera.update(player.position(), deltaSeconds, walls);
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective((float) Math.toRadians(65.0), (float) width / height, 0.1f, 400f);
    }

    @Override
    public void render() {
        glClearColor(0.5f, 0.62f, 0.75f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Matrix4f view = camera.viewMatrix();

        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uFogDensity", 0f);
        litShader.setUniform("uLightCount", lights.length);
        for (int i = 0; i < lights.length; i++) lights[i].apply(litShader, "uLights[" + i + "]");

        floorMat.use();
        litShader.setUniform("uModel", floorModel.identity());
        floorMesh.render();

        wallMat.use();
        for (Matrix4f m : wallModels) {
            litShader.setUniform("uModel", m);
            cubeMesh.render();
        }

        avatar.render(player.position(), player.facing());

        renderHud();
    }

    private void renderHud() {
        int fbw = window.framebufferWidth();
        int fbh = window.framebufferHeight();
        hud.begin(fbw, fbh);
        hud.text(12, 12, 2.2f, "GRAND THEFT LWJGL  -  Phase 0", 1f, 1f, 1f);
        hud.text(12, 40, 2f, "ON FOOT   speed " + String.format("%.1f", player.speed()), 0.8f, 0.9f, 1f);
        hud.text(12, 64, 1.7f, "WASD move   Shift run   mouse look", 0.75f, 0.8f, 0.85f);
        hud.end();
    }

    @Override
    public void dispose() {
        cubeMesh.dispose();
        floorMesh.dispose();
        avatar.dispose();
        hud.dispose();
        resources.dispose();
    }
}
