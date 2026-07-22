# Engine — Status & Roadmap

A lightweight 3D game engine in Java on top of **LWJGL 3** (OpenGL 3.3 core + GLFW) and **JOML**. This document tracks what exists today and what to build to make it a general-purpose engine for shipping games.

> Build/run and architecture details live in [`CLAUDE.md`](CLAUDE.md). This doc is the feature inventory + roadmap.

---

## 1. Current status

### ✅ Implemented

**Core / platform**
- Windowing & OpenGL 3.3 core context (`Window`, `WindowConfig` fluent builder).
- Cross-platform native detection (macOS arm64/x64, Windows, Linux).
- **Fixed-timestep** loop (`Scene.fixedUpdate` at 60 Hz) + variable per-frame update/render, depth testing, per-frame viewport reset (`Engine`).
- **Window resize handling** — framebuffer-size changes dispatched to `Scene.resize`.
- **Frame timing & FPS** (`Time`, shown in window title), leveled logging (`Log`), GL debug output (`GLDebug`).
- Fluent **`Application`** bootstrap — define a whole game (config + scenes) in one place.
- Runtime **scene switching** via number keys; only the active scene is initialized.
- **Input**: polled key state, edge-detected key presses, mouse-look deltas, mouse capture (`Input`).
- Explicit GPU-resource lifetime via `Disposable`.

**Rendering**
- `Mesh` — VAO/VBO/EBO from a flat array + attribute layout (`{3,3,2}` etc.); large-mesh safe.
- `ShaderProgram` — compile/link with error reporting, cached uniforms, inline or `fromFiles`.
- `Texture` — PNG/JPG via stb_image (vertical flip, RGBA, mipmaps).
- `Material` — shader + texture + surface params; many materials share one shader.
- `Transform` (pos/rot/scale) + `GameObject` (mesh + material + transform).
- `Camera` — first-person fly camera.

**Lighting**
- Phong shading (ambient + diffuse + specular).
- Multiple typed lights in one shader: **directional**, **point** (attenuation), **spot** (cone) via `Light`.
- **Directional shadow mapping** — depth FBO (`ShadowMap`), PCF soft edges, slope-scaled bias.

**World / assets**
- **OBJ model loader** (`OBJLoader`) — positions/uvs/normals, fan-triangulation, indexed output.
- **Procedural terrain** (`Terrain` + `Noise`) — Perlin/fBm heightmap, computed normals, elevation coloring, distance fog.
- Shaders/textures/models loaded from `src/main/resources` via the classpath.

### 🎬 Demo scenes (`scenes/`, switch with number keys)
1. `TriangleScene` — first triangle
2. `TexturedCubeScene` — textured cube
3. `CameraCubeScene` — fly camera through cubes
4. `LitCubeScene` — Phong lighting
5. `MaterialScene` — one shader, many materials
6. `LightsScene` — directional + point + spot (flashlight on `F`)
7. `ModelScene` — loaded .obj + file shaders
8. `ShadowScene` — directional shadows
9. `TerrainScene` — flyable procedural mountains

_(non-registered but present: `CubeScene`, `GameObjectScene`.)_

---

## 2. Class reference (`engine/`)

| Class | Responsibility |
|---|---|
| `Application` | Fluent bootstrap: config + scene registration → run |
| `Engine` | Owns the window, runs the fixed-timestep loop, manages/switches scenes |
| `Window`, `WindowConfig` | GLFW window + context + config |
| `Scene` | Content interface: init/update/fixedUpdate/render/resize/dispose(+name) |
| `Time` | Frame delta, elapsed, frame count, smoothed FPS |
| `Log`, `GLDebug` | Leveled logging + OpenGL debug output / error checks |
| `Input` | Keyboard/mouse polling, edge presses, mouse capture |
| `Camera` | Fly camera → view matrix |
| `Mesh` | Geometry buffers + draw |
| `ShaderProgram` | GLSL compile/link/uniforms |
| `Texture` | Image → GPU texture |
| `Material` | Shader + texture + surface params |
| `Transform`, `GameObject` | Placement + a renderable instance |
| `Light` | Directional/point/spot light data + upload |
| `ShadowMap` | Depth-only framebuffer for shadows |
| `OBJLoader` | .obj → `Mesh` |
| `Terrain`, `Noise` | Procedural heightmap terrain |
| `Geometry` | Static mesh data (cube, plane) |
| `Disposable` | GPU-resource cleanup contract |

