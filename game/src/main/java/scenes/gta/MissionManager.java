package scenes.gta;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a few courier missions from city road nodes, detects the player entering
 * an available mission's start marker, drives the active mission, and pays its
 * reward on completion. Exposes what to render (start markers + current objective)
 * and the active objective for the HUD.
 */
public class MissionManager {

    private static final float ENTER_RADIUS = 4.5f;
    private static final float OBJECTIVE_RADIUS = 5f;

    private final Economy economy;
    private final List<Mission> missions = new ArrayList<>();
    private Mission active;

    public MissionManager(City city, Economy economy) {
        this.economy = economy;

        // Candidate positions: interior road intersections (avoid the very edge + spawn).
        List<Vector3f> nodes = new ArrayList<>();
        for (int i = 1; i < city.nx; i++) {
            for (int j = 1; j < city.nz; j++) {
                Vector3f n = city.node(i, j, new Vector3f());
                if (n.lengthSquared() > 1f) {   // skip the central spawn node
                    nodes.add(n);
                }
            }
        }

        Random rng = new Random(2026L);
        String[] names = {"Courier Run", "Special Delivery", "Hot Package"};
        int[] rewards = {250, 400, 600};
        for (int m = 0; m < names.length && nodes.size() >= 3; m++) {
            Vector3f start = pop(nodes, rng);
            Vector3f o1 = pop(nodes, rng);
            Vector3f o2 = pop(nodes, rng);
            List<Objective> objs = List.of(
                    new Objective(o1.x, o1.z, OBJECTIVE_RADIUS, "Pick up the package"),
                    new Objective(o2.x, o2.z, OBJECTIVE_RADIUS, "Deliver the package"));
            missions.add(new Mission(names[m], rewards[m], start, objs));
        }
    }

    private static Vector3f pop(List<Vector3f> list, Random rng) {
        return list.remove(rng.nextInt(list.size()));
    }

    /** Update with the player's (or car's) position; returns a reward > 0 the frame a mission completes. */
    public int update(Vector3f pos) {
        if (active == null) {
            for (Mission m : missions) {
                if (m.state() == Mission.State.AVAILABLE && within(pos, m.start, ENTER_RADIUS)) {
                    m.begin();
                    active = m;
                    break;
                }
            }
            return 0;
        }
        if (active.update(pos)) {
            int reward = active.reward;
            economy.add(reward);
            active = null;
            return reward;
        }
        return 0;
    }

    public Mission active() {
        return active;
    }

    /** An available mission whose start marker the player is standing in (for the HUD prompt). */
    public Mission nearAvailable(Vector3f pos) {
        if (active != null) {
            return null;
        }
        for (Mission m : missions) {
            if (m.state() == Mission.State.AVAILABLE && within(pos, m.start, ENTER_RADIUS + 1f)) {
                return m;
            }
        }
        return null;
    }

    /** Start-marker positions for missions still available (to render as markers). */
    public List<Vector3f> availableStarts() {
        List<Vector3f> out = new ArrayList<>();
        for (Mission m : missions) {
            if (m.state() == Mission.State.AVAILABLE) {
                out.add(m.start);
            }
        }
        return out;
    }

    private static boolean within(Vector3f a, Vector3f b, float r) {
        float dx = a.x - b.x, dz = a.z - b.z;
        return dx * dx + dz * dz <= r * r;
    }
}
