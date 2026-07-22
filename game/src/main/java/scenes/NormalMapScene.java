package scenes;

import engine.Camera;
import engine.Geometry;
import engine.Hud;
import engine.Input;
import engine.Mesh;
import engine.Scene;
import engine.ShaderProgram;
import engine.ShaderReloader;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;

/**
 * Normal mapping + transparency + shader hot-reload.
 *   - Opaque cubes use tangent-space normal mapping ({@link Geometry#cubeWithTangents}).
 *   - Their shader is loaded via {@link ShaderReloader}: edit
 *     game/src/main/resources/shaders/normalmap.frag and it reloads live (or press R).
 *   - A few translucent panes are drawn afterwards with alpha blending, sorted
 *     back-to-front (transparency & blend ordering).
 *
 *   Move: W/A/S/D   Look: mouse   Reload shader: R   Switch scene: [ ]   Quit: Esc
 */
public class NormalMapScene implements Scene {

    private static final String TRANS_VERTEX = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform mat4 uModel;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() { gl_Position = uProjection * uView * uModel * vec4(aPos, 1.0); }
            """;

    private static final String TRANS_FRAGMENT = """
            #version 330 core
            out vec4 FragColor;
            uniform vec3 uColor;
            uniform float uAlpha;
            void main() { FragColor = vec4(uColor, uAlpha); }
            """;

    private static final Vector3f[] CUBE_POS = {
            new Vector3f(-2.2f, 0, 0), new Vector3f(0, 0, 0), new Vector3f(2.2f, 0, 0),
    };
    private static final Vector3f[] PANE_POS = {
            new Vector3f(-1.5f, 0, 3), new Vector3f(0.2f, 0, 4.5f), new Vector3f(1.8f, 0, 2.2f),
    };
    private static final Vector3f[] PANE_COLOR = {
            new Vector3f(1f, 0.3f, 0.3f), new Vector3f(0.3f, 1f, 0.4f), new Vector3f(0.4f, 0.5f, 1f),
    };

    private ShaderReloader normalShader;
    private ShaderProgram transShader;
    private Mesh normalCube;
    private Mesh paneCube;
    private Texture diffuse;
    private Texture normalMap;
    private Camera camera;
    private Input input;
    private Hud hud;
    private Window window;

    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
    private final Vector3f lightPos = new Vector3f();
    private float elapsed;

    @Override
    public void init(Window window) {
        this.window = window;
        // Load the normal-map shader from SOURCE files so edits reload live.
        // The working dir differs (game/ under Gradle, repo root elsewhere), so
        // resolve against both.
        normalShader = new ShaderReloader(
                resolveShaderPath("normalmap.vert"),
                resolveShaderPath("normalmap.frag"));
        transShader = new ShaderProgram(TRANS_VERTEX, TRANS_FRAGMENT);

        normalCube = new Mesh(Geometry.cubeWithTangents(), new int[]{3, 3, 2, 3});
        paneCube = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        diffuse = new Texture("textures/crate.png");
        normalMap = new Texture("textures/normal_bumps.png");

        camera = new Camera().setPosition(0f, 1f, 8f);
        input = window.input();
        input.setMouseCaptured(true);
        hud = new Hud();

        projection.identity().perspective(
                (float) Math.toRadians(45.0), window.aspectRatio(), 0.1f, 100f);
    }

    @Override
    public void update(float deltaSeconds) {
        elapsed += deltaSeconds;
        camera.processInput(input, deltaSeconds);

        normalShader.reloadIfChanged();               // live reload on file change
        if (input.isKeyPressed(GLFW_KEY_R)) {
            normalShader.reload();                     // or force it with R
        }

        float r = 4f;
        lightPos.set((float) Math.cos(elapsed) * r, 2f, (float) Math.sin(elapsed) * r + 3f);
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

        // --- Opaque, normal-mapped cubes ---
        ShaderProgram s = normalShader.get();
        s.bind();
        s.setUniform("uProjection", projection);
        s.setUniform("uView", view);
        s.setUniform("uViewPos", camera.position());
        s.setUniform("uLightPos", lightPos);
        s.setUniform("uLightColor", new Vector3f(1f, 1f, 1f));
        diffuse.bind(0);
        s.setUniform("uTexture", 0);
        normalMap.bind(1);
        s.setUniform("uNormalMap", 1);
        for (Vector3f pos : CUBE_POS) {
            model.identity().translate(pos).rotateY(elapsed * 0.4f);
            s.setUniform("uModel", model);
            normalCube.render();
        }

        // --- Transparent panes: blend, back-to-front, no depth writes ---
        Integer[] order = {0, 1, 2};
        Vector3f eye = camera.position();
        Arrays.sort(order, (a, b) -> Float.compare(
                PANE_POS[b].distanceSquared(eye), PANE_POS[a].distanceSquared(eye)));
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        transShader.bind();
        transShader.setUniform("uProjection", projection);
        transShader.setUniform("uView", view);
        transShader.setUniform("uAlpha", 0.45f);
        for (int idx : order) {
            model.identity().translate(PANE_POS[idx]).scale(1.4f);
            transShader.setUniform("uModel", model);
            transShader.setUniform("uColor", PANE_COLOR[idx]);
            paneCube.render();
        }
        glDepthMask(true);
        glDisable(GL_BLEND);

        hud.begin(window.framebufferWidth(), window.framebufferHeight());
        hud.text(12, 12, 2f, "NORMAL MAPPING + TRANSPARENCY", 1f, 1f, 1f);
        hud.text(12, 40, 2f, "R = reload shader from source,   [ ] = scene", 1f, 0.95f, 0.5f);
        hud.end();
    }

    /** Find the shader source file under either the game module dir or the repo root. */
    private static String resolveShaderPath(String name) {
        for (String base : new String[]{"", "game/"}) {
            java.nio.file.Path p = java.nio.file.Paths.get(base + "src/main/resources/shaders/" + name);
            if (java.nio.file.Files.exists(p)) {
                return p.toString();
            }
        }
        return "src/main/resources/shaders/" + name;
    }

    @Override
    public void dispose() {
        if (normalShader != null) normalShader.dispose();
        if (transShader != null) transShader.dispose();
        if (normalCube != null) normalCube.dispose();
        if (paneCube != null) paneCube.dispose();
        if (diffuse != null) diffuse.dispose();
        if (normalMap != null) normalMap.dispose();
        if (hud != null) hud.dispose();
    }
}
