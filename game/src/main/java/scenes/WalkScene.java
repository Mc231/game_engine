package scenes;

import engine.Audio;
import engine.CharacterController;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.Scene;
import engine.ShaderProgram;
import engine.Sound;
import engine.Terrain;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * "Game feel" demo: walk on the procedural terrain with a first-person
 * character controller (gravity + jump + ground clamping), action-mapped
 * controls, a text HUD, and a jump sound effect.
 *
 *   Move: W/A/S/D   Jump: Space   Look: mouse   Quit: Esc
 */
public class WalkScene implements Scene {

    private ShaderProgram terrainShader;
    private Terrain terrain;
    private CharacterController player;
    private Input input;
    private InputMap actions;
    private Hud hud;
    private Audio audio;
    private Sound jumpSound;
    private Window window;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f identity = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private final Vector3f sky = new Vector3f(0.62f, 0.74f, 0.88f);

    @Override
    public void init(Window window) {
        this.window = window;
        terrainShader = ShaderProgram.fromFiles("shaders/terrain.vert", "shaders/terrain.frag");
        terrain = new Terrain(256, 400f, 70f, 1337L);

        player = new CharacterController().setPosition(0f, 0f, 0f).setMoveSpeed(20f);

        actions = new InputMap()
                .bind("forward", GLFW_KEY_W)
                .bind("back", GLFW_KEY_S)
                .bind("left", GLFW_KEY_A)
                .bind("right", GLFW_KEY_D)
                .bind("jump", GLFW_KEY_SPACE);

        input = window.input();
        input.setMouseCaptured(true);

        hud = new Hud();
        audio = new Audio();
        jumpSound = new Sound("sounds/jump.wav");

        projection.identity().perspective(
                (float) Math.toRadians(60.0), window.aspectRatio(), 0.5f, 1200f);
    }

    @Override
    public void update(float deltaSeconds) {
        player.addLook(input.mouseDeltaX(), input.mouseDeltaY());

        float forward = (actions.isDown("forward", input) ? 1f : 0f)
                - (actions.isDown("back", input) ? 1f : 0f);
        float strafe = (actions.isDown("right", input) ? 1f : 0f)
                - (actions.isDown("left", input) ? 1f : 0f);
        boolean jump = actions.isPressed("jump", input);

        boolean wasGrounded = player.onGround();
        player.update(deltaSeconds, forward, strafe, jump, terrain::heightAt);

        if (jump && wasGrounded) {
            jumpSound.play();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective(
                (float) Math.toRadians(60.0), (float) width / height, 0.5f, 1200f);
    }

    @Override
    public void render() {
        glClearColor(sky.x, sky.y, sky.z, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        terrainShader.bind();
        terrainShader.setUniform("uProjection", projection);
        terrainShader.setUniform("uView", player.viewMatrix());
        terrainShader.setUniform("uModel", identity);
        terrainShader.setUniform("uViewPos", player.position());
        terrainShader.setUniform("uLightDir", lightDir);
        terrainShader.setUniform("uMaxHeight", terrain.maxHeight);
        terrainShader.setUniform("uFogColor", sky);
        terrain.mesh().render();

        // HUD overlay.
        Vector3f p = player.position();
        hud.begin(window.framebufferWidth(), window.framebufferHeight());
        hud.text(12, 12, 2f, "WALK  -  WASD move, SPACE jump, mouse look, [ ] switch scene", 1f, 1f, 1f);
        hud.text(12, 40, 2f,
                String.format("pos %.1f %.1f %.1f   %s", p.x, p.y, p.z,
                        player.onGround() ? "grounded" : "airborne"),
                1f, 0.95f, 0.5f);
        hud.end();
    }

    @Override
    public void dispose() {
        jumpSound.dispose();
        audio.destroy();
        hud.dispose();
        terrain.dispose();
        terrainShader.dispose();
    }
}
