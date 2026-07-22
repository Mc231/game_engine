package scenes;

import engine.CarController;
import engine.CubemapTexture;
import engine.Gamepad;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.InstancedMesh;
import engine.Material;
import engine.Mesh;
import engine.Model;
import engine.MtlLoader;
import engine.OBJLoader;
import engine.ResourceManager;
import engine.Road;
import engine.Scatter;
import engine.Scene;
import engine.ShaderProgram;
import engine.Skybox;
import engine.Terrain;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_RIGHT_Y;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_BUTTON_B;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UP;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * A driving game world: steer a (CC0 Kenney) car around a curvy spline road on
 * rolling terrain, under a cubemap sky, past an instanced forest. Third-person
 * mouse-orbit chase camera.
 *
 *   Accelerate: W / Up   Reverse: S / Down   Steer: A/D or Left/Right
 *   Brake: Space   Camera: mouse   Switch scene: [ ]   Quit: Esc
 */
public class DrivingScene implements Scene {

    private static final float MODEL_YAW_OFFSET = 0f;   // Kenney car faces +Z (our forward)
    private static final float CAM_DISTANCE = 10f;
    private static final float RX = 150f;               // track ellipse radii
    private static final float RZ = 110f;
    private static final float ROAD_WIDTH = 10f;

    // Instanced forest shader (baked vertex colors + one directional light).
    private static final String TREE_VERTEX = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            layout (location = 2) in vec3 aColor;
            layout (location = 3) in mat4 aInstance;
            out vec3 vColor;
            out vec3 vNormal;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() {
                vNormal = mat3(aInstance) * aNormal;
                vColor = aColor;
                gl_Position = uProjection * uView * aInstance * vec4(aPos, 1.0);
            }
            """;
    private static final String TREE_FRAGMENT = """
            #version 330 core
            in vec3 vColor;
            in vec3 vNormal;
            out vec4 FragColor;
            uniform vec3 uLightDir;
            void main() {
                vec3 N = normalize(vNormal);
                float diff = max(dot(N, normalize(-uLightDir)), 0.0);
                FragColor = vec4(vColor * (0.45 + 0.65 * diff), 1.0);
            }
            """;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram terrainShader;
    private ShaderProgram litShader;
    private ShaderProgram treeShader;
    private Terrain terrain;
    private Mesh roadMesh;
    private Material roadMaterial;
    private Model car;
    private Skybox skybox;
    private InstancedMesh forest;
    private CarController controller;
    private Input input;
    private InputMap actions;
    private Gamepad gamepad;
    private Hud hud;
    private Window window;

    private static final Matrix4f IDENTITY = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f carMatrix = new Matrix4f();
    private final Vector3f camPos = new Vector3f(RX, 8f, -10f);
    private final Vector3f camTarget = new Vector3f();
    private final Vector3f desiredCam = new Vector3f();
    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private final Vector3f sky = new Vector3f(0.62f, 0.74f, 0.88f);
    private float camYaw = 0f;
    private float camPitch = 0.42f;

    @Override
    public void init(Window window) {
        this.window = window;
        terrainShader = ShaderProgram.fromFiles("shaders/terrain.vert", "shaders/terrain.frag");
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        treeShader = new ShaderProgram(TREE_VERTEX, TREE_FRAGMENT);

        terrain = new Terrain(320, 700f, 12f, 2025L);   // large, gentle green hills

        // Curvy closed track: waypoints on a wobbly ellipse.
        int n = 12;
        Vector3f[] waypoints = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            float r = 1f + 0.18f * (float) Math.sin(i * 1.7);
            waypoints[i] = new Vector3f((float) Math.cos(a) * RX * r, 0f, (float) Math.sin(a) * RZ * r);
        }
        roadMesh = Road.spline(waypoints, ROAD_WIDTH, 24, terrain::heightAt, 0.15f);
        roadMaterial = new Material(litShader, resources.texture("textures/road.png"))
                .setAmbientStrength(0.4f).setSpecularStrength(0.08f).setShininess(6f);

        car = Model.load("models/car/car.obj", litShader, resources);

        // Start the car on the track at waypoint 0, heading toward waypoint 1.
        Vector3f w0 = waypoints[0];
        Vector3f dir = new Vector3f(waypoints[1]).sub(w0);
        controller = new CarController()
                .setPosition(w0.x, 0f, w0.z)
                .setHeading((float) Math.atan2(dir.x, dir.z));

        // Sky.
        skybox = new Skybox(new CubemapTexture(
                "skybox/right.png", "skybox/left.png", "skybox/top.png",
                "skybox/bottom.png", "skybox/front.png", "skybox/back.png"));

        // Instanced forest — off the track band, on the terrain.
        Matrix4f[] trees = Scatter.onArea(500, 300f, 99L, 2.0f, 3.8f, terrain::heightAt,
                (x, z) -> {
                    float e = (float) Math.sqrt((x / RX) * (x / RX) + (z / RZ) * (z / RZ));
                    return Math.abs(e - 1f) < 0.18f;   // keep trees off the road ring
                });
        forest = buildForest("models/tree/tree.obj", trees);

        input = window.input();
        input.setMouseCaptured(true);
        actions = new InputMap()
                .bind("accel", GLFW_KEY_W, GLFW_KEY_UP)
                .bind("reverse", GLFW_KEY_S, GLFW_KEY_DOWN)
                .bind("left", GLFW_KEY_A, GLFW_KEY_LEFT)
                .bind("right", GLFW_KEY_D, GLFW_KEY_RIGHT)
                .bind("brake", GLFW_KEY_SPACE);
        gamepad = new Gamepad();
        hud = new Hud();

        projection.identity().perspective(
                (float) Math.toRadians(60.0), window.aspectRatio(), 0.3f, 1200f);
    }

    @Override
    public void update(float deltaSeconds) {
        gamepad.update();

        float throttle = (actions.isDown("accel", input) ? 1f : 0f)
                - (actions.isDown("reverse", input) ? 1f : 0f)
                - deadzone(gamepad.axis(GLFW_GAMEPAD_AXIS_LEFT_Y));
        float steer = (actions.isDown("left", input) ? 1f : 0f)
                - (actions.isDown("right", input) ? 1f : 0f)
                - deadzone(gamepad.axis(GLFW_GAMEPAD_AXIS_LEFT_X));
        boolean brake = actions.isDown("brake", input) || gamepad.button(GLFW_GAMEPAD_BUTTON_B);

        controller.update(deltaSeconds, clamp(throttle), clamp(steer), brake, terrain::heightAt);

        camYaw += input.mouseDeltaX() * 0.005f
                + deadzone(gamepad.axis(GLFW_GAMEPAD_AXIS_RIGHT_X)) * deltaSeconds * 2.5f;
        camPitch += input.mouseDeltaY() * 0.005f
                + deadzone(gamepad.axis(GLFW_GAMEPAD_AXIS_RIGHT_Y)) * deltaSeconds * 2.5f;
        camPitch = Math.max(0.08f, Math.min(1.35f, camPitch));

        Vector3f carPos = controller.position();
        float a = controller.heading() + (float) Math.PI + camYaw;
        float cp = (float) Math.cos(camPitch);
        float sp = (float) Math.sin(camPitch);
        desiredCam.set(carPos).add(
                (float) Math.sin(a) * cp * CAM_DISTANCE, sp * CAM_DISTANCE, (float) Math.cos(a) * cp * CAM_DISTANCE);
        camPos.lerp(desiredCam, Math.min(deltaSeconds * 6f, 1f));
        camTarget.set(carPos).add(0f, 1.2f, 0f);
        view.identity().lookAt(camPos, camTarget, up);

        carMatrix.identity().translate(carPos).rotateY(controller.heading() + MODEL_YAW_OFFSET);
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective(
                (float) Math.toRadians(60.0), (float) width / height, 0.3f, 1200f);
    }

    @Override
    public void render() {
        glClearColor(sky.x, sky.y, sky.z, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Terrain.
        terrainShader.bind();
        terrainShader.setUniform("uProjection", projection);
        terrainShader.setUniform("uView", view);
        terrainShader.setUniform("uModel", IDENTITY);
        terrainShader.setUniform("uViewPos", camPos);
        terrainShader.setUniform("uLightDir", lightDir);
        terrainShader.setUniform("uMaxHeight", terrain.maxHeight);
        terrainShader.setUniform("uFogColor", sky);
        terrain.mesh().render();

        // Road + car (lit shader, one directional light).
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camPos);
        litShader.setUniform("uLightCount", 1);
        engine.Light.directional(lightDir, new Vector3f(1f, 1f, 1f)).apply(litShader, "uLights[0]");
        roadMaterial.use();
        litShader.setUniform("uModel", IDENTITY);
        roadMesh.render();
        litShader.setUniform("uModel", carMatrix);
        car.render();

        // Forest (instanced).
        treeShader.bind();
        treeShader.setUniform("uProjection", projection);
        treeShader.setUniform("uView", view);
        treeShader.setUniform("uLightDir", lightDir);
        forest.render();

        // Sky fills the background.
        skybox.render(view, projection);

        // HUD.
        float kmh = Math.abs(controller.speed()) * 3.6f;
        hud.begin(window.framebufferWidth(), window.framebufferHeight());
        hud.text(12, 12, 2f, "DRIVE  -  W/S drive, A/D steer, Space brake, mouse camera, [ ] scene", 1f, 1f, 1f);
        hud.text(12, 40, 3f, String.format("%3.0f km/h", kmh), 1f, 0.95f, 0.4f);
        hud.end();
    }

    /** Load a small OBJ, bake each part's material Kd into per-vertex colors, and instance it. */
    private InstancedMesh buildForest(String objPath, Matrix4f[] instances) {
        String dir = objPath.substring(0, objPath.lastIndexOf('/') + 1);
        OBJLoader.ModelData md = OBJLoader.parseModel(readResource(objPath));
        Map<String, MtlLoader.MaterialDef> mtl = md.mtlLib() != null
                ? MtlLoader.parse(readResource(dir + md.mtlLib()))
                : Map.of();

        List<Float> verts = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int base = 0;
        for (OBJLoader.Part part : md.parts()) {
            MtlLoader.MaterialDef def = part.materialName() != null ? mtl.get(part.materialName()) : null;
            float cr = def != null ? def.diffuse().x : 0.6f;
            float cg = def != null ? def.diffuse().y : 0.6f;
            float cb = def != null ? def.diffuse().z : 0.6f;
            float[] pv = part.mesh().vertices();   // stride 8: pos(3) normal(3) uv(2)
            int vcount = pv.length / 8;
            for (int v = 0; v < vcount; v++) {
                int o = v * 8;
                verts.add(pv[o]); verts.add(pv[o + 1]); verts.add(pv[o + 2]);       // position
                verts.add(pv[o + 3]); verts.add(pv[o + 4]); verts.add(pv[o + 5]);   // normal
                verts.add(cr); verts.add(cg); verts.add(cb);                         // color (from Kd)
            }
            for (int idx : part.mesh().indices()) {
                indices.add(base + idx);
            }
            base += vcount;
        }
        float[] va = new float[verts.size()];
        for (int i = 0; i < va.length; i++) va[i] = verts.get(i);
        int[] ia = new int[indices.size()];
        for (int i = 0; i < ia.length; i++) ia[i] = indices.get(i);
        return new InstancedMesh(va, new int[]{3, 3, 3}, ia, instances);
    }

    private static String readResource(String path) {
        try (InputStream in = DrivingScene.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Resource not found: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static float clamp(float v) {
        return Math.max(-1f, Math.min(1f, v));
    }

    private static float deadzone(float v) {
        return Math.abs(v) < 0.2f ? 0f : v;
    }

    @Override
    public void dispose() {
        terrain.dispose();
        roadMesh.dispose();
        car.dispose();
        skybox.dispose();
        forest.dispose();
        terrainShader.dispose();
        treeShader.dispose();
        hud.dispose();
        resources.dispose();   // lit shader, road + car textures
    }
}
