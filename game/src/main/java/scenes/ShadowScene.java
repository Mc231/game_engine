package scenes;

import engine.Geometry;

import engine.Camera;
import engine.Input;
import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.ShadowMap;
import engine.Texture;
import engine.Transform;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Directional shadow mapping in two passes:
 *   1) render the scene's depth from the light's point of view into a ShadowMap.
 *   2) render normally; for each pixel, compare its depth-in-light-space against
 *      the shadow map to decide if something is between it and the light.
 *
 * A slowly sweeping sun casts moving shadows of floating cubes onto a ground plane.
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse   Quit: Esc
 */
public class ShadowScene implements Scene {

    private static final int SHADOW_SIZE = 2048;

    private final Vector3f ORIGIN = new Vector3f(0f, 0f, 0f);
    private final Vector3f UP = new Vector3f(0f, 1f, 0f);

    private ShaderProgram depthShader;
    private ShaderProgram litShader;
    private ShadowMap shadowMap;
    private Mesh cubeMesh;
    private Mesh planeMesh;
    private Texture texture;
    private Camera camera;
    private Input input;
    private Window window;

    private final List<Transform> cubes = new ArrayList<>();
    private final Transform ground = new Transform();

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f lightView = new Matrix4f();
    private final Matrix4f lightProj = new Matrix4f();
    private final Matrix4f lightSpace = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.5f, -1f, -0.4f);
    private final Vector3f lightPos = new Vector3f();
    private final Vector3f lightColor = new Vector3f(1f, 0.97f, 0.9f);
    private float elapsed;

    @Override
    public void init(Window window) {
        this.window = window;
        depthShader = ShaderProgram.fromFiles("shaders/shadow_depth.vert", "shaders/shadow_depth.frag");
        litShader = ShaderProgram.fromFiles("shaders/shadow_lit.vert", "shaders/shadow_lit.frag");
        shadowMap = new ShadowMap(SHADOW_SIZE, SHADOW_SIZE);

        cubeMesh = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        planeMesh = new Mesh(Geometry.plane(14f, 14f), new int[]{3, 3, 2});
        texture = new Texture("textures/crate.png");

        ground.setPosition(0f, 0f, 0f);

        float[][] placements = {
                {0f, 1.5f, 0f},
                {-3f, 1.0f, -2f},
                {3f, 2.0f, 1f},
                {1.5f, 2.8f, -3f},
                {-2f, 3.2f, 2f},
        };
        for (float[] p : placements) {
            Transform t = new Transform().setPosition(p[0], p[1], p[2]);
            cubes.add(t);
        }

        camera = new Camera().setPosition(0f, 6f, 12f);
        input = window.input();
        input.setMouseCaptured(true);

        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
        camera.processInput(input, deltaSeconds);

        // Sweep the sun slowly so the shadows move.
        float ang = elapsed * 0.25f;
        lightDir.set((float) Math.sin(ang) * 0.5f, -1f, (float) Math.cos(ang) * 0.5f).normalize();

        for (int i = 0; i < cubes.size(); i++) {
            cubes.get(i).setRotationEuler(elapsed * 0.2f, elapsed * 0.3f + i, 0f);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;                 // ignore minimized/degenerate
        projection.identity().perspective(
                (float) Math.toRadians(45.0), (float) width / height, 0.1f, 100f);
    }

    @Override
    public void render() {
        // Light-space matrix: an orthographic "camera" looking along the sun direction.
        lightPos.set(lightDir).mul(-15f);                       // 15 units opposite the light
        lightView.identity().lookAt(lightPos, ORIGIN, UP);
        lightProj.identity().ortho(-14f, 14f, -14f, 14f, 1f, 40f);
        lightProj.mul(lightView, lightSpace);

        // --- Pass 1: depth from the light's viewpoint ---
        shadowMap.bindForWriting();
        depthShader.bind();
        depthShader.setUniform("uLightSpace", lightSpace);
        renderGeometry(depthShader);

        // --- Back to the screen ---
        ShadowMap.unbind(window.framebufferWidth(), window.framebufferHeight());

        // --- Pass 2: lit + shadowed ---
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", camera.viewMatrix());
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uLightDir", lightDir);
        litShader.setUniform("uLightColor", lightColor);
        litShader.setUniform("uLightSpace", lightSpace);
        texture.bind(0);
        litShader.setUniform("uTexture", 0);
        shadowMap.bindTexture(1);
        litShader.setUniform("uShadowMap", 1);
        renderGeometry(litShader);
    }

    /** Draw the ground + all cubes, setting uModel per object on the given shader. */
    private void renderGeometry(ShaderProgram shader) {
        shader.setUniform("uModel", ground.matrix());
        planeMesh.render();
        for (Transform t : cubes) {
            shader.setUniform("uModel", t.matrix());
            cubeMesh.render();
        }
    }

    @Override
    public void dispose() {
        cubeMesh.dispose();
        planeMesh.dispose();
        texture.dispose();
        shadowMap.dispose();
        depthShader.dispose();
        litShader.dispose();
    }
}
