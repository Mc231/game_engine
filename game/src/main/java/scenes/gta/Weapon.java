package scenes.gta;

/**
 * A hitscan firearm: a fire-rate cooldown, ammo, per-shot damage, and range. The
 * scene owns the actual ray cast (against pedestrians/buildings); this class just
 * governs <em>when</em> a shot may happen. Configurable so more weapon types drop
 * in later.
 */
public class Weapon {

    private final String name;
    private final float fireInterval;   // seconds between shots
    private final float damage;
    private final float range;
    private int ammo;
    private float cooldown;

    public Weapon(String name, float fireRate, float damage, float range, int ammo) {
        this.name = name;
        this.fireInterval = 1f / fireRate;
        this.damage = damage;
        this.range = range;
        this.ammo = ammo;
    }

    /** A starter pistol: 4 shots/sec, ~3 shots to down a ped, 60m range. */
    public static Weapon pistol() {
        return new Weapon("PISTOL", 4f, 34f, 60f, 9999);
    }

    public void update(float dt) {
        if (cooldown > 0f) {
            cooldown -= dt;
        }
    }

    /** True if a shot fires this call (ready + has ammo); consumes a round + resets cooldown. */
    public boolean tryFire() {
        if (cooldown > 0f || ammo <= 0) {
            return false;
        }
        cooldown = fireInterval;
        ammo--;
        return true;
    }

    public String name() {
        return name;
    }

    public float damage() {
        return damage;
    }

    public float range() {
        return range;
    }

    public int ammo() {
        return ammo;
    }
}
