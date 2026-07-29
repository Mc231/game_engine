package scenes.gta;

import org.joml.Vector3f;

/**
 * A looping time of day (0..1) that drives the sun direction, light color,
 * ambient level, and sky/fog color. The color model is hand-tuned (not physical):
 * dim blue night → warm dawn → bright noon → warm dusk. Feed the outputs into the
 * directional light, both shaders, and the clear color each frame.
 */
public class DayNightCycle {

    private final float dayLength;   // seconds for a full cycle
    private float t;                 // 0..1 (0 = midnight, 0.5 = noon)

    private final Vector3f sunDir = new Vector3f();
    private final Vector3f lightColor = new Vector3f();
    private final Vector3f skyColor = new Vector3f();
    private float ambient;

    public DayNightCycle(float dayLength, float startT) {
        this.dayLength = dayLength;
        this.t = startT;
        recompute();
    }

    public void update(float dt) {
        t += dt / dayLength;
        t -= (float) Math.floor(t);
        recompute();
    }

    private void recompute() {
        double ang = t * 2.0 * Math.PI;
        float el = (float) Math.sin(ang - Math.PI / 2.0);     // -1 midnight .. +1 noon
        float horiz = (float) Math.cos(Math.asin(Math.max(-1f, Math.min(1f, el))));
        // Sun position on the sky dome; light travels opposite (downward when up).
        sunDir.set((float) Math.cos(ang) * horiz, el, (float) Math.sin(ang) * horiz).negate();
        if (sunDir.lengthSquared() > 1e-6f) {
            sunDir.normalize();
        }

        float day = Math.max(0f, el);                          // 0 below horizon .. 1 noon
        float dusk = Math.max(0f, 1f - Math.abs(el) * 3f);     // horizon glow

        lightColor.set(
                0.45f + 0.55f * day + 0.4f * dusk,
                0.45f + 0.5f * day + 0.12f * dusk,
                0.5f + 0.45f * day);
        lightColor.mul(0.15f + 0.95f * day);                   // dim at night
        ambient = 0.16f + 0.34f * day;

        float skyScale = 0.25f + 0.9f * day;
        skyColor.set(
                Math.max(0.05f, (0.22f + 0.45f * day + 0.35f * dusk) * skyScale),
                Math.max(0.06f, (0.32f + 0.45f * day + 0.13f * dusk) * skyScale),
                Math.max(0.12f, (0.5f + 0.4f * day) * skyScale));
    }

    public Vector3f sunDir() {
        return sunDir;
    }

    public Vector3f lightColor() {
        return lightColor;
    }

    public Vector3f skyColor() {
        return skyColor;
    }

    public float ambient() {
        return ambient;
    }

    /** 0..1 time of day (0 = midnight, 0.5 = noon). */
    public float timeOfDay() {
        return t;
    }
}
