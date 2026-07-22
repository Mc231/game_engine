package engine;

import org.joml.Vector3f;

/**
 * A single light of one of three types. One class covers all three so the
 * shader can hold a uniform array of mixed lights and branch on {@link #type}.
 *
 *   - DIRECTIONAL: uses {@code direction} + {@code color} (no position, no falloff).
 *   - POINT:       uses {@code position} + {@code color} + attenuation.
 *   - SPOT:        POINT plus {@code direction} and the {@code cutOff} cone.
 *
 * {@link #apply} uploads every field under a struct uniform like
 * {@code uLights[2]}; fields irrelevant to the type are ignored by the shader.
 */
public class Light {

    public enum Type { DIRECTIONAL, POINT, SPOT }

    public Type type;
    public final Vector3f position = new Vector3f();
    public final Vector3f direction = new Vector3f(0f, -1f, 0f);
    public final Vector3f color = new Vector3f(1f, 1f, 1f);

    // Distance attenuation (point & spot): 1 / (c + l*d + q*d^2).
    public float constant = 1.0f;
    public float linear = 0.09f;
    public float quadratic = 0.032f;

    // Spot cone, stored as cosines of the inner/outer angles.
    public float cutOff = (float) Math.cos(Math.toRadians(12.5));
    public float outerCutOff = (float) Math.cos(Math.toRadians(17.5));

    public static Light directional(Vector3f direction, Vector3f color) {
        Light l = new Light();
        l.type = Type.DIRECTIONAL;
        l.direction.set(direction);
        l.color.set(color);
        return l;
    }

    public static Light point(Vector3f position, Vector3f color) {
        Light l = new Light();
        l.type = Type.POINT;
        l.position.set(position);
        l.color.set(color);
        return l;
    }

    public static Light spot(Vector3f position, Vector3f direction, Vector3f color) {
        Light l = new Light();
        l.type = Type.SPOT;
        l.position.set(position);
        l.direction.set(direction);
        l.color.set(color);
        return l;
    }

    /** Set the spot cone from inner/outer angles in degrees. */
    public Light setCone(float innerDegrees, float outerDegrees) {
        cutOff = (float) Math.cos(Math.toRadians(innerDegrees));
        outerCutOff = (float) Math.cos(Math.toRadians(outerDegrees));
        return this;
    }

    /** Upload every field of this light under the given struct uniform name. */
    public void apply(ShaderProgram shader, String name) {
        shader.setUniform(name + ".type", type.ordinal());
        shader.setUniform(name + ".position", position);
        shader.setUniform(name + ".direction", direction);
        shader.setUniform(name + ".color", color);
        shader.setUniform(name + ".constant", constant);
        shader.setUniform(name + ".linear", linear);
        shader.setUniform(name + ".quadratic", quadratic);
        shader.setUniform(name + ".cutOff", cutOff);
        shader.setUniform(name + ".outerCutOff", outerCutOff);
    }
}
