# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

A small 3D game engine built on **LWJGL 3.3.4** (OpenGL + GLFW) with **JOML** for math, plus a set of demo scenes that exercise it. Java 17, Gradle. The engine lives in the `engine` package; runnable demos live in `scenes`.

## Build & run

```bash
./gradlew run          # build + launch (starts on scene 1)
./gradlew build        # compile + assemble
./gradlew compileJava  # fast compile-only check
```

There are **no tests** in this project. Verification is done by running the app: a successful launch that stays open means shaders compiled, resources loaded, and framebuffers are complete. Scenes create all their GPU resources in `init()`, so a broken scene throws there and exits immediately.

**Testing a specific scene from the CLI:** the app opens a GUI window and blocks until closed. To verify a scene non-interactively, temporarily make it the **first** entry in the scene list in `src/main/java/Main.java` (only the current scene's `init()` runs), then:
```bash
timeout 10 ./gradlew run --console=plain; echo "exit=$?"   # exit=124 (timed out = stayed open) means success
```
Restore the list order afterward.

### Running from the IDE (macOS)

Do **not** use the green-arrow "run `main()`" — on macOS it fails with exit 1 because GLFW requires the JVM flag `-XstartOnFirstThread` (Cocoa must own the first thread) and the IDE's synthetic launcher can't add it. Use the committed Gradle run configuration **`.run/run.run.xml`** ("Run LWJGL (Gradle)") which runs the `run` task and inherits `applicationDefaultJvmArgs`. The Gradle JVM is pinned to Java 17 in `.idea/gradle.xml`.

## Platform natives

`build.gradle` auto-detects the OS/arch and selects the matching LWJGL native classifier (`natives-macos-arm64`, `natives-windows`, `natives-linux`, etc.), so no manual config is needed to build on a new machine.

## Architecture

Three source areas under `src/main/java`:

- **`engine/`** — the reusable engine. No dependencies on `scenes` or `Main`.
- **`scenes/`** — one class per demo, each implementing `engine.Scene`. This is the "content."
- **`Main.java`** (default package) — the only entry point; builds a `WindowConfig` and hands the `Engine` an ordered `List<Scene>`.

### The core loop and scene model

`Engine` owns the `Window` and runs the frame loop. It holds a `List<Scene>` but **only the currently active scene is initialized** — switching disposes the old scene and inits the new one. Number keys `1`–`9` (and `0` for a 10th) switch scenes at runtime; the window title reflects the active scene.

`Scene` is the extension point: `init(Window)` → `update(float deltaSeconds)` / `render()` each frame → `dispose()`. To add a demo, implement `Scene` in `scenes/` and add one line to the list in `Main`.

Frame order in `Engine.loop()`: poll events → refresh input → handle scene-switch keys → reset viewport to the framebuffer size → clear → `update` → `render` → swap.

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

### Uniform-name conventions (contract between engine and shaders)

Engine classes set uniforms by fixed names that shaders must declare:
- `Material.use()` → `uTexture` (unit 0), `uTint`, `uAmbientStrength`, `uSpecularStrength`, `uShininess`.
- `GameObject.render()` → `uModel`.
- `Light.apply()` → `uLights[i].{type,position,direction,color,constant,linear,quadratic,cutOff,outerCutOff}`.
- Scenes set the frame-wide `uProjection`, `uView`, `uViewPos`, `uLightCount` on the active shader before the object loop.

Uniforms a given shader doesn't declare are silently ignored (location `-1`), which is why the same `Material` works across shaders with different uniform sets.

## Resources

Assets live in `src/main/resources` and load via the classpath:
- `shaders/*.vert|*.frag` — loaded by `ShaderProgram.fromFiles`.
- `textures/*.png` — loaded by `Texture` (stb_image; flips vertically, forces RGBA, builds mipmaps).
- `models/*.obj` — loaded by `OBJLoader` (v/vt/vn/f, fan-triangulated, indexed).

`textures/crate.png` and `models/sphere.obj` are generated by Python/PIL scripts, not hand-authored.

## Status & roadmap

`ENGINE.md` is the feature inventory (what's implemented) plus the prioritized roadmap toward a full game engine. Consult it before proposing large features so new work fits the intended direction.

## Gotchas

- **macOS threading:** GLFW requires `-XstartOnFirstThread` (handled in `build.gradle` for the `run` task). See the IDE note above.
- **Named packages can't import the default package.** `Geometry` lives in `engine` (not `scenes`) for this reason; keep shared helpers used by scenes in a named package.
- **HiDPI/retina:** viewport must use the *framebuffer* size (`Window.framebufferWidth/Height`), which can exceed the window size. `Engine` resets the viewport to framebuffer size each frame, so scenes that change it (shadows) recover automatically.
- **Adding a dependency** to `build.gradle` requires a Gradle re-sync in the IDE before it resolves.
