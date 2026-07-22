package scenes;

import engine.Camera;
import engine.Input;
import engine.Scene;
import engine.ShaderProgram;
import engine.Terrain;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * A procedurally generated mountain landscape you can fly through.
 * Elevation-colored (grass / rock / snow), lit by a sun, with distance fog.
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class TerrainScene implements Scene {

    private ShaderProgram shader;
    private Terrain terrain;
    private Camera camera;
    private Input input;

    private final Matrix4f model = new Matrix4f();          // terrain is already in world space
    private final Matrix4f projection = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private final Vector3f sky = new Vector3f(0.62f, 0.74f, 0.88f);

    @Override
    public void init(Window window) {
        shader = ShaderProgram.fromFiles("shaders/terrain.vert", "shaders/terrain.frag");
        // 256x256 grid, 400 units wide, peaks up to 70 units, fixed seed.
        terrain = new Terrain(256, 400f, 70f, 1337L);

        camera = new Camera().setPosition(0f, 90f, 180f).setMoveSpeed(45f);
        input = window.input();
        input.setMouseCaptured(true);

        // Far plane must reach across the terrain.
        projection.identity().perspective(
                (float) Math.toRadians(60.0), window.aspectRatio(), 0.5f, 1200f);
    }

    @Override
    public void update(float deltaSeconds) {
        camera.processInput(input, deltaSeconds);
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;                 // ignore minimized/degenerate
        projection.identity().perspective(
                (float) Math.toRadians(60.0), (float) width / height, 0.5f, 1200f);
    }

    @Override
    public void render() {
        // Override the engine's clear color with a sky color (fog fades to this).
        glClearColor(sky.x, sky.y, sky.z, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        shader.bind();
        shader.setUniform("uProjection", projection);
        shader.setUniform("uView", camera.viewMatrix());
        shader.setUniform("uModel", model);
        shader.setUniform("uViewPos", camera.position());
        shader.setUniform("uLightDir", lightDir);
        shader.setUniform("uMaxHeight", terrain.maxHeight);
        shader.setUniform("uFogColor", sky);
        terrain.mesh().render();
    }

    @Override
    public void dispose() {
        terrain.dispose();
        shader.dispose();
    }
}
