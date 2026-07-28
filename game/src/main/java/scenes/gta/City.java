package scenes.gta;

import engine.AABB;
import engine.Disposable;
import engine.InstancedMesh;
import engine.Mesh;
import engine.SpatialGrid;
import org.joml.Vector3f;

import java.util.List;

/**
 * A generated city: the render meshes (asphalt ground, instanced sidewalk slabs,
 * instanced building batches grouped by tint) plus the collision data (building
 * {@link AABB}s and a {@link SpatialGrid} broad-phase) and spawn points.
 *
 * <p>Pure content produced by {@link CityGenerator}; the scene owns the shaders
 * and textures and does the actual draw calls. Disposes only the meshes it owns.
 */
public class City implements Disposable {

    public final Mesh ground;                 // big asphalt quad (drawn with the lit shader)
    public final float groundHalf;            // half-extent of the ground quad
    public final InstancedMesh sidewalks;     // concrete slabs
    public final InstancedMesh[] buildingBatches;
    public final Vector3f[] buildingTints;    // one tint per batch (parallel to buildingBatches)
    public final List<AABB> colliders;        // one per building
    public final SpatialGrid grid;

    public final Vector3f playerSpawn;
    public final Vector3f carSpawn;
    public final float carHeading;

    public City(Mesh ground, float groundHalf, InstancedMesh sidewalks,
                InstancedMesh[] buildingBatches, Vector3f[] buildingTints,
                List<AABB> colliders, SpatialGrid grid,
                Vector3f playerSpawn, Vector3f carSpawn, float carHeading) {
        this.ground = ground;
        this.groundHalf = groundHalf;
        this.sidewalks = sidewalks;
        this.buildingBatches = buildingBatches;
        this.buildingTints = buildingTints;
        this.colliders = colliders;
        this.grid = grid;
        this.playerSpawn = playerSpawn;
        this.carSpawn = carSpawn;
        this.carHeading = carHeading;
    }

    /** Building colliders near a world XZ position, via the broad-phase grid. */
    public AABB[] wallsNear(Vector3f p, float radius) {
        return grid.nearby(p.x, p.z, radius);
    }

    /** Building colliders near a world XZ coordinate. */
    public AABB[] wallsNear(float x, float z, float radius) {
        return grid.nearby(x, z, radius);
    }

    @Override
    public void dispose() {
        ground.dispose();
        sidewalks.dispose();
        for (InstancedMesh b : buildingBatches) {
            b.dispose();
        }
    }
}
