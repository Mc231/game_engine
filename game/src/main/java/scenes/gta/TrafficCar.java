package scenes.gta;

import engine.AABB;
import engine.CarController;
import engine.Collide;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Random;

/**
 * An AI traffic car that drives the city's road grid: it heads for a target
 * intersection node, and on arrival picks a neighbouring node (never immediately
 * reversing) and continues. Steering aligns the heading toward the target (the
 * {@link CarController} steering sign: positive steer increases heading), throttle
 * eases off in turns, and it pushes out of buildings as a safety net.
 */
public class TrafficCar {

    private static final float BODY_RADIUS = 2.0f;
    private static final float ARRIVE = 3.5f;
    // Forward zone in which a pedestrian/car makes this car brake.
    private static final float BRAKE_DIST = 8.5f;
    private static final float BRAKE_HALF_WIDTH = 2.6f;
    private final Vector3f fwd = new Vector3f();

    private final CarController ctrl = new CarController()
            .setMaxSpeed(13f).setEnginePower(14f).setTurnRate(2.2f).setRideHeight(0f);
    private final City city;
    private final Random rng;
    private final Matrix4f matrix = new Matrix4f();
    private final Vector3f nodePos = new Vector3f();

    private int ci, cj;   // node we came from
    private int ti, tj;   // node we're driving to

    public TrafficCar(City city, Random rng) {
        this.city = city;
        this.rng = rng;
        ci = rng.nextInt(city.nx + 1);
        cj = rng.nextInt(city.nz + 1);
        pickTargetFrom(ci, cj);
        city.node(ci, cj, nodePos);
        float h = (float) Math.atan2(city.node(ti, tj, new Vector3f()).x - nodePos.x,
                city.node(ti, tj, new Vector3f()).z - nodePos.z);
        ctrl.setPosition(nodePos.x, 0f, nodePos.z).setHeading(h);
    }

    /** Choose a target node adjacent to (i,j), avoiding the node we came from. */
    private void pickTargetFrom(int i, int j) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        int[] order = {0, 1, 2, 3};
        for (int k = order.length - 1; k > 0; k--) {   // shuffle
            int s = rng.nextInt(k + 1);
            int t = order[k]; order[k] = order[s]; order[s] = t;
        }
        int fallbackI = i, fallbackJ = j;
        for (int o : order) {
            int niv = i + dirs[o][0];
            int njv = j + dirs[o][1];
            if (niv < 0 || niv > city.nx || njv < 0 || njv > city.nz) {
                continue;
            }
            fallbackI = niv; fallbackJ = njv;
            if (niv != ci || njv != cj) {   // don't reverse if avoidable
                ti = niv; tj = njv;
                return;
            }
        }
        ti = fallbackI; tj = fallbackJ;
    }

    private static final float STOP_LINE = 6f;   // brake before a red-facing intersection

    /**
     * @param walls   nearby building colliders
     * @param hazards points (pedestrians, other cars) to brake for; a point at
     *                this car's own position is ignored (it never brakes for itself)
     * @param lights  the city signal cycle (may be null)
     */
    public void update(float dt, AABB[] walls, Vector3f[] hazards, TrafficLights lights) {
        Vector3f pos = ctrl.position();
        city.node(ti, tj, nodePos);

        float dx = nodePos.x - pos.x, dz = nodePos.z - pos.z;
        float distToNode = (float) Math.sqrt(dx * dx + dz * dz);
        if (distToNode < ARRIVE) {
            int oldTi = ti, oldTj = tj;
            ci = ti; cj = tj;               // arrived: this node becomes "came from"
            pickTargetFrom(oldTi, oldTj);
            city.node(ti, tj, nodePos);
            dx = nodePos.x - pos.x; dz = nodePos.z - pos.z;
            distToNode = (float) Math.sqrt(dx * dx + dz * dz);
        }

        float desired = (float) Math.atan2(dx, dz);
        float diff = (float) Math.atan2(Math.sin(desired - ctrl.heading()), Math.cos(desired - ctrl.heading()));
        float steer = Math.max(-1f, Math.min(1f, diff * 1.6f));

        // Stop at the approaching intersection when this car's road has a red/amber.
        boolean redLight = false;
        if (lights != null && distToNode < STOP_LINE) {
            boolean nsAxis = tj != cj;   // travelling along Z ⇒ N–S road
            redLight = !lights.isGreen(nsAxis);
        }

        boolean brake = redLight || hazardAhead(pos, hazards);
        float throttle = brake ? 0f : 1f - Math.min(0.65f, Math.abs(diff) * 0.6f);   // ease off in turns

        ctrl.update(dt, throttle, steer, brake, GtaGround.FLAT);
        Collide.resolveCircle(ctrl.position(), BODY_RADIUS, walls);
    }

    /** True if any hazard sits in the forward brake zone (ignores this car itself). */
    private boolean hazardAhead(Vector3f pos, Vector3f[] hazards) {
        if (hazards == null) {
            return false;
        }
        fwd.set((float) Math.sin(ctrl.heading()), 0f, (float) Math.cos(ctrl.heading()));
        for (Vector3f h : hazards) {
            float dx = h.x - pos.x, dz = h.z - pos.z;
            float ahead = dx * fwd.x + dz * fwd.z;              // forward distance
            if (ahead > 0.6f && ahead < BRAKE_DIST) {
                float lateral = Math.abs(dx * -fwd.z + dz * fwd.x);   // perpendicular offset
                if (lateral < BRAKE_HALF_WIDTH) {
                    return true;
                }
            }
        }
        return false;
    }

    public Vector3f position() {
        return ctrl.position();
    }

    public Matrix4f matrix() {
        Vector3f p = ctrl.position();
        return matrix.identity().translate(p).rotateY(ctrl.heading()).scale(Vehicle.MODEL_SCALE);
    }
}
