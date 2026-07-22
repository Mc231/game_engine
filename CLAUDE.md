# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A small 3D game engine built on **LWJGL 3.3.4** (OpenGL + GLFW) with **JOML** for math, plus a set of demo scenes that exercise it. Java 17, Gradle **multi-module**: the reusable engine is the `:engine` module (a `java-library`); the runnable game/demos are the `:game` module (an `application` that depends on `:engine`). The module boundary is compile-enforced — engine code cannot import game code.

## Build & run

```bash
./gradlew run             # build + launch (resolves to :game:run; starts on scene 1)
./gradlew build           # compile + assemble both modules
./gradlew :engine:compileJava   # fast compile-only check of the engine
./gradlew :game:compileJava     # fast compile-only check of the game
```

### Tests

```bash
./gradlew :engine:test    # JUnit 5 unit tests
```

Unit tests (in `:engine`) cover the **pure-logic** classes only — no OpenGL context needed: `OBJLoader.parse`, `Noise`/`Terrain` height math, `Transform`/`Camera` math, `WindowConfig`, `Light`, `Geometry`.

**GL-dependent classes** (`Mesh`, `ShaderProgram`, `Texture`, `ShadowMap`, `Engine`) need a live context and are intentionally **not** unit-tested — they're verified by running the app (see below). When adding testable logic, keep it separable from GL calls, following the existing split: `OBJLoader.parse(String)` returns data / `OBJLoader.load()` uploads it; `Terrain.heightAt(...)` is a pure static function.

### Verify by running

A successful launch that stays open means shaders compiled, resources loaded, and framebuffers are complete. Scenes create all their GPU resources in `init()`, so a broken scene throws there and exits immediately.

