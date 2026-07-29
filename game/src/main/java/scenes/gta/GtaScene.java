package scenes.gta;

import engine.AABB;
import engine.Audio;
import engine.Geometry;
import engine.Hud;
import engine.Input;
import engine.InputMap;
import engine.Light;
import engine.Material;
import engine.Mesh;
import engine.Model;
import engine.OrbitCamera;
import engine.ResourceManager;
import engine.Scene;
import engine.ShaderProgram;
import engine.Sound;
import engine.Texture;
import engine.Window;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

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
    // Fog / horizon.
    private static final float FOG_DENSITY = 0.006f;
    // Street-furniture placement offsets (≈ half the road width).
    private static final float LIGHT_CORNER = 5.5f, LAMP_SIDE = 5.5f;

    private ShaderProgram litShader;    // ground, car, avatar, props (Phong, textured)
    private ShaderProgram cityShader;   // instanced sidewalks + buildings (biplanar, directional)
    private Material asphaltMat;
    private Material postMat;           // grey light poles
    private Material lampMat;           // emissive lamp heads (tint set per draw)
    private Mesh propCube;              // unit cube for poles/lamps
    private Texture concreteTex;
    private Texture facadeTex;

    private City city;
    private Avatar avatar;
    private Vehicle car;
    private PedManager peds;
    private TrafficManager traffic;
    private TrafficLights lights;
    private Vector3f[] lightNodes;      // intersection signal posts
    private Vector3f[] lampPosts;       // decorative street lamps
    private Mode mode = Mode.ON_FOOT;
    private boolean aiming;
    private int pedsHit;

    // Combat.
    private Weapon weapon;
    private Audio audio;
    private Sound gunshot;
    private float health = 100f;
    private float flashTimer;
    private final Vector3f muzzlePos = new Vector3f();
    private final Vector3f aim = new Vector3f();

    // Wanted level + police.
    private WantedSystem wanted;
    private PoliceManager police;
    private float damagedTimer;   // suppresses health regen after being shot
    private float wastedFlash;

    // Missions + money.
    private Economy economy;
    private MissionManager missions;
    private float rewardFlash;
    private int lastReward;

    // Polish: day/night, minimap, audio, save.
    private DayNightCycle dayNight;
    private Minimap minimap;
    private Sound cash;
    private Sound siren;
    private float sirenTimer;

    private ThirdPersonController player;
    private OrbitCamera camera;
    private Input input;
    private InputMap actions;
    private Hud hud;
    private Window window;

    private final Light[] worldLights = new Light[2];
    private final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f).normalize();
    private final Matrix4f projection = new Matrix4f();
    private final Matrix4f identity = new Matrix4f();
    private final Matrix4f model = new Matrix4f();
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
        postMat = new Material(litShader, white).setTint(0.2f, 0.2f, 0.23f).setAmbientStrength(0.5f);
        lampMat = new Material(litShader, white).setTint(1f, 1f, 1f).setAmbientStrength(1f);
        propCube = new Mesh(Geometry.cubeWithNormalsAndUV(), new int[]{3, 3, 2});

        city = CityGenerator.generate(new CityGenerator.Config());
        lights = new TrafficLights();
        buildStreetFurniture();

        avatar = new Avatar(litShader, white, Avatar.civilian());
        Model carModel = Model.load("models/car/car.obj", litShader, resources);
        car = new Vehicle(carModel, city.carSpawn.x, city.carSpawn.z, city.carHeading, 4.2f, new Vector3f(-2.7f, 0f, 0f));
        peds = new PedManager(litShader, white, city, 40, 99L);
        traffic = new TrafficManager(litShader, resources, city, 14, 7L);

        worldLights[0] = Light.directional(lightDir, new Vector3f(0.9f, 0.88f, 0.82f));
        worldLights[1] = Light.point(new Vector3f(0f, 20f, 0f), new Vector3f(0.35f, 0.37f, 0.45f));

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

        weapon = Weapon.pistol();
        audio = new Audio();
        gunshot = new Sound("sounds/gunshot.wav");
        wanted = new WantedSystem();
        police = new PoliceManager(litShader, white, city, gunshot);

        SaveGame.State saved = SaveGame.load();
        economy = new Economy(saved.money);
        missions = new MissionManager(city, economy);
        missions.markCompleted(saved.completed);

        dayNight = new DayNightCycle(150f, 0.32f);   // ~2.5 min cycle, start mid-morning
        minimap = new Minimap();
        cash = new Sound("sounds/cash.wav");
        siren = new Sound("sounds/siren.wav");

        projection.identity().perspective((float) Math.toRadians(65.0), window.aspectRatio(), 0.1f, 600f);
    }

    /** Precompute signal-post positions (intersections) and decorative lamp posts (road edges). */
    private void buildStreetFurniture() {
        List<Vector3f> nodes = new ArrayList<>();
        for (int i = 0; i <= city.nx; i++) {
            for (int j = 0; j <= city.nz; j++) {
                nodes.add(city.node(i, j, new Vector3f()));
            }
        }
        lightNodes = nodes.toArray(new Vector3f[0]);

        List<Vector3f> lamps = new ArrayList<>();
        for (int i = 0; i < city.nx; i++) {          // midpoints of E–W road segments
            for (int j = 0; j <= city.nz; j++) {
                lamps.add(new Vector3f(city.offX + (i + 0.5f) * city.pitch, 0f, city.offZ + j * city.pitch + LAMP_SIDE));
            }
        }
        for (int i = 0; i <= city.nx; i++) {          // midpoints of N–S road segments
            for (int j = 0; j < city.nz; j++) {
                lamps.add(new Vector3f(city.offX + i * city.pitch + LAMP_SIDE, 0f, city.offZ + (j + 0.5f) * city.pitch));
            }
        }
        lampPosts = lamps.toArray(new Vector3f[0]);
    }

    private void fireWeapon() {
        if (!weapon.tryFire()) {
            return;
        }
        // Shoot from the camera THROUGH the crosshair (screen center) so bullets
        // land where the reticle points, not parallel-offset from it.
        Vector3f origin = new Vector3f(camera.position());
        aim.set(camera.aimDirection());
        AABB[] walls = city.wallsNear(player.position(), weapon.range());
        // Police take priority; otherwise a hit ped is a crime that raises the wanted level.
        boolean hitCop = police.shoot(origin, aim, weapon.range(), weapon.damage(), walls);
        if (!hitCop && peds.shoot(origin, aim, weapon.range(), weapon.damage(), walls)) {
            wanted.addCrime(20f);
        }
        Vector3f fwd = camera.forwardXZ();
        muzzlePos.set(player.position().x + fwd.x * 0.9f, 1.4f, player.position().z + fwd.z * 0.9f);
        flashTimer = 0.05f;
        gunshot.play();
    }

    @Override
    public void update(float deltaSeconds) {
        lights.update(deltaSeconds);
        dayNight.update(deltaSeconds);
        if (wanted.wanted()) {
            sirenTimer -= deltaSeconds;
            if (sirenTimer <= 0f) {
                siren.play();
                sirenTimer = 1.1f;
            }
        } else {
            sirenTimer = 0f;
        }
        if (flashTimer > 0f) flashTimer -= deltaSeconds;
        if (wastedFlash > 0f) wastedFlash -= deltaSeconds;
        if (rewardFlash > 0f) rewardFlash -= deltaSeconds;
        camera.addLook(input.mouseDeltaX(), input.mouseDeltaY());

        if (mode == Mode.ON_FOOT) {
            AABB[] near = city.wallsNear(player.position(), NEAR_RADIUS);
            float forward = (actions.isDown("forward", input) ? 1f : 0f) - (actions.isDown("back", input) ? 1f : 0f);
            float strafe = (actions.isDown("right", input) ? 1f : 0f) - (actions.isDown("left", input) ? 1f : 0f);
            boolean run = actions.isDown("run", input);
            player.update(deltaSeconds, forward, strafe, run, camera, near);
            avatar.animate(player.speed(), deltaSeconds);

            weapon.update(deltaSeconds);
            if (input.isMouseButtonPressed(GLFW_MOUSE_BUTTON_LEFT)) {
                fireWeapon();
            }

            if (input.isKeyPressed(GLFW_KEY_F) && car.nearSeat(player.position())) {
                enterCar();
            }
            // Hold RMB to aim over-the-shoulder: shift the camera off the character + zoom in.
            aiming = input.isMouseButtonDown(GLFW_MOUSE_BUTTON_RIGHT);
            camera.setShoulder(aiming ? 1.6f : 0f)
                    .setDistance(aiming ? 4.0f : FOOT_DISTANCE)
                    .setTargetHeight(aiming ? 1.55f : FOOT_HEIGHT);
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

        // Pedestrians flee the on-foot player, or a moving car.
        Vector3f playerThreat = mode == Mode.ON_FOOT ? player.position() : null;
        Vector3f carThreat = mode == Mode.DRIVING ? car.position() : null;
        Vector3f anchor = mode == Mode.DRIVING ? car.position() : player.position();
        int carHits = peds.update(deltaSeconds, playerThreat, carThreat, anchor,
                car.position(), car.forward(), car.speed());
        pedsHit += carHits;
        if (carHits > 0) wanted.addCrime(15f * carHits);
        traffic.update(deltaSeconds, peds.positions(), car.position(), lights);

        // Police response + wanted decay.
        Vector3f target = mode == Mode.DRIVING ? car.position() : player.position();
        float dmg = police.update(deltaSeconds, wanted.stars(), target);
        if (dmg > 0f) {
            health -= dmg;
            damagedTimer = 2.5f;
        }
        wanted.update(deltaSeconds, police.seesTarget());
        if (damagedTimer > 0f) damagedTimer -= deltaSeconds;
        if (!wanted.wanted() && damagedTimer <= 0f && health < 100f) {
            health = Math.min(100f, health + 7f * deltaSeconds);   // slow regen when clean
        }
        if (health <= 0f) wasted();

        // Missions (use the same active position as the police target).
        int reward = missions.update(target);
        if (reward > 0) {
            lastReward = reward;
            rewardFlash = 3.5f;
            cash.play();
            SaveGame.save(economy.money(), missions.completedNames());
        }
    }

    private void wasted() {
        mode = Mode.ON_FOOT;
        player.place(city.playerSpawn.x, city.playerSpawn.z, 0f);
        health = 100f;
        wanted.clear();
        police.clearAll();
        damagedTimer = 0f;
        wastedFlash = 2.5f;
        camera.setDistance(FOOT_DISTANCE).setTargetHeight(FOOT_HEIGHT).setBaseYaw(0f).resetYaw();
    }

    private void enterCar() {
        mode = Mode.DRIVING;
        camera.setShoulder(0f).setDistance(CAR_DISTANCE).setTargetHeight(CAR_HEIGHT).resetYaw();
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
        Vector3f sky = dayNight.skyColor();
        Vector3f sun = dayNight.sunDir();
        Vector3f sunColor = dayNight.lightColor();
        worldLights[0] = Light.directional(sun, sunColor);   // the moving sun

        glClearColor(sky.x, sky.y, sky.z, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        Matrix4f view = camera.viewMatrix();

        // --- Ground, car, avatar, props (lit/Phong shader) ---
        litShader.bind();
        litShader.setUniform("uProjection", projection);
        litShader.setUniform("uView", view);
        litShader.setUniform("uViewPos", camera.position());
        litShader.setUniform("uFogColor", sky);
        litShader.setUniform("uFogDensity", FOG_DENSITY);
        litShader.setUniform("uLightCount", worldLights.length);
        for (int i = 0; i < worldLights.length; i++) worldLights[i].apply(litShader, "uLights[" + i + "]");

        asphaltMat.use();
        litShader.setUniform("uModel", identity);
        city.ground.render();

        litShader.setUniform("uModel", car.matrix());
        car.render();
        traffic.render();
        if (mode == Mode.ON_FOOT) {
            avatar.render(player.position(), player.facing());
        }
        peds.render();
        police.render();
        renderStreetFurniture();
        renderMissionMarkers();

        if (flashTimer > 0f) {   // muzzle flash
            lampMat.use();
            lamp(muzzlePos.x, muzzlePos.y, muzzlePos.z, tmp.set(1f, 0.95f, 0.5f));
        }

        // --- Sidewalks + buildings (instanced biplanar shader) ---
        cityShader.bind();
        cityShader.setUniform("uProjection", projection);
        cityShader.setUniform("uView", view);
        cityShader.setUniform("uViewPos", camera.position());
        cityShader.setUniform("uFogColor", sky);
        cityShader.setUniform("uFogDensity", FOG_DENSITY);
        cityShader.setUniform("uLightDir", sun);
        cityShader.setUniform("uLightColor", sunColor);
        cityShader.setUniform("uAmbient", dayNight.ambient());
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

        Vector3f here = mode == Mode.DRIVING ? car.position() : player.position();
        minimap.render(window.framebufferWidth(), window.framebufferHeight(),
                city, here, police.positions(), missions);
    }

    /** Signal posts at intersections (colored by phase) + decorative street lamps. */
    private void renderStreetFurniture() {
        postMat.use();
        for (Vector3f n : lightNodes) pole(n.x + LIGHT_CORNER, n.z + LIGHT_CORNER);
        for (Vector3f p : lampPosts) pole(p.x, p.z);

        lampMat.use();
        Vector3f ns = lights.nsColor(), ew = lights.ewColor();
        for (Vector3f n : lightNodes) {
            lamp(n.x + LIGHT_CORNER, 4.3f, n.z + LIGHT_CORNER, ns);   // N–S signal head
            lamp(n.x + LIGHT_CORNER, 3.5f, n.z + LIGHT_CORNER, ew);   // E–W signal head
        }
        Vector3f warm = tmp.set(1f, 0.85f, 0.55f);
        for (Vector3f p : lampPosts) lamp(p.x, 4.1f, p.z, warm);
    }

    /** Glowing pillars: gold for available mission starts, green for the current objective. */
    private void renderMissionMarkers() {
        lampMat.use();
        for (Vector3f s : missions.availableStarts()) {
            marker(s.x, s.z, tmp.set(1f, 0.82f, 0.2f));
        }
        Mission m = missions.active();
        if (m != null && m.current() != null) {
            marker(m.current().pos.x, m.current().pos.z, tmp.set(0.3f, 1f, 0.4f));
        }
    }

    private void marker(float x, float z, Vector3f color) {
        litShader.setUniform("uTint", color);
        litShader.setUniform("uModel", model.identity().translate(x, 4f, z).scale(0.7f, 8f, 0.7f));
        propCube.render();
    }

    private void pole(float x, float z) {
        litShader.setUniform("uModel", model.identity().translate(x, 2.2f, z).scale(0.22f, 4.4f, 0.22f));
        propCube.render();
    }

    private void lamp(float x, float y, float z, Vector3f color) {
        litShader.setUniform("uTint", color);
        litShader.setUniform("uModel", model.identity().translate(x, y, z).scale(0.5f, 0.5f, 0.5f));
        propCube.render();
    }

    private void renderHud() {
        int fbw = window.framebufferWidth();
        int fbh = window.framebufferHeight();
        hud.begin(fbw, fbh);
        hud.text(12, 12, 2.2f, "GRAND THEFT LWJGL  -  v0.4", 1f, 1f, 1f);

        int stars = wanted.stars();
        if (stars > 0) {
            StringBuilder s = new StringBuilder("WANTED ");
            for (int i = 0; i < stars; i++) s.append("* ");
            hud.text(fbw - 220f, 12f, 2.4f, s.toString(), 1f, 0.85f, 0.2f);
        }
        if (wastedFlash > 0f) {
            hud.text(fbw / 2f - 120f, fbh / 2f - 60f, 5f, "WASTED", 1f, 0.25f, 0.2f);
        }

        // Money + mission status.
        hud.text(fbw - 220f, 44f, 2.6f, "$" + economy.money(), 0.5f, 1f, 0.5f);
        Vector3f here = mode == Mode.DRIVING ? car.position() : player.position();
        Mission m = missions.active();
        if (m != null && m.current() != null) {
            float ox = here.x - m.current().pos.x, oz = here.z - m.current().pos.z;
            float d = (float) Math.sqrt(ox * ox + oz * oz);
            hud.text(12, fbh - 60f, 2f, m.name + " (" + m.step() + "/" + m.steps() + "):  "
                    + m.current().label + "  " + String.format("%.0f", d) + "m", 1f, 0.95f, 0.5f);
        } else {
            Mission near = missions.nearAvailable(here);
            if (near != null) {
                hud.text(12, fbh - 60f, 2f, "MISSION: " + near.name + "  ($" + near.reward + ") - stand here",
                        1f, 0.85f, 0.4f);
            }
        }
        if (rewardFlash > 0f) {
            hud.text(fbw / 2f - 160f, fbh / 2f + 40f, 3.5f, "MISSION COMPLETE  +$" + lastReward, 0.5f, 1f, 0.5f);
        }
        if (mode == Mode.ON_FOOT) {
            hud.text(12, 40, 2f, "ON FOOT   health " + String.format("%.0f", health)
                    + "   " + weapon.name() + " " + weapon.ammo(), 0.8f, 0.9f, 1f);
            String hint = car.nearSeat(player.position())
                    ? "[F] enter car    LMB shoot  RMB aim    WASD move" : "LMB shoot   RMB aim   WASD move   Shift run";
            hud.text(12, 64, 1.7f, hint, 0.75f, 0.85f, 0.9f);
            // crosshair — tighter red reticle while aiming, faint white otherwise
            float cx = fbw / 2f, cy = fbh / 2f;
            if (aiming) {
                hud.text(cx - 5f, cy - 9f, 2.6f, "o", 1f, 0.3f, 0.25f);
            } else {
                hud.text(cx - 4f, cy - 8f, 2f, "+", 0.85f, 0.85f, 0.85f);
            }
        } else {
            hud.text(12, 40, 2f, "DRIVING   " + String.format("%.0f", Math.abs(car.speed()) * 3.6f) + " km/h"
                    + (pedsHit > 0 ? "     hits " + pedsHit : ""), 1f, 0.9f, 0.7f);
            hud.text(12, 64, 1.7f, "[F] exit    W/S drive   A/D steer   Space brake", 0.85f, 0.85f, 0.8f);
        }
        hud.end();
    }

    @Override
    public void dispose() {
        city.dispose();
        propCube.dispose();
        avatar.dispose();
        car.dispose();
        SaveGame.save(economy.money(), missions.completedNames());
        peds.dispose();
        traffic.dispose();
        police.dispose();
        minimap.dispose();
        gunshot.dispose();
        cash.dispose();
        siren.dispose();
        audio.destroy();
        hud.dispose();
        resources.dispose();
    }
}