---

## 3. Roadmap — from renderer to game engine

Grouped by area, roughly ordered by value. ★ = effort (1 easy → 4 hard).

### A. Engine foundation ✅ DONE
- ✅ **`Application` bootstrap** — fluent entry API (`Application.create().title(...).scene(...).run()`); a game is defined in one place. `Main` uses it.
- ✅ **Fixed-timestep update loop** — accumulator in `Engine`; `Scene.fixedUpdate(step)` runs at 60 Hz independent of frame rate; `update(dt)`/`render()` stay per-frame.
- ✅ **Window resize handling** — `Engine` dispatches framebuffer-size changes to `Scene.resize(w,h)`; all perspective scenes rebuild their projection aspect.
- ✅ **Time & frame stats** — `Time` (delta, elapsed, frameCount, smoothed fps); FPS shown in the window title.
- ✅ **Logging + GL debug** — `Log` (leveled) + `GLDebug` (debug-output callback where supported, `checkError` fallback for macOS's 4.1 context).

### B. Content & scene structure
- **★★ Entity/Component model (mini-ECS)** — components (Transform, MeshRenderer, Light, Script) on entities; a `World` that updates/renders them. This is the backbone of a real engine.
- **★★ Scene graph / parenting** — child transforms relative to parents (turrets on tanks, moons on planets).
- **★★ `Model` (multi-mesh) + material assignment** — OBJ files with groups/materials (.mtl), not just one mesh.
- **★★ Resource/asset manager** — cache and share textures/shaders/models by path; dispose centrally.

### C. Rendering upgrades
- **★★ Skybox / cubemap** — a real sky instead of flat fog color.
- **★★ Normal mapping** — surface detail from a texture (needs tangents in the vertex format).
- **★★ Transparency & blend ordering** — glass, foliage, particles.
- **★★★ Framebuffer/post-processing stack** — bloom, tone mapping, FXAA, color grading.
- **★★★ Point/spot shadows & multiple shadow casters** — cube-map shadows, cascaded shadow maps for large terrain.
- **★★★ Instanced rendering** — draw thousands of grass/trees/rocks efficiently.
- **★★★ Text rendering** — bitmap or SDF fonts (needed for UI/HUD/debug).

### D. Gameplay systems
- **★★ Input mapping / actions** — bind "jump"/"fire" to keys/mouse/gamepad; gamepad support via GLFW.
- **★★ Terrain collision & walking** — sample `Terrain.heightAt(x,z)` so the camera/player stands on the surface; character controller.
- **★★★ Physics & collision** — AABB/sphere broadphase, raycasting; or integrate a library.
- **★★ Audio** — sound effects + music via LWJGL's OpenAL (`lwjgl-openal`).
- **★★ UI/HUD layer** — 2D overlay rendering (orthographic), buttons/text; consider Nuklear (`lwjgl-nuklear`).

### E. Tooling & pipeline
- **★★ Scene serialization** — save/load scenes & entities (JSON), so levels aren't hardcoded in Java.
- **★★★ In-engine editor overlay** — inspect/tweak transforms, lights, materials at runtime (ImGui-style; LWJGL has bindings).
- **★★ Config/settings** — resolution, vsync, key bindings from a file.
- **★★★ Hot-reload** — reload shaders/textures on file change while running.

---

## 4. Suggested next milestones

A pragmatic order to reach "can build a small game":

1. ~~**Foundation:** fixed-timestep loop + resize handling + FPS/time (A).~~ ✅ **DONE**
2. **Structure:** mini-ECS + resource manager (B). Turns "hardcoded scenes" into "data-driven worlds." ← next
3. **Playable world:** terrain collision + input actions + a character controller (D). First real "game feel."
4. **Polish the look:** skybox + text/HUD + audio (C/D). Now it looks and sounds like a game.
5. **Pipeline:** scene serialization + settings (E). Author levels without recompiling.

After that, pick depth features (physics, post-processing, editor) based on the game being built.

---

## 5. Conventions to preserve

- Keep `engine/` free of any dependency on `scenes/` or a specific game.
- New shaders go in `resources/shaders/` and load via `ShaderProgram.fromFiles`.
- Honor the uniform-name contract (see `CLAUDE.md`) so `Material`/`GameObject`/`Light` stay shader-agnostic.
- Shared GPU resources are owned/disposed by the scene, not by `GameObject`/`Material`.
- Everything holding GPU state implements `Disposable`.
