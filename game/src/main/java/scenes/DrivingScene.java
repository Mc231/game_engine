package scenes;

import engine.CarController;
import engine.Gamepad;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.Material;
import engine.Model;
import engine.Mesh;
import engine.ResourceManager;
import engine.Road;
import engine.Scene;
import engine.ShaderProgram;
import engine.Terrain;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_X;
import static org.lwjgl.glfw.GLFW.GLFW_GAMEPAD_AXIS_LEFT_Y;
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
 * A driving game: steer a (CC0 Kenney) car around a road loop laid on rolling
 * procedural terrain, with a third-person chase camera.
 *
 *   Accelerate: W / Up     Reverse: S / Down     Steer: A/D or Left/Right
 *   Brake: Space           Switch scene: [ ]     Quit: Esc
 *   (Gamepad: left stick to drive, B to brake.)
 */
public class DrivingScene implements Scene {

    /** If the car appears to drive backwards, flip this by Math.PI. */
    private static final float MODEL_YAW_OFFSET = (float) Math.PI;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram terrainShader;
    private ShaderProgram litShader;
    private Terrain terrain;
    private Mesh roadMesh;
    private Material roadMaterial;
    private Model car;
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
    private final Vector3f camPos = new Vector3f(140f, 8f, -10f);
    private final Vector3f camTarget = new Vector3f();
    private final Vector3f desiredCam = new Vector3f();
    private final Vector3f up = new Vector3f(0f, 1f, 0f);
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private final Vector3f sky = new Vector3f(0.62f, 0.74f, 0.88f);

    @Override
    public void init(Window window) {
        this.window = window;
        terrainShader = ShaderProgram.fromFiles("shaders/terrain.vert", "shaders/terrain.frag");
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");

        terrain = new Terrain(320, 600f, 16f, 2025L);   // large, gentle rolling hills
        roadMesh = Road.loop(140f, 100f, 9f, 260, terrain::heightAt, 0.12f);
        roadMaterial = new Material(litShader, resources.texture("textures/road.png"))
                .setAmbientStrength(0.35f).setSpecularStrength(0.1f).setShininess(8f);

        car = Model.load("models/car/car.obj", litShader, resources);
        controller = new CarController().setPosition(140f, 0f, 0f).setHeading(0f);

        input = window.input();
        actions = new InputMap()
                .bind("accel", GLFW_KEY_W, GLFW_KEY_UP)
                .bind("reverse", GLFW_KEY_S, GLFW_KEY_DOWN)
                .bind("left", GLFW_KEY_A, GLFW_KEY_LEFT)
                .bind("right", GLFW_KEY_D, GLFW_KEY_RIGHT)
                .bind("brake", GLFW_KEY_SPACE);
        gamepad = new Gamepad();
        hud = new Hud();

        projection.identity().perspective(
                (float) Math.toRadians(60.0), window.aspectRatio(), 0.3f, 1000f);
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

        // Third-person chase camera: sit behind & above the car, smoothly.
        Vector3f carPos = controller.position();
        Vector3f fwd = controller.forward();
        desiredCam.set(carPos).sub(fwd.x * 9f, 0f, fwd.z * 9f).add(0f, 4.5f, 0f);
        camPos.lerp(desiredCam, Math.min(deltaSeconds * 4f, 1f));
        camTarget.set(carPos).add(0f, 1.2f, 0f);
        view.identity().lookAt(camPos, camTarget, up);

        carMatrix.identity().translate(carPos).rotateY(controller.heading() + MODEL_YAW_OFFSET);
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective(
                (float) Math.toRadians(60.0), (float) width / height, 0.3f, 1000f);
    }

    @Override
    public void render() {
        glClearColor(sky.x, sky.y, sky.z, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // --- Terrain ---
        terrainShader.bind();
        terrainShader.setUniform("uProjection", projection);
        terrainShader.setUniform("uView", view);
        terrainShader.setUniform("uModel", IDENTITY);
        terrainShader.setUniform("uViewPos", camPos);
        terrainShader.setUniform("uLightDir", lightDir);
        terrainShader.setUniform("uMaxHeight", terrain.maxHeight);
        terrainShader.setUniform("uFogColor", sky);
        terrain.mesh().render();

        // --- Road + car share the lit shader (one directional light) ---
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
        car.render();     // parts bind the car's colormap material

        // --- HUD ---
        float kmh = Math.abs(controller.speed()) * 3.6f;
        hud.begin(window.framebufferWidth(), window.framebufferHeight());
        hud.text(12, 12, 2f, "DRIVE  -  W/S or arrows, A/D steer, Space brake, [ ] scene", 1f, 1f, 1f);
        hud.text(12, 40, 3f, String.format("%3.0f km/h", kmh), 1f, 0.95f, 0.4f);
        hud.end();
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
        terrainShader.dispose();
        hud.dispose();
        resources.dispose();   // lit shader, road + car textures
    }
}
