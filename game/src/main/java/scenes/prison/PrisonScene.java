package scenes.prison;

import engine.AABB;
import engine.Audio;
import engine.CubemapTexture;
import engine.Geometry;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.InstancedMesh;
import engine.Light;
import engine.Material;
import engine.Mesh;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Skybox;
import engine.Sound;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Prison Break — a first-person stealth escape. Sneak from your cell through a
 * guard room (grab KEYCARD A), across the yard (grab KEYCARD B), and out the
 * gate — unseen. Guards' vision cones show on the floor (green = searching,
 * red = spotting you); get seen too long and you restart at your cell.
 *
 *   Move: W/A/S/D   Sneak: Ctrl   Use/open: E   Look: mouse   Start/Restart: Enter/R
 */
public class PrisonScene implements Scene {

    private enum State { TITLE, PLAYING, WIN }

    private static final float START_X = 0f, START_Z = 3f, START_YAW = 90f;

    private static final String FLAT_VERT = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform mat4 uModel; uniform mat4 uView; uniform mat4 uProjection;
            void main() { gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0); }
            """;
    private static final String FLAT_FRAG = """
            #version 330 core
            out vec4 FragColor; uniform vec3 uColor; uniform float uAlpha;
            void main() { FragColor = vec4(uColor, uAlpha); }
            """;
    private static final String BAR_VERT = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            layout (location = 2) in vec2 aUv;
            layout (location = 3) in mat4 aInstance;
            out vec3 vNormal;
            uniform mat4 uView; uniform mat4 uProjection;
            void main() {
                vNormal = mat3(aInstance) * aNormal;
                gl_Position = uProjection * uView * aInstance * vec4(aPos, 1.0);
            }
            """;
    private static final String BAR_FRAG = """
            #version 330 core
            in vec3 vNormal;
            out vec4 FragColor;
            uniform vec3 uColor; uniform vec3 uLightDir;
            void main() {
                float d = max(dot(normalize(vNormal), normalize(-uLightDir)), 0.0);
                FragColor = vec4(uColor * (0.35 + 0.7 * d), 1.0);
            }
            """;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram litShader;
    private ShaderProgram litTriShader;
    private ShaderProgram flatShader;
    private ShaderProgram barShader;
    private Mesh cubeMesh;
    private Mesh floorMesh;
    private Mesh coneMesh;
    private InstancedMesh bars;
    private Material guardMat, visorMat, cardMat, doorMat, fixtureMat;
    private Texture wallTex, wallN, floorTex, floorN;
    private Skybox skybox;

    private final List<Matrix4f> wallModels = new ArrayList<>();
    private final List<AABB> staticWalls = new ArrayList<>();
    private final List<Door> doors = new ArrayList<>();
    private final List<Keycard> keycards = new ArrayList<>();
    private final List<Guard> guards = new ArrayList<>();
    private AABB[] activeWalls;
    private AABB exit;
    private final Matrix4f floorModel = new Matrix4f().translate(0f, 0f, 43f);
    private final Light[] lights = new Light[3];
    private final Vector3f[] fixtures = {new Vector3f(0f, 3.88f, 15f), new Vector3f(0f, 3.88f, 40f)};

    private PrisonPlayer player;
    private final Set<String> inventory = new HashSet<>();
    private Input input;
    private InputMap actions;
    private Hud hud;
    private Audio audio;
    private Sound sPickup, sUnlock, sCaught, sEscape, sStep;
    private Window window;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Vector3f tmp = new Vector3f();
    private final Vector3f barLight = new Vector3f(-0.3f, -1f, -0.25f).normalize();

    private State state = State.TITLE;
    private float detection = 0f, caughtFlash = 0f, promptTimer = 0f, spin = 0f, stepTimer = 0f, time = 0f;
    private String lockedMsg = "";

    @Override
    public void init(Window window) {
        this.window = window;
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        litTriShader = resources.shader("shaders/lit.vert", "shaders/lit_tri.frag");
        flatShader = new ShaderProgram(FLAT_VERT, FLAT_FRAG);
        barShader = new ShaderProgram(BAR_VERT, BAR_FRAG);
        cubeMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        floorMesh = new Mesh(Geometry.plane(48f, 48f), new int[]{3, 3, 2});
        coneMesh = buildCone();
        skybox = new Skybox(new CubemapTexture(
                "skybox/right.png", "skybox/left.png", "skybox/top.png",
                "skybox/bottom.png", "skybox/front.png", "skybox/back.png"));

        wallTex = resources.texture("textures/wall.jpg");
        wallN = resources.texture("textures/wall_n.jpg");
        floorTex = resources.texture("textures/floor.jpg");
        floorN = resources.texture("textures/floor_n.jpg");
        Texture white = resources.texture("textures/white.png");
        guardMat = new Material(litShader, white).setTint(0.22f, 0.28f, 0.45f).setAmbientStrength(0.4f);
        visorMat = new Material(litShader, white).setTint(1.0f, 0.85f, 0.3f).setAmbientStrength(0.8f);
        cardMat = new Material(litShader, white).setAmbientStrength(0.9f);
        doorMat = new Material(litShader, white).setTint(0.45f, 0.32f, 0.2f).setAmbientStrength(0.35f);
        fixtureMat = new Material(litShader, white).setTint(1.0f, 0.95f, 0.8f).setAmbientStrength(1.0f);

        buildLevel();
        bars = buildBars();

        lights[0] = Light.directional(new Vector3f(-0.3f, -1f, -0.25f), new Vector3f(0.55f, 0.55f, 0.6f));
        lights[1] = Light.point(new Vector3f(0f, 3.6f, 15f), new Vector3f(1f, 0.9f, 0.7f));
        lights[2] = Light.point(new Vector3f(0f, 3.6f, 40f), new Vector3f(1f, 0.9f, 0.7f));

        player = new PrisonPlayer();
        player.setStart(START_X, START_Z, START_YAW);

        input = window.input();
        actions = new InputMap()
                .bind("forward", GLFW_KEY_W, GLFW_KEY_UP)
                .bind("back", GLFW_KEY_S, GLFW_KEY_DOWN)
                .bind("left", GLFW_KEY_A, GLFW_KEY_LEFT)
                .bind("right", GLFW_KEY_D, GLFW_KEY_RIGHT)
                .bind("sneak", GLFW_KEY_LEFT_CONTROL);
        hud = new Hud();
        audio = new Audio();
        sPickup = new Sound("sounds/pickup.wav");
        sUnlock = new Sound("sounds/unlock.wav");
        sCaught = new Sound("sounds/caught.wav");
        sEscape = new Sound("sounds/escape.wav");
        sStep = new Sound("sounds/step.wav");

        projection.identity().perspective((float) Math.toRadians(70.0), window.aspectRatio(), 0.1f, 300f);
    }

    private void buildLevel() {
        float H = 4f;
        addWall(-8f, 2f, 43f, 0.6f, H, 86f);
        addWall(8f, 2f, 43f, 0.6f, H, 86f);
        addWall(0f, 2f, 0f, 16.6f, H, 0.6f);
        addWall(0f, 4f, 25f, 16f, 0.3f, 50f);       // indoor ceiling (blocks sky over z 0..50)
        divider(30f, null);
        divider(50f, "A");
        divider(82f, "B");
        addWall(5f, 0.5f, 40f, 2.2f, 1f, 1.2f);      // desk

        keycards.add(new Keycard("A", 5f, 1.15f, 40f, new Vector3f(0.2f, 0.9f, 1f)));
        keycards.add(new Keycard("B", -6f, 1f, 66f, new Vector3f(1f, 0.4f, 0.7f)));

        guards.add(new Guard(new Vector3f[]{new Vector3f(0f, 0f, 13f), new Vector3f(0f, 0f, 27f)}));
        guards.add(new Guard(new Vector3f[]{new Vector3f(-5f, 0f, 35f), new Vector3f(5f, 0f, 46f)}));
        guards.add(new Guard(new Vector3f[]{new Vector3f(-8f, 0f, 58f), new Vector3f(8f, 0f, 76f)}));

        exit = AABB.fromCenterSize(new Vector3f(0f, 1.5f, 84.5f), new Vector3f(3f, 3f, 3f));
        rebuildColliders();
    }

    private void divider(float z, String key) {
        addWall(-4.75f, 2f, z, 6.5f, 4f, 0.6f);
        addWall(4.75f, 2f, z, 6.5f, 4f, 0.6f);
        if (key != null) {
            doors.add(new Door(key, 0f, 1.5f, z, 3f, 3f, 0.5f));
        }
    }

    private void addWall(float cx, float cy, float cz, float sx, float sy, float sz) {
        wallModels.add(new Matrix4f().translate(cx, cy, cz).scale(sx, sy, sz));
        staticWalls.add(AABB.fromCenterSize(new Vector3f(cx, cy, cz), new Vector3f(sx, sy, sz)));
    }

    /** Decorative cell bars across the cell front (z=10), with a doorway gap. */
    private InstancedMesh buildBars() {
        List<Matrix4f> inst = new ArrayList<>();
        float z = 10f;
        for (float x = -7.4f; x <= 7.4f; x += 0.55f) {
            if (Math.abs(x) < 1.7f) continue;
            inst.add(new Matrix4f().translate(x, 1.75f, z).scale(0.06f, 3.4f, 0.06f));
        }
        for (float y : new float[]{0.35f, 3.3f}) {
            inst.add(new Matrix4f().translate(-4.6f, y, z).scale(5.8f, 0.07f, 0.07f));
            inst.add(new Matrix4f().translate(4.6f, y, z).scale(5.8f, 0.07f, 0.07f));
        }
        int[] idx = new int[36];
        for (int i = 0; i < 36; i++) idx[i] = i;
        return new InstancedMesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2}, idx, inst.toArray(new Matrix4f[0]));
    }

    private void rebuildColliders() {
        List<AABB> a = new ArrayList<>(staticWalls);
        for (Door d : doors) {
            if (!d.open) a.add(d.aabb());
        }
        activeWalls = a.toArray(new AABB[0]);
    }

    private void startGame() {
        for (Door d : doors) d.open = false;
        for (Keycard k : keycards) k.collected = false;
        for (Guard g : guards) g.reset();
        inventory.clear();
        rebuildColliders();
        player.setStart(START_X, START_Z, START_YAW);
        detection = 0f;
        caughtFlash = 0f;
        time = 0f;
        state = State.PLAYING;
        input.setMouseCaptured(true);
    }

    private void caught() {
        player.setStart(START_X, START_Z, START_YAW);
        for (Guard g : guards) g.reset();
        detection = 0f;
        caughtFlash = 1.6f;
        sCaught.play();
    }

    @Override
    public void update(float deltaSeconds) {
        spin += deltaSeconds * 2f;
        if (promptTimer > 0f) promptTimer -= deltaSeconds;
        if (caughtFlash > 0f) caughtFlash -= deltaSeconds;

        if (state == State.TITLE) {
            if (input.isKeyPressed(GLFW_KEY_ENTER)) startGame();
            return;
        }
        if (state == State.WIN) {
            if (input.isKeyPressed(GLFW_KEY_R)) startGame();
            return;
        }

        time += deltaSeconds;
        player.addLook(input.mouseDeltaX(), input.mouseDeltaY());
        float forward = (actions.isDown("forward", input) ? 1f : 0f) - (actions.isDown("back", input) ? 1f : 0f);
        float strafe = (actions.isDown("right", input) ? 1f : 0f) - (actions.isDown("left", input) ? 1f : 0f);
        boolean sneak = actions.isDown("sneak", input);
        player.update(deltaSeconds, forward, strafe, sneak, activeWalls);

        if (forward != 0f || strafe != 0f) {
            stepTimer -= deltaSeconds;
            if (stepTimer <= 0f) {
                sStep.play();
                stepTimer = sneak ? 0.6f : 0.4f;
            }
        }

        if (input.isKeyPressed(GLFW_KEY_E)) {
            boolean handled = false;
            for (Keycard k : keycards) {
                if (k.inReach(player.position, 2.2f)) {
                    k.collected = true;
                    inventory.add(k.id);
                    sPickup.play();
                    handled = true;
                    break;
                }
            }
            if (!handled) {
                for (Door d : doors) {
                    if (!d.open && d.inReach(player.position, 2.8f)) {
                        if (d.tryOpen(inventory)) {
                            sUnlock.play();
                            rebuildColliders();
                        } else {
                            lockedMsg = "LOCKED - need keycard " + d.requiredKey;
                            promptTimer = 1.6f;
                        }
                        break;
                    }
                }
            }
        }

        boolean seen = false;
        for (Guard g : guards) {
            g.update(deltaSeconds);
            if (g.canSee(player.position, activeWalls)) seen = true;
        }
        detection += (seen ? deltaSeconds / 1.1f : -deltaSeconds / 0.7f);
        detection = Math.max(0f, Math.min(1f, detection));

        if (detection >= 1f) {
            caught();
        } else if (exit.contains(player.position)) {
            state = State.WIN;
            sEscape.play();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective((float) Math.toRadians(70.0), (float) width / height, 0.1f, 300f);
    }

    @Override
    public void render() {
        glClearColor(0.05f, 0.06f, 0.08f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Matrix4f view = player.viewMatrix();

        // --- Walls + floor: concrete color + normal maps (world-projected) ---
        litTriShader.bind();
        litTriShader.setUniform("uProjection", projection);
        litTriShader.setUniform("uView", view);
        litTriShader.setUniform("uViewPos", player.position);
        litTriShader.setUniform("uTint", tmp.set(1f, 1f, 1f));
        litTriShader.setUniform("uAmbientStrength", 0.3f);
        litTriShader.setUniform("uSpecularStrength", 0.06f);
        litTriShader.setUniform("uShininess", 8f);
        litTriShader.setUniform("uLightCount", lights.length);
        for (int i = 0; i < lights.length; i++) lights[i].apply(litTriShader, "uLights[" + i + "]");

        floorTex.bind(0);
        litTriShader.setUniform("uTexture", 0);
        floorN.bind(1);
        litTriShader.setUniform("uNormalMap", 1);
        litTriShader.setUniform("uTexScale", 0.3f);
        litTriShader.setUniform("uModel", floorModel);
        floorMesh.render();

        wallTex.bind(0);
        wallN.bind(1);
        litTriShader.setUniform("uTexScale", 0.4f);
        for (Matrix4f m : wallModels) {
            litTriShader.setUniform("uModel", m);
            cubeMesh.render();
        }

        // --- Colored props: doors, keycards, guards, ceiling fixtures ---
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", player.position);
        litShader.setUniform("uFogDensity", 0f);
        litShader.setUniform("uLightCount", lights.length);
        for (int i = 0; i < lights.length; i++) lights[i].apply(litShader, "uLights[" + i + "]");

        doorMat.use();
        for (Door d : doors) {
            if (!d.open) {
                litShader.setUniform("uModel", d.model(model));
                cubeMesh.render();
            }
        }
        cardMat.use();
        for (Keycard k : keycards) {
            if (!k.collected) {
                litShader.setUniform("uTint", k.color);
                litShader.setUniform("uModel", k.model(model, spin));
                cubeMesh.render();
            }
        }
        for (Guard g : guards) {
            guardMat.use();
            litShader.setUniform("uModel", model.identity().translate(g.position.x, 0.95f, g.position.z).scale(0.7f, 1.9f, 0.7f));
            cubeMesh.render();
            float fs = (float) Math.sin(g.facing());
            float fc = (float) Math.cos(g.facing());
            visorMat.use();
            litShader.setUniform("uModel", model.identity()
                    .translate(g.position.x + fs * 0.42f, 1.55f, g.position.z + fc * 0.42f)
                    .rotateY(g.facing()).scale(0.55f, 0.22f, 0.2f));
            cubeMesh.render();
        }
        fixtureMat.use();
        for (Vector3f f : fixtures) {
            litShader.setUniform("uModel", model.identity().translate(f).scale(2f, 0.15f, 2f));
            cubeMesh.render();
        }

        // --- Cell bars (instanced) ---
        barShader.bind();
        barShader.setUniform("uProjection", projection);
        barShader.setUniform("uView", view);
        barShader.setUniform("uLightDir", barLight);
        barShader.setUniform("uColor", tmp.set(0.5f, 0.52f, 0.56f));
        bars.render();

        skybox.render(view, projection);

        // --- Vision cones ---
        flatShader.bind();
        flatShader.setUniform("uProjection", projection);
        flatShader.setUniform("uView", view);
        flatShader.setUniform("uAlpha", 0.2f);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        for (Guard g : guards) {
            flatShader.setUniform("uModel", model.identity().translate(g.position.x, 0f, g.position.z).rotateY(g.facing()));
            flatShader.setUniform("uColor", g.alerted() ? tmp.set(1f, 0.25f, 0.2f) : tmp.set(0.3f, 0.9f, 0.45f));
            coneMesh.render();
        }
        glDepthMask(true);
        glDisable(GL_BLEND);

        renderHud();
    }

    private void renderHud() {
        int fbw = window.framebufferWidth();
        int fbh = window.framebufferHeight();
        float cx = fbw / 2f, cyy = fbh / 2f;
        hud.begin(fbw, fbh);

        if (state == State.TITLE) {
            hud.text(cx - 170f, cyy - 60f, 6f, "PRISON BREAK", 1f, 1f, 1f);
            hud.text(cx - 160f, cyy + 10f, 2.5f, "Sneak past the guards. Grab both keycards. Reach the gate.", 0.9f, 0.9f, 0.9f);
            hud.text(cx - 90f, cyy + 44f, 3f, "press ENTER", 0.4f, 1f, 0.5f);
            hud.end();
            return;
        }

        hud.text(12, 12, 2f, objective(), 1f, 1f, 1f);
        hud.text(12, 38, 2f, "keys: " + (inventory.contains("A") ? "[A]" : "[ ]") + " "
                + (inventory.contains("B") ? "[B]" : "[ ]") + "     time " + String.format("%.0f", time) + "s", 0.7f, 0.85f, 1f);
        int n = Math.round(detection * 12f);
        StringBuilder bar = new StringBuilder("ALERT [");
        for (int i = 0; i < 12; i++) bar.append(i < n ? '#' : '.');
        bar.append(']');
        hud.text(12, 64, 2f, bar.toString(), 0.4f + detection * 0.6f, 1f - detection * 0.7f, 0.3f);

        hud.text(cx - 5f, cyy - 8f, 2f, "+", 1f, 1f, 1f);
        String prompt = nearPrompt();
        if (prompt != null) hud.text(cx - 90f, cyy + 24f, 2f, prompt, 1f, 1f, 0.6f);
        if (promptTimer > 0f) hud.text(cx - 120f, cyy + 50f, 2.5f, lockedMsg, 1f, 0.4f, 0.3f);

        if (state == State.WIN) {
            hud.text(cx - 240f, cyy - 20f, 4f, "YOU ESCAPED!  " + String.format("%.1f", time) + "s  - press R", 0.4f, 1f, 0.5f);
        } else if (caughtFlash > 0f) {
            hud.text(cx - 120f, cyy - 60f, 5f, "CAUGHT!", 1f, 0.3f, 0.2f);
        }
        hud.end();
    }

    private String objective() {
        if (!inventory.contains("A")) return "Objective: find KEYCARD A (guard room)";
        if (!inventory.contains("B")) return "Objective: find KEYCARD B (the yard)";
        return "Objective: open the GATE and escape";
    }

    private String nearPrompt() {
        for (Keycard k : keycards) {
            if (k.inReach(player.position, 2.2f)) return "E: pick up keycard " + k.id;
        }
        for (Door d : doors) {
            if (!d.open && d.inReach(player.position, 2.8f)) {
                return inventory.contains(d.requiredKey) ? "E: open door" : "E: locked (needs " + d.requiredKey + ")";
            }
        }
        return null;
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
        bars.dispose();
        skybox.dispose();
        flatShader.dispose();
        barShader.dispose();
        hud.dispose();
        sPickup.dispose();
        sUnlock.dispose();
        sCaught.dispose();
        sEscape.dispose();
        sStep.dispose();
        audio.destroy();
        resources.dispose();
    }
}
