package scenes.gta;

import engine.AABB;
import engine.CarController;
import engine.Collide;
import engine.Disposable;
import engine.Model;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A drivable car: a {@link Model} + a {@link CarController} + the local seat/exit
 * points and proximity used for on-foot entry. Keeps all per-car state in one
 * place so adding more cars later is trivial.
 *
 * <p>Heading convention matches {@link CarController} and the CC0 Kenney model:
 * the car faces +Z at heading 0, so its transform is
 * {@code translate(pos).rotateY(heading)} (no model-yaw offset).
 */
public class Vehicle implements Disposable {

    /** The CC0 car is ~1.1 tall / 2.55 long at scale 1 — half a person. Scale it up to read as a car. */
    public static final float MODEL_SCALE = 1.8f;

    private final Model model;
    private final CarController controller = new CarController();
    private final float enterRadius;
    private final Vector3f exitLocal;      // where the driver spawns on exit (car-local)
    private final Matrix4f matrix = new Matrix4f();
    private final Vector3f scratch = new Vector3f();

    public Vehicle(Model model, float startX, float startZ, float headingRad,
                   float enterRadius, Vector3f exitLocal) {
        this.model = model;
        this.enterRadius = enterRadius;
        this.exitLocal = new Vector3f(exitLocal);
        controller.setPosition(startX, 0f, startZ).setHeading(headingRad);
    }

    /** Body radius used for arcade collision against buildings (scaled car ≈ 2.3 wide / 4.6 long). */
    private static final float BODY_RADIUS = 2.0f;

    /** Advance the driving simulation (inputs already resolved to -1..1 / brake). */
    public void update(float dt, float throttle, float steer, boolean brake) {
        controller.update(dt, throttle, steer, brake, GtaGround.FLAT);
    }

    /** Push the car out of any overlapping building colliders (call after {@link #update}). */
    public void collide(AABB[] walls) {
        Collide.resolveCircle(controller.position(), BODY_RADIUS, walls);
    }

    /** True if a character at {@code p} is close enough to get in. */
    public boolean nearSeat(Vector3f p) {
        Vector3f c = controller.position();
        float dx = p.x - c.x, dz = p.z - c.z;
        return dx * dx + dz * dz <= enterRadius * enterRadius;
    }

    /** World position where the driver should appear when leaving the car. */
    public Vector3f worldExit(Vector3f dest) {
        float h = controller.heading();
        float sin = (float) Math.sin(h), cos = (float) Math.cos(h);
        // rotateY(h) applied to (lx,0,lz): (lx*cos + lz*sin, 0, -lx*sin + lz*cos)
        float wx = exitLocal.x * cos + exitLocal.z * sin;
        float wz = -exitLocal.x * sin + exitLocal.z * cos;
        Vector3f c = controller.position();
        return dest.set(c.x + wx, 0f, c.z + wz);
    }

    public Matrix4f matrix() {
        Vector3f c = controller.position();
        return matrix.identity().translate(c).rotateY(controller.heading()).scale(MODEL_SCALE);
    }

    public void render() {
        model.render();
    }

    public Vector3f position() {
        return controller.position();
    }

    /** Unit forward direction (new vector). */
    public Vector3f forward() {
        return controller.forward();
    }

    public float heading() {
        return controller.heading();
    }

    public float speed() {
        return controller.speed();
    }

    @Override
    public void dispose() {
        model.dispose();
    }
}
