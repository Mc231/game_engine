package engine;

import org.joml.Vector3f;

/**
 * Instantiates a live ECS {@link World} from a {@link SceneData} description.
 * Shared assets (meshes, textures) are fetched through the {@link ResourceManager},
 * which owns and disposes them — so the caller disposes the manager, not the world.
 *
 * Supported component descriptors (see {@link ComponentData}):
 *   - {@code "mesh"}: a {@link MeshRenderer}; {@code mesh} is "cube" or an .obj path,
 *     {@code texture} a classpath image, plus {@code tint}/{@code shininess}.
 *   - {@code "light"}: a {@link LightComponent}; {@code lightType} in
 *     point/directional/spot, plus {@code color}.
 */
public final class SceneBuilder {

    private SceneBuilder() {
    }

    public static World build(SceneData data, ShaderProgram litShader, ResourceManager resources) {
        World world = new World();
        if (data == null || data.entities() == null) {
            return world;
        }
        for (EntityData ed : data.entities()) {
            Entity entity = new Entity(ed.name() != null ? ed.name() : "Entity");
            applyTransform(entity, ed);
            if (ed.components() != null) {
                for (ComponentData cd : ed.components()) {
                    addComponent(entity, cd, litShader, resources);
                }
            }
            world.add(entity);
        }
        return world;
    }

    private static void applyTransform(Entity entity, EntityData ed) {
        float[] p = ed.position();
        float[] r = ed.rotation();
        float[] s = ed.scale();
        if (p != null && p.length == 3) {
            entity.transform().setPosition(p[0], p[1], p[2]);
        }
        if (s != null && s.length == 3) {
            entity.transform().setScale(s[0], s[1], s[2]);
        }
        if (r != null && r.length == 3) {
            entity.transform().setRotationEuler(r[0], r[1], r[2]);
        }
    }

    private static void addComponent(Entity entity, ComponentData cd,
                                     ShaderProgram litShader, ResourceManager resources) {
        if ("mesh".equals(cd.type())) {
            Mesh mesh = resolveMesh(cd.mesh(), resources);
            Texture texture = resources.texture(cd.texture() != null ? cd.texture() : "textures/white.png");
            Material material = new Material(litShader, texture);
            if (cd.tint() != null && cd.tint().length == 3) {
                material.setTint(cd.tint()[0], cd.tint()[1], cd.tint()[2]);
            }
            if (cd.shininess() > 0f) {
                material.setShininess(cd.shininess());
            }
            entity.add(new MeshRenderer(mesh, material));
        } else if ("light".equals(cd.type())) {
            entity.add(new LightComponent(makeLight(cd)));
        }
    }

    private static Mesh resolveMesh(String key, ResourceManager resources) {
        if (key == null || key.equals("cube")) {
            // Procedural cube cached in the manager so it is shared + disposed centrally.
            return resources.get("mesh:cube",
                    () -> new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2}));
        }
        return resources.mesh(key);   // otherwise an .obj path
    }

    private static Light makeLight(ComponentData cd) {
        Vector3f color = (cd.color() != null && cd.color().length == 3)
                ? new Vector3f(cd.color()[0], cd.color()[1], cd.color()[2])
                : new Vector3f(1f, 1f, 1f);
        String type = cd.lightType() != null ? cd.lightType() : "point";
        return switch (type) {
            case "directional" -> Light.directional(new Vector3f(-0.3f, -1f, -0.5f), color);
            case "spot" -> Light.spot(new Vector3f(), new Vector3f(0f, -1f, 0f), color);
            default -> Light.point(new Vector3f(), color);
        };
    }
}
