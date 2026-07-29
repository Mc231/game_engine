package scenes.gta;

import engine.AABB;
import engine.Disposable;
import engine.Mesh;
import engine.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;

/**
 * A 2D top-down minimap drawn in a screen corner: a dark map panel, nearby
 * building rectangles, and colored blips (player centered, police, mission start
 * and active objective). Its own ortho 2D shader + unit quad; drawn last with
 * depth off and blending on. North-up (no rotation) for simplicity.
 */
public class Minimap implements Disposable {

    private static final int SIZE = 200;         // panel size (px)
    private static final int PAD = 16;           // margin from screen edge
    private static final float WORLD_RADIUS = 115f;   // world units shown across the half-panel

    private static final String VERT = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform mat4 uProjection; uniform mat4 uModel;
            void main() { gl_Position = uProjection * uModel * vec4(aPos, 1.0); }
            """;
    private static final String FRAG = """
            #version 330 core
            out vec4 FragColor; uniform vec3 uColor; uniform float uAlpha;
            void main() { FragColor = vec4(uColor, uAlpha); }
            """;

    private final ShaderProgram shader = new ShaderProgram(VERT, FRAG);
    private final Mesh quad = new Mesh(new float[]{
            0, 0, 0, 1, 0, 0, 1, 1, 0,
            0, 0, 0, 1, 1, 0, 0, 1, 0}, new int[]{3});
    private final Matrix4f ortho = new Matrix4f();
    private final Matrix4f m = new Matrix4f();

    private float cx, cy, scale;

    public void render(int fbw, int fbh, City city, Vector3f player,
                       List<Vector3f> police, MissionManager missions) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        ortho.identity().ortho(0f, fbw, fbh, 0f, -1f, 1f);   // top-left origin, y down
        shader.bind();
        shader.setUniform("uProjection", ortho);

        cx = fbw - PAD - SIZE / 2f;
        cy = fbh - PAD - SIZE / 2f;
        scale = (SIZE / 2f) / WORLD_RADIUS;

        rect(cx - SIZE / 2f, cy - SIZE / 2f, SIZE, SIZE, 0.08f, 0.09f, 0.11f, 0.75f);   // panel

        // Nearby buildings.
        for (AABB b : city.colliders) {
            Vector3f c = b.center();
            float dx = c.x - player.x, dz = c.z - player.z;
            if (dx * dx + dz * dz > WORLD_RADIUS * WORLD_RADIUS) {
                continue;
            }
            Vector3f s = b.size();
            float w = s.x * scale, h = s.z * scale;
            rect(cx + dx * scale - w / 2f, cy + dz * scale - h / 2f, w, h, 0.42f, 0.44f, 0.48f, 1f);
        }

        // Blips.
        for (Vector3f s : missions.availableStarts()) {
            blip(s, player, 1f, 0.82f, 0.2f);
        }
        Mission act = missions.active();
        if (act != null && act.current() != null) {
            blip(act.current().pos, player, 0.3f, 1f, 0.4f);
        }
        for (Vector3f p : police) {
            blip(p, player, 1f, 0.2f, 0.2f);
        }
        rect(cx - 3f, cy - 3f, 6f, 6f, 1f, 1f, 1f, 1f);   // player (center)

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }

    private void blip(Vector3f world, Vector3f player, float r, float g, float b) {
        float dx = world.x - player.x, dz = world.z - player.z;
        if (dx * dx + dz * dz > WORLD_RADIUS * WORLD_RADIUS) {
            return;
        }
        rect(cx + dx * scale - 3f, cy + dz * scale - 3f, 6f, 6f, r, g, b, 1f);
    }

    private final Vector3f color = new Vector3f();

    private void rect(float x, float y, float w, float h, float r, float g, float b, float a) {
        shader.setUniform("uModel", m.identity().translate(x, y, 0f).scale(w, h, 1f));
        shader.setUniform("uColor", color.set(r, g, b));
        shader.setUniform("uAlpha", a);
        quad.render();
    }

    @Override
    public void dispose() {
        quad.dispose();
        shader.dispose();
    }
}
