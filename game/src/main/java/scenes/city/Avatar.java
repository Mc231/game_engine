package scenes.city;

import engine.Disposable;
import engine.Geometry;
import engine.Material;
import engine.Mesh;
import engine.ShaderProgram;
import engine.Texture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A blocky humanoid rendered from primitives (torso, belt, head, peaked cap,
 * two arms, two legs) with a <em>procedural</em> walk: arms and legs swing in
 * opposition, scaled by how fast the avatar is moving, plus a subtle body bob.
 * The engine has no skeletal animation, so this per-limb matrix animation is how
 * every character (player, pedestrians, police) moves.
 *
 * <p>Generalized from the Prison Break guard. Colors come from a {@link Palette}
 * so the same avatar serves the player and differently-dressed NPCs. Local +Z is
 * forward, so a facing angle drives {@code rotateY(facing)}.
 */
public class Avatar implements Disposable {

    /** Clothing/skin colors for one avatar. */
    public static final class Palette {
        public final Vector3f uniform, skin, cap, belt;

        public Palette(Vector3f uniform, Vector3f skin, Vector3f cap, Vector3f belt) {
            this.uniform = uniform;
            this.skin = skin;
            this.cap = cap;
            this.belt = belt;
        }
    }

    /** A neutral civilian look (blue top, tan skin, dark cap, black belt). */
    public static Palette civilian() {
        return new Palette(
                new Vector3f(0.25f, 0.42f, 0.62f),
                new Vector3f(0.80f, 0.62f, 0.48f),
                new Vector3f(0.15f, 0.16f, 0.20f),
                new Vector3f(0.10f, 0.10f, 0.11f));
    }

    private final Mesh cube;
    private final Material uniformMat, skinMat, capMat, beltMat;
    private final ShaderProgram shader;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f model = new Matrix4f();

    private float walkPhase;
    private float swing;   // current limb swing angle (rad)
    private float bob;     // current vertical body bob

    public Avatar(ShaderProgram shader, Texture white, Palette p) {
        this.shader = shader;
        this.cube = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});
        this.uniformMat = new Material(shader, white).setTint(p.uniform.x, p.uniform.y, p.uniform.z).setAmbientStrength(0.45f);
        this.skinMat = new Material(shader, white).setTint(p.skin.x, p.skin.y, p.skin.z).setAmbientStrength(0.5f);
        this.capMat = new Material(shader, white).setTint(p.cap.x, p.cap.y, p.cap.z).setAmbientStrength(0.45f);
        this.beltMat = new Material(shader, white).setTint(p.belt.x, p.belt.y, p.belt.z).setAmbientStrength(0.45f);
    }

    /**
     * Advance the walk animation. {@code speed} (world units/sec) scales the
     * stride cadence and swing amount, so a still avatar rests its arms and a
     * fast one strides hard. Call once per update with the frame's {@code dt}.
     */
    public void animate(float speed, float dt) {
        walkPhase += speed * dt * 1.4f;                       // cadence ∝ speed
        float amp = 0.5f * Math.min(1f, speed / 3.5f);        // rest arms when still
        swing = (float) Math.sin(walkPhase) * amp;
        bob = Math.abs((float) Math.sin(walkPhase)) * 0.04f * Math.min(1f, speed / 3.5f);
    }

    /**
     * Draw the avatar standing on the ground at {@code (pos.x, pos.y, pos.z)}
     * (feet at pos.y), facing {@code facing} radians. Uses the pose from the last
     * {@link #animate}. Frame uniforms (view/projection/lights) must be set first.
     */
    public void render(Vector3f pos, float facing) {
        base.identity().translate(pos.x, pos.y + bob, pos.z).rotateY(facing);

        uniformMat.use();
        part(0f, 1.24f, 0f, 0.52f, 0.82f, 0.30f);      // torso
        beltMat.use();
        part(0f, 0.86f, 0f, 0.54f, 0.12f, 0.32f);      // belt
        skinMat.use();
        part(0f, 1.86f, 0f, 0.30f, 0.30f, 0.28f);      // head
        capMat.use();
        part(0f, 2.05f, 0f, 0.36f, 0.14f, 0.36f);      // cap crown
        part(0f, 1.99f, 0.20f, 0.34f, 0.07f, 0.16f);   // cap peak (front)

        uniformMat.use();                               // arms swing...
        limb(-0.34f, 1.52f, 0f, swing, 0.68f, 0.15f, 0.15f);
        limb(0.34f, 1.52f, 0f, -swing, 0.68f, 0.15f, 0.15f);
        capMat.use();                                   // ...legs swing opposite
        limb(-0.15f, 0.86f, 0f, -swing, 0.82f, 0.19f, 0.20f);
        limb(0.15f, 0.86f, 0f, swing, 0.82f, 0.19f, 0.20f);
    }

    /** A box centered at (x,y,z) in avatar-local space. */
    private void part(float x, float y, float z, float sx, float sy, float sz) {
        shader.setUniform("uModel", model.set(base).translate(x, y, z).scale(sx, sy, sz));
        cube.render();
    }

    /** A limb box hanging from pivot (px,py,pz), rotated about X, extending len downward. */
    private void limb(float px, float py, float pz, float rotX, float len, float sx, float sz) {
        shader.setUniform("uModel", model.set(base)
                .translate(px, py, pz).rotateX(rotX).translate(0f, -len / 2f, 0f).scale(sx, len, sz));
        cube.render();
    }

    /** Approximate world-space bounding box for camera/collision use. */
    public float height() {
        return 2.12f;
    }

    @Override
    public void dispose() {
        cube.dispose();
    }
}
