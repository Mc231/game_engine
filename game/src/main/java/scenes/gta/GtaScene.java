package scenes.gta;

import engine.AABB;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.Light;
import engine.Material;
import engine.Model;
import engine.OrbitCamera;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Grand Theft LWJGL (codename MiniCity) — the game scene, and the growth point
 * for every phase.
 *
 * <p>Phase 0/1: third-person on-foot {@link Avatar} + enter/drive/exit a
 * {@link Vehicle}. Phase 2: a procedural {@link City} of streets, sidewalks, and
 * instanced buildings you collide with (broad-phase via the city's spatial grid;
 * arcade car push-out via {@code Collide.resolveCircle}).
 *
 *   On foot: W/A/S/D move · Shift run · mouse look · F enter car
 *   Driving: W/S drive · A/D steer · Space brake · mouse orbit · F exit
 */
public class GtaScene implements Scene {

    private enum Mode { ON_FOOT, DRIVING }

    // Camera profiles per mode.
    private static final float FOOT_DISTANCE = 6.5f, FOOT_HEIGHT = 1.4f;
    private static final float CAR_DISTANCE = 10f, CAR_HEIGHT = 1.2f;
    // How far around a mover we query building colliders each frame.
    private static final float NEAR_RADIUS = 26f;

    private final ResourceManager resources = new ResourceManager();
    private ShaderProgram litShader;    // ground, car, avatar (Phong, textured)
    private ShaderProgram cityShader;   // instanced sidewalks + buildings (biplanar, directional)
    private Material asphaltMat;
    private Texture concreteTex;
    private Texture facadeTex;

    private City city;
    private Avatar avatar;
    private Vehicle car;
    private Mode mode = Mode.ON_FOOT;

    private ThirdPersonController player;
    private OrbitCamera camera;
    private Input input;
    private InputMap actions;
    private Hud hud;
    private Window window;

    private final Light[] lights = new Light[2];
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private final Vector3f lightColor = new Vector3f(1f, 0.97f, 0.9f);
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f identity = new Matrix4f();
    private final Vector3f tmp = new Vector3f();

    @Override
    public void init(Window window) {
        this.window = window;
        litShader = resources.shader("shaders/lit.vert", "shaders/lit.frag");
        cityShader = resources.shader("shaders/city.vert", "shaders/city.frag");

        Texture asphaltTex = resources.texture("textures/road.png");
        concreteTex = resources.texture("textures/wall.jpg");
        facadeTex = resources.texture("textures/facade.png");
        Texture white = resources.texture("textures/white.png");
        asphaltMat = new Material(litShader, asphaltTex).setTint(0.5f, 0.5f, 0.52f).setAmbientStrength(0.4f);

        city = CityGenerator.generate(new CityGenerator.Config());

        avatar = new Avatar(litShader, white, Avatar.civilian());
        Model carModel = Model.load("models/car/car.obj", litShader, resources);
        car = new Vehicle(carModel, city.carSpawn.x, city.carSpawn.z, city.carHeading, 3.6f, new Vector3f(-1.9f, 0f, 0f));

        lights[0] = Light.directional(lightDir, new Vector3f(0.9f, 0.88f, 0.82f));
        lights[1] = Light.point(new Vector3f(0f, 20f, 0f), new Vector3f(0.35f, 0.37f, 0.45f));

        player = new ThirdPersonController(city.playerSpawn.x, city.playerSpawn.z, 0f);
        camera = new OrbitCamera().setPitch(0.5f).setDistance(FOOT_DISTANCE).setTargetHeight(FOOT_HEIGHT);

        input = window.input();
        input.setMouseCaptured(true);
        actions = new InputMap()
                .bind("forward", GLFW_KEY_W, GLFW_KEY_UP)
                .bind("back", GLFW_KEY_S, GLFW_KEY_DOWN)
                .bind("left", GLFW_KEY_A, GLFW_KEY_LEFT)
                .bind("right", GLFW_KEY_D, GLFW_KEY_RIGHT)
                .bind("run", GLFW_KEY_LEFT_SHIFT, GLFW_KEY_RIGHT_SHIFT)
                .bind("brake", GLFW_KEY_SPACE);
        hud = new Hud();

        projection.identity().perspective((float) Math.toRadians(65.0), window.aspectRatio(), 0.1f, 600f);
    }

