package scenes.gta;

import org.joml.Vector3f;

import java.util.List;

/**
 * A mission: an ordered chain of {@link Objective}s activated by entering its
 * start marker, paying {@code reward} on completion. State machine:
 * {@code AVAILABLE → ACTIVE → COMPLETE}.
 */
public class Mission {

    public enum State { AVAILABLE, ACTIVE, COMPLETE }

    public final String name;
    public final int reward;
    public final Vector3f start;
    private final List<Objective> objectives;
    private State state = State.AVAILABLE;
    private int index;

    public Mission(String name, int reward, Vector3f start, List<Objective> objectives) {
        this.name = name;
        this.reward = reward;
        this.start = new Vector3f(start.x, 0f, start.z);
        this.objectives = objectives;
    }

    public State state() {
        return state;
    }

    public void begin() {
        state = State.ACTIVE;
        index = 0;
    }

    /** Advance if the current objective is reached; returns true the frame it completes. */
    public boolean update(Vector3f pos) {
        if (state != State.ACTIVE) {
            return false;
        }
        if (objectives.get(index).reached(pos)) {
            index++;
            if (index >= objectives.size()) {
                state = State.COMPLETE;
                return true;
            }
        }
        return false;
    }

    /** The objective the player is currently pursuing, or null if not active. */
    public Objective current() {
        return state == State.ACTIVE ? objectives.get(index) : null;
    }

    public int step() {
        return index + 1;
    }

    public int steps() {
        return objectives.size();
    }
}
