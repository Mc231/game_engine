# Engine — Status & Roadmap

A lightweight 3D game engine in Java on top of **LWJGL 3** (OpenGL 3.3 core + GLFW) and **JOML**. This document tracks what exists today and what to build to make it a general-purpose engine for shipping games.

> Build/run and architecture details live in [`CLAUDE.md`](CLAUDE.md). This doc is the feature inventory + roadmap.

---

## 1. Current status

### ✅ Implemented

**Core / platform**
- Windowing & OpenGL 3.3 core context (`Window`, `WindowConfig` fluent builder).
- Cross-platform native detection in `build.gradle` (macOS arm64/x64, Windows, Linux).
- Main loop with delta time, depth testing, per-frame viewport reset (`Engine`).
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
| `Engine` | Owns the window, runs the loop, manages/switches scenes |
| `Window`, `WindowConfig` | GLFW window + context + config |
| `Scene` | Content interface: init/update/render/dispose(+name) |
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

### A. Engine foundation (do these first — everything else builds on them)
- **★★ Scene/game separation & `Application` bootstrap** — a clean entry API so a *game* is defined without touching `Main` (register scenes, set config in one place).
- **★★ Fixed-timestep update loop** — decouple physics/logic from render rate (accumulator pattern); keeps simulation deterministic.
- **★ Window resize handling** — framebuffer-size callback → update viewport + projection aspect (currently fixed at init).
- **★★ Time & frame stats** — `Time` (delta, elapsed, fps) + on-screen or title FPS.
- **★★ Logging + GL debug callback** — surface GL errors immediately instead of silent failures.

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

1. **Foundation:** fixed-timestep loop + resize handling + FPS/time (A). Small, unblocks everything.
2. **Structure:** mini-ECS + resource manager (B). Turns "hardcoded scenes" into "data-driven worlds."
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
