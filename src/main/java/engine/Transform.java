package engine;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Position + rotation + scale for an object, and the model matrix they produce.
 * Fields are public for direct tweaking; the fluent setters are for convenience.
 *
 * <pre>{@code
 * transform.setPosition(2, 0, -3).setScale(0.5f).setRotationEuler(0, angle, 0);
 * shader.setUniform("uModel", transform.matrix());
 * }</pre>
 */
public class Transform {

    public final Vector3f position = new Vector3f(0f, 0f, 0f);
    public final Quaternionf rotation = new Quaternionf();      // identity by default
    public final Vector3f scale = new Vector3f(1f, 1f, 1f);

    private final Matrix4f matrix = new Matrix4f();

    public Transform setPosition(float x, float y, float z) {
        position.set(x, y, z);
        return this;
    }

    public Transform setPosition(Vector3f p) {
        position.set(p);
        return this;
    }

    public Transform setScale(float uniform) {
        scale.set(uniform, uniform, uniform);
        return this;
    }

    public Transform setScale(float x, float y, float z) {
        scale.set(x, y, z);
        return this;
    }

    /** Set rotation from Euler angles in radians (applied X, then Y, then Z). */
    public Transform setRotationEuler(float x, float y, float z) {
        rotation.rotationXYZ(x, y, z);
        return this;
    }

    /** The model matrix: translate * rotate * scale, rebuilt from the current fields. */
    public Matrix4f matrix() {
        return matrix.identity().translationRotateScale(
                position.x, position.y, position.z,
                rotation.x, rotation.y, rotation.z, rotation.w,
                scale.x, scale.y, scale.z);
    }
}
