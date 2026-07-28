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

    // Road network: intersection nodes sit at (offX + i*pitch, offZ + j*pitch)
    // for i in [0,nx], j in [0,nz] — the streets that separate the blocks.
    public final float pitch;
    public final int nx, nz;
    public final float offX, offZ;

    public City(Mesh ground, float groundHalf, InstancedMesh sidewalks,
                InstancedMesh[] buildingBatches, Vector3f[] buildingTints,
                List<AABB> colliders, SpatialGrid grid,
                Vector3f playerSpawn, Vector3f carSpawn, float carHeading,
                float pitch, int nx, int nz, float offX, float offZ) {
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
        this.pitch = pitch;
        this.nx = nx;
        this.nz = nz;
        this.offX = offX;
        this.offZ = offZ;
    }

    /** World position of road-intersection node (i, j), i in [0,nx], j in [0,nz]. */
    public Vector3f node(int i, int j, Vector3f dest) {
        return dest.set(offX + i * pitch, 0f, offZ + j * pitch);
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
