package engine;

/**
 * A {@link Component} that pairs a {@link Mesh} with a {@link Material}, marking its
 * entity as renderable. It owns neither resource, so it never disposes them.
 */
public class MeshRenderer extends Component {

    private final Mesh mesh;
    private final Material material;

    public MeshRenderer(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Mesh mesh() {
        return mesh;
    }

    public Material material() {
        return material;
    }
}