    @Override
    public void update(float deltaSeconds) {
        camera.addLook(input.mouseDeltaX(), input.mouseDeltaY());

        if (mode == Mode.ON_FOOT) {
            AABB[] near = city.wallsNear(player.position(), NEAR_RADIUS);
            float forward = (actions.isDown("forward", input) ? 1f : 0f) - (actions.isDown("back", input) ? 1f : 0f);
            float strafe = (actions.isDown("right", input) ? 1f : 0f) - (actions.isDown("left", input) ? 1f : 0f);
            boolean run = actions.isDown("run", input);
            player.update(deltaSeconds, forward, strafe, run, camera, near);
            avatar.animate(player.speed(), deltaSeconds);

            if (input.isKeyPressed(GLFW_KEY_F) && car.nearSeat(player.position())) {
                enterCar();
            }
            camera.setBaseYaw(0f);
            camera.update(player.position(), deltaSeconds, near);
        } else { // DRIVING
            AABB[] near = city.wallsNear(car.position(), NEAR_RADIUS);
            float throttle = (actions.isDown("forward", input) ? 1f : 0f) - (actions.isDown("back", input) ? 1f : 0f);
            float steer = (actions.isDown("left", input) ? 1f : 0f) - (actions.isDown("right", input) ? 1f : 0f);
            boolean brake = actions.isDown("brake", input);
            car.update(deltaSeconds, throttle, steer, brake);
            car.collide(near);

            if (input.isKeyPressed(GLFW_KEY_F)) {
                exitCar();
            }
            camera.setBaseYaw(car.heading() + (float) Math.PI);
            camera.update(car.position(), deltaSeconds, near);
        }
    }

    private void enterCar() {
        mode = Mode.DRIVING;
        camera.setDistance(CAR_DISTANCE).setTargetHeight(CAR_HEIGHT).resetYaw();
    }

    private void exitCar() {
        mode = Mode.ON_FOOT;
        car.worldExit(tmp);
        player.place(tmp.x, tmp.z, car.heading() + (float) (Math.PI / 2.0));   // face away from the door
        avatar.animate(0f, 0f);
        camera.setDistance(FOOT_DISTANCE).setTargetHeight(FOOT_HEIGHT).resetYaw();
    }

    @Override
    public void resize(int width, int height) {
        if (height == 0) return;
        projection.identity().perspective((float) Math.toRadians(65.0), (float) width / height, 0.1f, 600f);
    }

    @Override
    public void render() {
        glClearColor(0.55f, 0.66f, 0.78f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Matrix4f view = camera.viewMatrix();

        // --- Ground, car, avatar (lit/Phong shader) ---
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uFogDensity", 0f);
        litShader.setUniform("uLightCount", lights.length);
        for (int i = 0; i < lights.length; i++) lights[i].apply(litShader, "uLights[" + i + "]");

        asphaltMat.use();
        litShader.setUniform("uModel", identity);
        city.ground.render();

        litShader.setUniform("uModel", car.matrix());
        car.render();
        if (mode == Mode.ON_FOOT) {
            avatar.render(player.position(), player.facing());
        }

        // --- Sidewalks + buildings (instanced biplanar shader) ---
        cityShader.bind();
        cityShader.setUniform("uProjection", projection);
        cityShader.setUniform("uView", view);
        cityShader.setUniform("uLightDir", lightDir);
        cityShader.setUniform("uLightColor", lightColor);
        cityShader.setUniform("uAmbient", 0.45f);
        cityShader.setUniform("uTexture", 0);

        concreteTex.bind(0);
        cityShader.setUniform("uTexScale", 0.18f);
        cityShader.setUniform("uTint", tmp.set(0.82f, 0.82f, 0.84f));
        city.sidewalks.render();

        facadeTex.bind(0);
        cityShader.setUniform("uTexScale", 0.11f);
        for (int i = 0; i < city.buildingBatches.length; i++) {
            cityShader.setUniform("uTint", city.buildingTints[i]);
            city.buildingBatches[i].render();
        }

        renderHud();
    }

    private void renderHud() {
        int fbw = window.framebufferWidth();
        int fbh = window.framebufferHeight();
        hud.begin(fbw, fbh);
        hud.text(12, 12, 2.2f, "GRAND THEFT LWJGL  -  Phase 2 (city)", 1f, 1f, 1f);
        if (mode == Mode.ON_FOOT) {
            hud.text(12, 40, 2f, "ON FOOT   speed " + String.format("%.1f", player.speed()), 0.8f, 0.9f, 1f);
            String hint = car.nearSeat(player.position())
                    ? "[F] enter car    WASD move   Shift run" : "WASD move   Shift run   mouse look";
            hud.text(12, 64, 1.7f, hint, 0.75f, 0.85f, 0.9f);
        } else {
            hud.text(12, 40, 2f, "DRIVING   " + String.format("%.0f", Math.abs(car.speed()) * 3.6f) + " km/h", 1f, 0.9f, 0.7f);
            hud.text(12, 64, 1.7f, "[F] exit    W/S drive   A/D steer   Space brake", 0.85f, 0.85f, 0.8f);
        }
        hud.end();
    }

    @Override
    public void dispose() {
        city.dispose();
        avatar.dispose();
        car.dispose();
        hud.dispose();
        resources.dispose();
    }
}
