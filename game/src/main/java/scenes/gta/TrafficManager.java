package scenes.gta;

import engine.Disposable;
import engine.Model;
import engine.ResourceManager;
import engine.ShaderProgram;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Spawns and drives a small fleet of {@link TrafficCar}s on the city road grid.
 * All cars share one {@link Model} (drawn once per car with its own transform),
 * so traffic is cheap. Cars roam the grid indefinitely; the city is small enough
 * that streaming/recycling isn't needed yet.
 */
public class TrafficManager implements Disposable {

    private final Model model;
    private final ShaderProgram shader;
    private final City city;
    private final List<TrafficCar> cars = new ArrayList<>();
    private final Vector3f scratch = new Vector3f();

    public TrafficManager(ShaderProgram shader, ResourceManager resources, City city, int count, long seed) {
        this.shader = shader;
        this.city = city;
        this.model = Model.load("models/car/car.obj", shader, resources);
        Random rng = new Random(seed);
        for (int i = 0; i < count; i++) {
            cars.add(new TrafficCar(city, rng));
        }
    }

    private static final float CAR_SEPARATION = 3.4f;

    /**
     * @param pedPositions   pedestrian positions (traffic brakes for them)
     * @param playerCarPos   the player's car (traffic brakes for and is separated from it)
     */
    public void update(float dt, Vector3f[] pedPositions, Vector3f playerCarPos) {
        int n = cars.size();

        // Hazards each car brakes for: peds + player car + all traffic cars
        // (a car ignores the hazard at its own position).
        Vector3f[] hazards = new Vector3f[pedPositions.length + 1 + n];
        System.arraycopy(pedPositions, 0, hazards, 0, pedPositions.length);
        hazards[pedPositions.length] = playerCarPos;
        for (int i = 0; i < n; i++) {
            hazards[pedPositions.length + 1 + i] = cars.get(i).position();
        }

        for (TrafficCar c : cars) {
            c.update(dt, city.wallsNear(c.position(), 14f), hazards);
        }

        // Physical separation so cars never overlap: split pushes between two
        // traffic cars; push only the traffic car away from the player's car.
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                separate(cars.get(i).position(), cars.get(j).position(), true);
            }
            separate(cars.get(i).position(), playerCarPos, false);
        }
    }

    /** Push overlapping car centers apart; if {@code bothMove}, split the correction. */
    private void separate(Vector3f a, Vector3f b, boolean bothMove) {
        float dx = a.x - b.x, dz = a.z - b.z;
        float d2 = dx * dx + dz * dz;
        if (d2 >= CAR_SEPARATION * CAR_SEPARATION) {
            return;
        }
        float d = (float) Math.sqrt(d2);
        float nx, nz;
        if (d > 1e-4f) {
            nx = dx / d; nz = dz / d;
        } else {
            nx = 1f; nz = 0f;   // coincident: pick an arbitrary axis
            d = 0f;
        }
        float overlap = CAR_SEPARATION - d;
        if (bothMove) {
            a.x += nx * overlap * 0.5f; a.z += nz * overlap * 0.5f;
            b.x -= nx * overlap * 0.5f; b.z -= nz * overlap * 0.5f;
        } else {
            a.x += nx * overlap; a.z += nz * overlap;   // b (player car) stays put
        }
    }

    /** Draw all traffic cars. The lit shader must be bound with frame uniforms set. */
    public void render() {
        for (TrafficCar c : cars) {
            shader.setUniform("uModel", c.matrix());
            model.render();
        }
    }

    @Override
    public void dispose() {
        model.dispose();
    }
}
