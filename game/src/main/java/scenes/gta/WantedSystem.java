package scenes.gta;

/**
 * The wanted level: crimes add <em>heat</em>, which decays over time (faster while
 * the player is unseen by police), and maps to a 0–5 <em>star</em> rating via
 * thresholds. Pure logic — no rendering or GL — so it's unit-tested.
 */
public class WantedSystem {

    // Star i requires heat >= HEAT_FOR_STARS[i-1].
    private static final float[] HEAT_FOR_STARS = {30f, 90f, 180f, 300f, 450f};
    private static final float MAX_HEAT = 600f;
    private static final float DECAY_SEEN = 2.5f;     // heat/sec while police can see you
    private static final float DECAY_UNSEEN = 9f;     // heat/sec while hidden

    private float heat;

    /** Register a crime worth {@code amount} heat. */
    public void addCrime(float amount) {
        heat = Math.min(MAX_HEAT, heat + amount);
    }

    /** Decay heat; pass whether police currently have eyes on the player. */
    public void update(float dt, boolean seen) {
        float decay = (seen ? DECAY_SEEN : DECAY_UNSEEN) * dt;
        heat = Math.max(0f, heat - decay);
    }

    /** Current star rating, 0–5. */
    public int stars() {
        int s = 0;
        for (float threshold : HEAT_FOR_STARS) {
            if (heat >= threshold) {
                s++;
            } else {
                break;
            }
        }
        return s;
    }

    public boolean wanted() {
        return stars() > 0;
    }

    public float heat() {
        return heat;
    }

    /** Clear all heat (e.g. on bust / respawn). */
    public void clear() {
        heat = 0f;
    }
}
