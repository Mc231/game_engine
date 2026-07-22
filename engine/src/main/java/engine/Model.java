package engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A multi-part model loaded from a Wavefront .obj: one {@link Mesh} + one
 * {@link Material} per {@code usemtl} group, with material colors/textures read
 * from the referenced .mtl library.
 *
 * All parts share the {@link ShaderProgram} passed to {@link #load}; textures
 * are fetched through a {@link ResourceManager} (which owns them). The model
 * owns only its meshes — {@link #dispose} frees those. Set the frame-wide
 * uniforms and {@code uModel} on the shader, then call {@link #render}.
 */
public class Model implements Disposable {

    private final List<Mesh> meshes = new ArrayList<>();
    private final List<Material> materials = new ArrayList<>();

    private Model() {
    }

    public static Model load(String objResource, ShaderProgram shader, ResourceManager resources) {
        String dir = directoryOf(objResource);
        OBJLoader.ModelData data = OBJLoader.parseModel(readResource(objResource));
        Map<String, MtlLoader.MaterialDef> library = data.mtlLib() != null
                ? MtlLoader.parse(readResource(dir + data.mtlLib()))
                : Map.of();

        Model model = new Model();
        for (OBJLoader.Part part : data.parts()) {
            Mesh mesh = new Mesh(part.mesh().vertices(), new int[]{3, 3, 2}, part.mesh().indices());

            MtlLoader.MaterialDef def = part.materialName() != null ? library.get(part.materialName()) : null;
            // Untextured materials use a 1x1 white texture, tinted by the diffuse color.
            Texture texture = (def != null && def.diffuseTexture() != null)
                    ? resources.texture(dir + def.diffuseTexture())
                    : resources.texture("textures/white.png");

            Material material = new Material(shader, texture);
            if (def != null) {
                material.setTint(def.diffuse().x, def.diffuse().y, def.diffuse().z)
                        .setShininess(def.shininess());
            }

            model.meshes.add(mesh);
            model.materials.add(material);
        }
        return model;
    }

    /** Draw every part with its own material. Caller sets frame uniforms + {@code uModel}. */
    public void render() {
        for (int i = 0; i < meshes.size(); i++) {
            materials.get(i).use();
            meshes.get(i).render();
        }
    }

    public int partCount() {
        return meshes.size();
    }

    @Override
    public void dispose() {
        for (Mesh mesh : meshes) {
            mesh.dispose();
        }
        meshes.clear();
        materials.clear();
    }

    private static String directoryOf(String resource) {
        int slash = resource.lastIndexOf('/');
        return slash >= 0 ? resource.substring(0, slash + 1) : "";
    }

    private static String readResource(String path) {
        try (InputStream in = Model.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Model resource not found on classpath: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read model: " + path, e);
        }
    }
}
