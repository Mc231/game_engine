package engine;

import org.joml.Vector3f;

/**
 * A half-line with an {@code origin} and a normalized {@code direction}. The
 * direction passed to the constructor is copied and normalized, so parameter
 * {@code t} in {@link #pointAt(float)} is measured in world-space units.
 */
public class Ray {

    public final Vector3f origin;
    public final Vector3f direction;

    /** Builds a ray; {@code direction} is copied and normalized. */
    public Ray(Vector3f origin, Vector3f direction) {
        this.origin = new Vector3f(origin);
        this.direction = new Vector3f(direction).normalize();
    }

    /** The point {@code origin + direction * t} (new vector). */
    public Vector3f pointAt(float t) {
        return new Vector3f(direction).mul(t).add(origin);
    }
}
