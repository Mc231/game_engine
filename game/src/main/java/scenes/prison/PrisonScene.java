package scenes.prison;

import engine.AABB;
import engine.Geometry;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.Light;
import engine.Material;
import engine.Mesh;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Prison Break — Phase 0 MVP. First-person stealth: sneak down the cell block
 * past a patrolling guard's vision cone to the exit. Get spotted and you
 * restart at your cell. Pure stealth, hard restart.
 *
 *   Move: W/A/S/D   Sneak: Left-Ctrl   Look: mouse   Restart (after escape): R
 */
public class PrisonScene implements Scene {

    private static final float START_X = 0f, START_Z = 3f, START_YAW = 90f;   // face +Z

    private static final String CONE_VERT = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() { gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0); }
            """;
    private static final String CONE_FRAG = """
            #version 330 core
            out vec4 FragColor;
            uniform vec3 uColor;
            uniform float uAlpha;
            void main() { FragColor = vec4(uColor, uAlpha); }
            """;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram litShader;
    private ShaderProgram coneShader;
    private Mesh cubeMesh;
    private Mesh floorMesh;
    private Mesh coneMesh;
    private Material wallMat;
    private Material floorMat;
    private Material guardMat;
    private Material visorMat;

    private final List<Matrix4f> wallModels = new ArrayList<>();
    private AABB[] wallAABBs;
    private final Matrix4f floorModel = new Matrix4f().translate(0f, 0f, 20f);

    private PrisonPlayer player;
    private Guard guard;
    private AABB exit;
    private final Light[] lights = new Light[4];

    private Input input;
    private InputMap actions;
    private Hud hud;
    private Window window;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Vector3f visorPos = new Vector3f();
    private float detection = 0f;
    private float caughtFlash = 0f;
    private boolean won = false;

    @Override
    public void init(Window window) {
        this.window = window;
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        coneShader = new ShaderProgram(CONE_VERT, CONE_FRAG);
        cubeMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        floorMesh = new Mesh(Geometry.plane(40f, 40f), new int[]{3, 3, 2});
        coneMesh = buildCone();

        Texture wall = resources.texture("textures/wall.jpg");
        Texture floor = resources.texture("textures/floor.jpg");
        Texture white = resources.texture("textures/white.png");
        wallMat = new Material(litShader, wall).setAmbientStrength(0.22f).setSpecularStrength(0.05f).setShininess(6f);
        floorMat = new Material(litShader, floor).setAmbientStrength(0.22f).setSpecularStrength(0.05f).setShininess(6f);
        guardMat = new Material(litShader, white).setTint(0.22f, 0.28f, 0.45f).setAmbientStrength(0.35f);
        visorMat = new Material(litShader, white).setTint(1.0f, 0.85f, 0.3f).setAmbientStrength(0.7f);

        // --- Level: a cell-block corridor with two pillars for cover ---
        addBox(-5.5f, 1.75f, 22f, 1f, 3.5f, 46f);      // left wall
        addBox(5.5f, 1.75f, 22f, 1f, 3.5f, 46f);       // right wall
        addBox(0f, 1.75f, -0.5f, 12f, 3.5f, 1f);       // back wall (cell end)
        addBox(-3.0f, 1.6f, 22f, 1.4f, 3.2f, 3.0f);    // pillar 1
        addBox(3.0f, 1.6f, 30f, 1.4f, 3.2f, 3.0f);     // pillar 2
        wallAABBs = new AABB[wallModels.size()];
        // rebuild AABBs in the same order (addBox stored them)
        for (int i = 0; i < aabbList.size(); i++) wallAABBs[i] = aabbList.get(i);

        exit = AABB.fromCenterSize(new Vector3f(0f, 1.5f, 42f), new Vector3f(9f, 3f, 3f));

        // Ceiling lights down the corridor.
        float[] lz = {6f, 17f, 28f, 39f};
        for (int i = 0; i < lights.length; i++) {
            lights[i] = Light.point(new Vector3f(0f, 3.2f, lz[i]), new Vector3f(1.0f, 0.95f, 0.85f));
        }

        player = new PrisonPlayer();
        player.setStart(START_X, START_Z, START_YAW);
        guard = new Guard(new Vector3f[]{new Vector3f(0f, 0f, 15f), new Vector3f(0f, 0f, 32f)});

        input = window.input();
        input.setMouseCaptured(true);
        actions = new InputMap()
                .bind("forward", GLFW_KEY_W, GLFW_KEY_UP)
                .bind("back", GLFW_KEY_S, GLFW_KEY_DOWN)
                .bind("left", GLFW_KEY_A, GLFW_KEY_LEFT)
                .bind("right", GLFW_KEY_D, GLFW_KEY_RIGHT)
                .bind("sneak", GLFW_KEY_LEFT_CONTROL);
        hud = new Hud();

        projection.identity().perspective(
                (float) Math.toRadians(70.0), window.aspectRatio(), 0.1f, 200f);
    }

    private final List<AABB> aabbList = new ArrayList<>();

    private void addBox(float cx, float cy, float cz, float sx, float sy, float sz) {
        wallModels.add(new Matrix4f().translate(cx, cy, cz).scale(sx, sy, sz));
        aabbList.add(AABB.fromCenterSize(new Vector3f(cx, cy, cz), new Vector3f(sx, sy, sz)));
    }

    @Override
    public void update(float deltaSeconds) {
        if (won) {
            if (input.isKeyPressed(GLFW_KEY_R)) restart();
            return;
        }

        player.addLook(input.mouseDeltaX(), input.mouseDeltaY());
        float forward = (actions.isDown("forward", input) ? 1f : 0f) - (actions.isDown("back", input) ? 1f : 0f);
        float strafe = (actions.isDown("right", input) ? 1f : 0f) - (actions.isDown("left", input) ? 1f : 0f);
        boolean sneak = actions.isDown("sneak", input);
        player.update(deltaSeconds, forward, strafe, sneak, wallAABBs);

        guard.update(deltaSeconds);
        boolean seen = guard.canSee(player.position, wallAABBs);

        detection += (seen ? deltaSeconds / 1.1f : -deltaSeconds / 0.7f);
        detection = Math.max(0f, Math.min(1f, detection));
        if (caughtFlash > 0f) caughtFlash -= deltaSeconds;

        if (detection >= 1f) {
            caught();
        } else if (exit.contains(player.position)) {
            won = true;
        }
    }

    private void caught() {
        player.setStart(START_X, START_Z, START_YAW);
        guard.reset();
        detection = 0f;
        caughtFlash = 1.6f;
    }

    private void restart() {
        player.setStart(START_X, START_Z, START_YAW);
        guard.reset();
        detection = 0f;
        won = false;
        caughtFlash = 0f;
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective(
                (float) Math.toRadians(70.0), (float) width / height, 0.1f, 200f);
    }

    @Override
    public void render() {
        glClearColor(0.03f, 0.03f, 0.04f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        Matrix4f view = player.viewMatrix();
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", player.position);
        litShader.setUniform("uFogDensity", 0f);
        litShader.setUniform("uLightCount", lights.length);
        for (int i = 0; i < lights.length; i++) {
            lights[i].apply(litShader, "uLights[" + i + "]");
        }

        floorMat.use();
        litShader.setUniform("uModel", floorModel);
        floorMesh.render();

        wallMat.use();
        for (Matrix4f m : wallModels) {
            litShader.setUniform("uModel", m);
            cubeMesh.render();
        }

        // Guard body + visor (facing marker).
        guardMat.use();
        litShader.setUniform("uModel", model.identity().translate(guard.position.x, 0.95f, guard.position.z).scale(0.7f, 1.9f, 0.7f));
        cubeMesh.render();
        float fs = (float) Math.sin(guard.facing());
        float fc = (float) Math.cos(guard.facing());
        visorPos.set(guard.position.x + fs * 0.42f, 1.55f, guard.position.z + fc * 0.42f);
        visorMat.use();
        litShader.setUniform("uModel", model.identity().translate(visorPos).rotateY(guard.facing()).scale(0.55f, 0.22f, 0.2f));
        cubeMesh.render();

        // Vision cone on the floor (green = searching, red = spotting you).
        coneShader.bind();
        coneShader.setUniform("uProjection", projection);
        coneShader.setUniform("uView", view);
        coneShader.setUniform("uModel", model.identity().translate(guard.position.x, 0f, guard.position.z).rotateY(guard.facing()));
        coneShader.setUniform("uColor", guard.alerted() ? new Vector3f(1f, 0.25f, 0.2f) : new Vector3f(0.3f, 0.9f, 0.45f));
        coneShader.setUniform("uAlpha", 0.22f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        coneMesh.render();
        glDepthMask(true);
        glDisable(GL_BLEND);

        // HUD.
        int fbw = window.framebufferWidth();
        float cx = fbw / 2f;
        float cyy = window.framebufferHeight() / 2f;
        hud.begin(fbw, window.framebufferHeight());
        hud.text(12, 12, 2f, "PRISON BREAK  -  reach the gate unseen. WASD move, Ctrl sneak, [ ] scene", 1f, 1f, 1f);
        int n = Math.round(detection * 12f);
        StringBuilder bar = new StringBuilder("ALERT [");
        for (int i = 0; i < 12; i++) bar.append(i < n ? '#' : '.');
        bar.append(']');
        hud.text(12, 40, 2f, bar.toString(), 0.4f + detection * 0.6f, 1f - detection * 0.7f, 0.3f);
        if (player.crouching()) {
            hud.text(12, 66, 2f, "sneaking", 0.6f, 0.8f, 1f);
        }
        if (won) {
            hud.text(cx - 200f, cyy - 20f, 4f, "YOU ESCAPED - press R", 0.4f, 1f, 0.5f);
        } else if (caughtFlash > 0f) {
            hud.text(cx - 120f, cyy - 20f, 5f, "CAUGHT!", 1f, 0.3f, 0.2f);
        }
        hud.end();
    }

    private Mesh buildCone() {
        int seg = 18;
        float rng = 15f;
        float half = (float) Math.toRadians(30.0);
        List<Float> v = new ArrayList<>();
        float y = 0.06f;
        for (int i = 0; i < seg; i++) {
            float a0 = -half + 2f * half * i / seg;
            float a1 = -half + 2f * half * (i + 1) / seg;
            v.add(0f); v.add(y); v.add(0f);
            v.add((float) Math.sin(a0) * rng); v.add(y); v.add((float) Math.cos(a0) * rng);
            v.add((float) Math.sin(a1) * rng); v.add(y); v.add((float) Math.cos(a1) * rng);
        }
        float[] arr = new float[v.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = v.get(i);
        return new Mesh(arr, new int[]{3});
    }

    @Override
    public void dispose() {
        cubeMesh.dispose();
        floorMesh.dispose();
        coneMesh.dispose();
        coneShader.dispose();
        hud.dispose();
        resources.dispose();
    }
}
