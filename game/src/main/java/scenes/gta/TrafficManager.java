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

    public void update(float dt) {
        for (TrafficCar c : cars) {
            c.update(dt, city.wallsNear(c.position(), 14f));
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
