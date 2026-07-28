package scenes.gta;

import org.joml.Vector3f;

/**
 * A single global traffic-signal cycle for the whole city: north–south green,
 * then a short amber, then east–west green, then amber, repeating. Every
 * intersection shares this phase (a simple, readable model). Amber is treated as
 * "stop" for braking, so there's a safe all-stop moment between greens.
 *
 * Axis convention: a car travelling along Z (between nodes that differ in j) is on
 * a N–S road; along X (differ in i) is on an E–W road.
 */
public class TrafficLights {

    public enum Phase { NS_GREEN, NS_AMBER, EW_GREEN, EW_AMBER }

    private static final Vector3f GREEN = new Vector3f(0.2f, 1f, 0.3f);
    private static final Vector3f AMBER = new Vector3f(1f, 0.72f, 0.1f);
    private static final Vector3f RED = new Vector3f(1f, 0.2f, 0.15f);

    private final float greenTime;
    private final float amberTime;
    private Phase phase = Phase.NS_GREEN;
    private float t;

    public TrafficLights() {
        this(7f, 2f);
    }

    public TrafficLights(float greenTime, float amberTime) {
        this.greenTime = greenTime;
        this.amberTime = amberTime;
    }

    public void update(float dt) {
        t += dt;
        float dur = (phase == Phase.NS_GREEN || phase == Phase.EW_GREEN) ? greenTime : amberTime;
        if (t >= dur) {
            t = 0f;
            phase = switch (phase) {
                case NS_GREEN -> Phase.NS_AMBER;
                case NS_AMBER -> Phase.EW_GREEN;
                case EW_GREEN -> Phase.EW_AMBER;
                case EW_AMBER -> Phase.NS_GREEN;
            };
        }
    }

    /** May a car on the given axis proceed through an intersection? (Amber = no.) */
    public boolean isGreen(boolean nsAxis) {
        return nsAxis ? phase == Phase.NS_GREEN : phase == Phase.EW_GREEN;
    }

    /** Lamp color for the N–S signal (for rendering). */
    public Vector3f nsColor() {
        return phase == Phase.NS_GREEN ? GREEN : (phase == Phase.NS_AMBER ? AMBER : RED);
    }

    /** Lamp color for the E–W signal (for rendering). */
    public Vector3f ewColor() {
        return phase == Phase.EW_GREEN ? GREEN : (phase == Phase.EW_AMBER ? AMBER : RED);
    }
}
