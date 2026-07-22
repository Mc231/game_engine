package scenes;

import engine.Camera;
import engine.CubemapTexture;
import engine.Framebuffer;
import engine.Geometry;
import engine.Hud;
import engine.InstancedMesh;
import engine.Input;
import engine.PostProcessor;
import engine.Scene;
import engine.ShaderProgram;
import engine.Skybox;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;

/**
 * Rendering showcase combining three section-C features:
 *   - a cubemap {@link Skybox},
 *   - a field of {@link InstancedMesh} cubes drawn in ONE call,
 *   - all rendered into an offscreen {@link Framebuffer} then run through a
 *     {@link PostProcessor} effect (cycle with E).
 *
 *   Move: W/A/S/D   Up/Down: Space / Left-Shift   Look: mouse
 *   Post FX: E   Switch scene: [ ]   Quit: Esc
 */
public class SkyboxScene implements Scene {

    private static final String INST_VERTEX = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec3 aNormal;
            layout (location = 2) in vec2 aTexCoord;
            layout (location = 3) in mat4 aInstance;
            out vec3 normal;
            out vec2 texCoord;
            uniform mat4 uView;
            uniform mat4 uProjection;
            void main() {
                normal = mat3(aInstance) * aNormal;
                texCoord = aTexCoord;
                gl_Position = uProjection * uView * aInstance * vec4(aPos, 1.0);
            }
            """;

    private static final String INST_FRAGMENT = """
            #version 330 core
            in vec3 normal;
            in vec2 texCoord;
            out vec4 FragColor;
            uniform sampler2D uTexture;
            uniform vec3 uLightDir;
            void main() {
                vec3 N = normalize(normal);
                float diff = max(dot(N, normalize(-uLightDir)), 0.0);
                vec3 base = texture(uTexture, texCoord).rgb;
                FragColor = vec4(base * (0.3 + 0.7 * diff), 1.0);
            }
            """;

    private static final String[] EFFECT_NAMES = {"none", "grayscale", "invert", "vignette"};

    private Framebuffer framebuffer;
    private Skybox skybox;
    private InstancedMesh cubes;
    private PostProcessor post;
    private Hud hud;
    private ShaderProgram instShader;
    private Texture texture;
    private Camera camera;
    private Input input;
    private Window window;

    private final Matrix4f projection = new Matrix4f();
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private int effect = PostProcessor.NONE;

    @Override
    public void init(Window window) {
        this.window = window;
        framebuffer = new Framebuffer(window.framebufferWidth(), window.framebufferHeight());
        skybox = new Skybox(new CubemapTexture(
                "skybox/right.png", "skybox/left.png", "skybox/top.png",
                "skybox/bottom.png", "skybox/front.png", "skybox/back.png"));
        post = new PostProcessor();
        hud = new Hud();
        instShader = new ShaderProgram(INST_VERTEX, INST_FRAGMENT);
        texture = new Texture("textures/crate.png");

        // A grid of cubes as instances (one draw call for all of them).
        int n = 24;
        float spacing = 2.2f;
        float[] cubeVerts = Geometry.cubeWithNormalsAndUV();
        int[] indices = new int[cubeVerts.length / 8];       // 36 vertices, identity indices
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i;
        }
        Matrix4f[] instances = new Matrix4f[n * n];
        int k = 0;
        for (int gx = 0; gx < n; gx++) {
            for (int gz = 0; gz < n; gz++) {
                float x = (gx - n / 2f) * spacing;
                float z = (gz - n / 2f) * spacing;
                float y = (float) (Math.sin(gx * 0.6) * Math.cos(gz * 0.6)) * 1.5f;
                float angle = (gx + gz) * 0.25f;
                instances[k++] = new Matrix4f().translate(x, y, z).rotateY(angle).scale(0.8f);
            }
        }
        cubes = new InstancedMesh(cubeVerts, new int[]{3, 3, 2}, indices, instances);

        camera = new Camera().setPosition(0f, 8f, 28f).setMoveSpeed(18f);
        input = window.input();
        input.setMouseCaptured(true);

        projection.identity().perspective(
                (float) Math.toRadians(60.0), window.aspectRatio(), 0.1f, 500f);
    }

    @Override
    public void update(float deltaSeconds) {
        camera.processInput(input, deltaSeconds);
        if (input.isKeyPressed(GLFW_KEY_E)) {
            effect = (effect + 1) % PostProcessor.EFFECT_COUNT;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        framebuffer.dispose();
        framebuffer = new Framebuffer(width, height);
        projection.identity().perspective(
                (float) Math.toRadians(60.0), (float) width / height, 0.1f, 500f);
    }

    @Override
    public void render() {
        Matrix4f view = camera.viewMatrix();

        // --- Pass 1: render the scene into the offscreen framebuffer ---
        framebuffer.bind();
        glClearColor(0f, 0f, 0f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        instShader.bind();
        instShader.setUniform("uProjection", projection);
        instShader.setUniform("uView", view);
        instShader.setUniform("uLightDir", lightDir);
        texture.bind(0);
        instShader.setUniform("uTexture", 0);
        cubes.render();                       // all instances, one call

        skybox.render(view, projection);      // fills the background

        framebuffer.unbind(window.framebufferWidth(), window.framebufferHeight());

        // --- Pass 2: post-process the framebuffer onto the screen ---
        post.render(framebuffer, effect);

        // --- HUD on top ---
        hud.begin(window.framebufferWidth(), window.framebufferHeight());
        hud.text(12, 12, 2f, "SKYBOX + INSTANCING + POST-FX", 1f, 1f, 1f);
        hud.text(12, 40, 2f, "Post FX: " + EFFECT_NAMES[effect] + "  (E to cycle,  [ ] scene)",
                1f, 0.95f, 0.5f);
        hud.end();
    }

    @Override
    public void dispose() {
        framebuffer.dispose();
        skybox.dispose();
        cubes.dispose();
        post.dispose();
        hud.dispose();
        instShader.dispose();
        texture.dispose();
    }
}