**Testing a specific scene from the CLI:** the app opens a GUI window and blocks until closed. To verify a scene non-interactively, temporarily make it the **first** entry in the scene list in `game/src/main/java/Main.java` (only the current scene's `init()` runs), then:
```bash
timeout 10 ./gradlew run --console=plain; echo "exit=$?"   # exit=124 (timed out = stayed open) means success
```
Restore the list order afterward.

### Running from the IDE (macOS)

Do **not** use the green-arrow "run `main()`" — on macOS it fails with exit 1 because GLFW requires the JVM flag `-XstartOnFirstThread` (Cocoa must own the first thread) and the IDE's synthetic launcher can't add it. Use the committed Gradle run configuration **`.run/run.run.xml`** ("Run LWJGL (Gradle)") which runs the `run` task and inherits `applicationDefaultJvmArgs`. The Gradle JVM is pinned to Java 17 in `.idea/gradle.xml`.

## Platform natives

`engine/build.gradle` auto-detects the OS/arch and selects the matching LWJGL native classifier (`natives-macos-arm64`, `natives-windows`, `natives-linux`, etc.), so no manual config is needed to build on a new machine. LWJGL modules in use: `lwjgl` (core), `-glfw`, `-opengl`, `-stb` (images + easy-font), `-openal` (audio), plus JOML. The Java bindings + JOML are `api` dependencies of `:engine` (so `:game` gets them on its compile classpath); the natives are `runtimeOnly` (they propagate to `:game`'s runtime classpath automatically).

## Architecture

Two Gradle modules:

- **`:engine`** (`engine/src/main/java/engine/`) — the reusable engine library. Depends on nothing in the game; the module boundary makes an engine→game import a compile error.
- **`:game`** (`game/src/main/java/`) — depends on `:engine`. Contains `Main` (default package; the entry point, which uses `Application.create()…scene(…)…run()` to configure the window and register scenes), the `scenes/` package (one class per demo, each implementing `engine.Scene`), and the resources.

### The core loop and scene model

`Engine` owns the `Window` and runs the frame loop. It holds a `List<Scene>` but **only the currently active scene is initialized** — switching disposes the old scene and inits the new one. Number keys `1`–`9` (and `0` for a 10th) select scenes; `[` / `]` cycle prev/next (for more than 10). The window title reflects the active scene.

`Scene` is the extension point: `init(Window)` once → per frame `fixedUpdate(step)` (0+ times) / `update(deltaSeconds)` / `render()` → `resize(w,h)` on framebuffer changes → `dispose()`. `fixedUpdate`, `resize`, and `name` are `default` no-ops, so most scenes only implement init/update/render/dispose. To add a scene, implement `Scene` in `scenes/` and register it via `Application.scene(...)` in `Main`.

The loop is **fixed-timestep**: `fixedUpdate` runs at a constant 60 Hz (accumulator), while `update`/`render` run once per frame — put deterministic simulation/physics in `fixedUpdate`, input/camera in `update`. Frame order in `Engine.loop()`: poll events → refresh input → scene-switch keys → dispatch `resize` on size change → `fixedUpdate` steps → `update` → viewport + clear → `render` → swap. `Time` tracks FPS (shown in the window title); `GLDebug.enable()` wires OpenGL debug output where supported.

### Rendering component model

- **`Mesh`** — VAO/VBO(+optional EBO). Constructed from a flat `float[]` plus an **attribute-layout array** (e.g. `{3, 3, 2}` = position, normal, uv). This layout drives everything; a scene changes its vertex format by changing that array, not the `Mesh` code. Uploads via `MemoryUtil.memAlloc*` (not `MemoryStack`) so it handles large meshes (terrain).
- **`ShaderProgram`** — compile/link with error checking; caches uniform locations; `setUniform` overloads for `int`/`float`/`Vector3f`/`Matrix4f`. Build from inline strings or `ShaderProgram.fromFiles("shaders/x.vert", "shaders/x.frag")`.
- **`Material`** — a shader + optional texture + params (tint, shininess, ambient/specular strength). `use()` binds them. Many materials can share one shader and differ only in params.
- **`GameObject`** — shared `Mesh` + `Material` + own `Transform`; `render()` applies the material and sets `uModel`.
- **`Transform`** — position + rotation (`Quaternionf`) + scale → `matrix()`.
- **`Camera`** — fly camera (yaw/pitch → `lookAt` view matrix); driven by `Input`.
- **`Light`** — one class, three types (directional/point/spot) via an enum; `apply(shader, "uLights[i]")` uploads into a GLSL light-struct array.
- **`Texture`** / **`OBJLoader`** / **`Terrain`** / **`Noise`** / **`ShadowMap`** — asset loading and generation (see `ENGINE.md`).

**Ownership rule:** `GameObject` and `Material` do **not** own (or dispose) their shared `Mesh`/`Texture`/`ShaderProgram` — the owning `Scene` disposes those in its `dispose()`. Anything holding GPU resources implements `engine.Disposable`. Only the active scene is disposed on shutdown (others were never initialized).

### Entity-component + resources (higher-level structure)

Beyond the direct `GameObject`+`Mesh` approach, the engine has a small ECS: `Entity` (a `Transform` + a list of components + parent/children), `Component` (lifecycle `update(dt)`; subclass it for "scripts"), and `World` (holds entities, `update(dt)` recurses, `collect(Type)` gathers components). Built-in components: `MeshRenderer` (mesh + material) and `LightComponent` (copies its entity's world position into the light). `Entity.worldMatrix()` composes parent × local for scene graphs. `EcsScene` is the reference usage — a scene still owns the render loop (it `collect`s renderers/lights and draws them). `ResourceManager` caches textures/shaders/meshes by key (loads once) and disposes them all in its own `dispose()` — a scene that uses it disposes the manager instead of the individual assets.

### Gameplay subsystems

- **`InputMap`** — binds named actions to keys; query with `isDown(action, input)` / `isPressed(action, input)`. Keeps key codes out of scene logic.
- **`CharacterController`** — first-person walk with gravity, jump, and ground-clamping. Physics is pure: ground height comes from a `Ground` functional interface (`Terrain::heightAt` in `WalkScene`), so it's unit-tested without GL.
- **`Audio` + `Sound`** — OpenAL (`lwjgl-openal`). `Audio` opens the device/context (construct once, `destroy()` on teardown); `Sound` loads a 16-bit PCM WAV into a buffer/source and `play()`s it.
- **`Hud`** — a 2D text overlay via stb_easy_font; quads are expanded to triangles for the core profile. `begin(w,h)` (framebuffer size) → `text(...)` → `end()`.
- `WalkScene` integrates all of the above walking on the `Terrain`.

### Advanced rendering

- **`Skybox`** (+ `CubemapTexture`) — a 6-face cubemap drawn with the camera's rotation but no translation (locked to the viewer); render it after opaque geometry.
- **`Framebuffer`** — render-to-texture target (color texture + depth renderbuffer). Bind it, draw the scene, `unbind(screenW, screenH)`, then sample its color texture.
- **`PostProcessor`** — a full-screen pass that samples a `Framebuffer` and applies an effect (`NONE`/`GRAYSCALE`/`INVERT`/`VIGNETTE`); the base for bloom/FXAA later.
- **`InstancedMesh`** — one mesh drawn many times via `glDrawElementsInstanced` with a per-instance `mat4` attribute at locations `base..base+3` (base = the count of the mesh's own attributes, e.g. 3 for `{3,3,2}`); the shader declares `layout(location=3) in mat4 aInstance`.
- `SkyboxScene` combines all three (instanced field under a sky, rendered through a post effect).

### Tooling / pipeline

- **Scene serialization** — levels are authored as JSON, not Java. `SceneData`/`EntityData`/`ComponentData` are the (flat, Gson-friendly) data model; `SceneSerializer` does JSON ↔ `SceneData` (+ `loadFromResource`/`saveToFile`); `SceneBuilder.build(sceneData, litShader, resources)` instantiates a live ECS `World`, pulling meshes/textures through the `ResourceManager` (so the scene disposes the manager, not individual assets). `SerializedScene` loads `game/src/main/resources/levels/demo.json`. Component descriptors are discriminated by a `type` string (`"mesh"` / `"light"`) with nullable fields — no Gson polymorphic adapters needed.
- **Settings** — `Settings.load("settings.properties")` reads resolution/vsync/mouse-sensitivity/volume (defaults for missing/malformed keys, never throws); `Main` applies width/height/vsync to the `Application`. `Settings.fromProperties(Properties)` is the unit-tested pure seam.
- The JSON library is **Gson** (`implementation` dep in `:engine` — internal, not exposed to `:game`).
- **Hot-reload** — `ShaderReloader(vertPath, fragPath)` loads a shader from **filesystem source files** (not the classpath) and `reloadIfChanged()`/`reload()` rebuilds it live, keeping the old program if the new one fails to compile. The Gradle `run` task's working dir is the `game/` module dir, so source paths are relative to that (`NormalMapScene.resolveShaderPath` tries both `game/` and repo-root bases).

### Gameplay math & input

- **`InputMap`** also binds **gamepad** buttons (`bindPad`); `isDown`/`isPressed` have `(action, Input, Gamepad)` overloads. **`Gamepad`** polls GLFW gamepad state (call `update()` each frame). `WalkScene` reads keyboard + controller.
- **`AABB` / `Ray` / `Intersect`** — pure collision math (ray-AABB slab test, AABB overlap, ray-plane); `PhysicsScene` raycasts from the camera to pick cubes.
- **Normal mapping** — `Geometry.cubeWithTangents()` (layout `{3,3,2,3}`) + `shaders/normalmap.*` (TBN, tangent-space normals). **Transparency** — alpha blend with `glDepthMask(false)` and back-to-front sorting (see `NormalMapScene`).

### The driving game (`DrivingScene`)

`DrivingScene` is a small game built on the engine: **`CarController`** (arcade driving physics — throttle/steer/brake, ground-clamped to a height field; pure + unit-tested, `forward = (sin h, 0, cos h)`, heading 0 → +Z) + **`Road`** (`loop` = oval, or `spline` = curvy Catmull-Rom track through waypoints; both conform to terrain height) + **`Scatter`** (random instance transforms over the terrain, with an exclude region to keep trees off the road) + a `Skybox` and an `InstancedMesh` forest + a mouse-orbit chase camera. Trees are a CC0 Kenney model whose per-material `Kd` colors are baked into per-vertex colors (`buildForest`) so the whole forest draws in one instanced call. It's a **time trial**: `Road.centerline(...)` gives the sampled track path; each frame the scene finds the nearest centerline point → distance beyond `OFF_TRACK` triggers `crash()` (respawn at the start, lap timer reset), and the nearest-point index drives lap detection (pass the halfway index, then return near index 0 → a completed lap, tracking the best time). The car is a **third-party CC0 model** (Kenney "Car Kit") at `game/src/main/resources/models/car/` (`car.obj` + `colormap.png`, see `CREDITS.txt`) — it loads through the existing `Model`/`MtlLoader` (standard `v/vt/vn` OBJ + `map_Kd` texture; the extra per-vertex color floats on `v` lines are ignored). Note `MODEL_YAW_OFFSET` in the scene flips the model 180° if a downloaded car faces the other way. New game assets/models go under `game/src/main/resources/`; keep third-party assets CC0/permissive and record provenance in a `CREDITS.txt`.

### Uniform-name conventions (contract between engine and shaders)

Engine classes set uniforms by fixed names that shaders must declare:
- `Material.use()` → `uTexture` (unit 0), `uTint`, `uAmbientStrength`, `uSpecularStrength`, `uShininess`.
- `GameObject.render()` → `uModel`.
- `Light.apply()` → `uLights[i].{type,position,direction,color,constant,linear,quadratic,cutOff,outerCutOff}`.
- Scenes set the frame-wide `uProjection`, `uView`, `uViewPos`, `uLightCount` on the active shader before the object loop.

Uniforms a given shader doesn't declare are silently ignored (location `-1`), which is why the same `Material` works across shaders with different uniform sets.

## Resources

Assets live in `game/src/main/resources` and load via the classpath:
- `shaders/*.vert|*.frag` — loaded by `ShaderProgram.fromFiles`.
- `textures/*.png` — loaded by `Texture` (stb_image; flips vertically, forces RGBA, builds mipmaps).
- `models/*.obj` (+ optional `.mtl`) — `OBJLoader.load` for a single merged mesh, or `Model.load(objPath, shader, resources)` for a **multi-material model** (one mesh+material per `usemtl`; colors/textures come from the `.mtl` via `MtlLoader`). `textures/white.png` is the default diffuse for untextured materials.

`textures/crate.png` and `models/sphere.obj` are generated by Python/PIL scripts, not hand-authored.

## Status & roadmap

`ENGINE.md` is the feature inventory (what's implemented) plus the prioritized roadmap toward a full game engine. Consult it before proposing large features so new work fits the intended direction.

## Gotchas

- **macOS threading:** GLFW requires `-XstartOnFirstThread` (handled in `build.gradle` for the `run` task). See the IDE note above.
- **Named packages can't import the default package.** `Geometry` lives in `engine` (not `scenes`) for this reason; keep shared helpers used by scenes in a named package.
- **HiDPI/retina:** viewport must use the *framebuffer* size (`Window.framebufferWidth/Height`), which can exceed the window size. `Engine` resets the viewport to framebuffer size each frame, so scenes that change it (shadows) recover automatically.
- **Adding a dependency** to `build.gradle` requires a Gradle re-sync in the IDE before it resolves.
