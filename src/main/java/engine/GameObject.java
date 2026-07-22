package engine;

/**
 * A renderable thing: a shared {@link Mesh}, a {@link Material} (its look), and
 * its own {@link Transform} (its placement). It owns none of the shared GPU
 * resources, so it never disposes them — the scene owns those lifetimes.
 *
 * Frame-wide uniforms (view, projection, lights) are set by the scene on the
 * material's shader; {@link #render()} adds only the per-object model matrix.
 */
public class GameObject {

    private final Mesh mesh;
    private final Material material;
    private final Transform transform = new Transform();

    public GameObject(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Transform transform() {
        return transform;
    }

    public Material material() {
        return material;
    }

    /** Apply the material, upload this object's model matrix, and draw. */
    public void render() {
        material.use();
        material.shader().setUniform("uModel", transform.matrix());
        mesh.render();
    }
}
