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
- **Skybox** (`Skybox` + `CubemapTexture`), **post-processing** (`Framebuffer` + `PostProcessor`), **instanced rendering** (`InstancedMesh`), **HUD text** (`Hud`).

**Lighting**
- Phong shading (ambient + diffuse + specular).
- Multiple typed lights in one shader: **directional**, **point** (attenuation), **spot** (cone) via `Light`.
- **Directional shadow mapping** — depth FBO (`ShadowMap`), PCF soft edges, slope-scaled bias.

**World / assets**
- **OBJ model loader** (`OBJLoader`) — positions/uvs/normals, fan-triangulation, indexed output; `parseModel` splits by `usemtl`. **Multi-material `Model`** (`Model` + `MtlLoader`) — one mesh+material per group, .mtl colors/textures.
- **Procedural terrain** (`Terrain` + `Noise`) — Perlin/fBm heightmap, computed normals, elevation coloring, distance fog.
- Shaders/textures/models loaded from `src/main/resources` via the classpath.

**Content structure**
- **Mini-ECS**: `Entity` + `Component` + `World`, with `MeshRenderer`, `LightComponent`, and script components (override `Component.update`).
- **Scene graph / parenting**: `Entity` parent/children; `worldMatrix()` composes parent × local for multi-level hierarchies.
- **`ResourceManager`**: caches textures/shaders/meshes by key (loads once), owns and disposes them centrally.

**Gameplay**
- **Input actions** (`InputMap`): named actions → keys, `isDown`/`isPressed`.
- **Character controller** (`CharacterController`): first-person walk, gravity, jump, ground-clamp to a height field.
- **Audio** (`Audio` + `Sound`): OpenAL playback of WAV sound effects.
- **HUD** (`Hud`): 2D text overlay via stb_easy_font (core-profile triangles).
- Runtime scene cycling with `[` / `]` (beyond the 10 number keys).

### 🎬 Demo scenes (`scenes/`, switch with number keys `1`–`9`,`0`, or cycle with `[` / `]`)
1. `TriangleScene` — first triangle
2. `TexturedCubeScene` — textured cube
3. `CameraCubeScene` — fly camera through cubes
4. `LitCubeScene` — Phong lighting
5. `MaterialScene` — one shader, many materials
6. `LightsScene` — directional + point + spot (flashlight on `F`)
7. `ModelScene` — multi-material `.obj`/`.mtl` model
8. `ShadowScene` — directional shadows
9. `TerrainScene` — flyable procedural mountains
0. `EcsScene` — entity-component + scene graph
`[`/`]` → `WalkScene` — **walk the terrain** (gravity/jump, HUD, jump SFX)
`[`/`]` → `SkyboxScene` — skybox + instanced field + post-FX (`E` to cycle)

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
| `OBJLoader`, `MtlLoader`, `Model` | OBJ/MTL parsing + multi-material model |
| `Geometry` | Static mesh data (cube, plane) |
| `Entity`, `Component`, `World` | Mini-ECS: entities, components, and their container |
| `MeshRenderer`, `LightComponent` | Built-in components (draw a mesh / carry a light) |
| `ResourceManager` | Cache-by-key asset loading + central disposal |
| `InputMap` | Named action → key bindings over `Input` |
| `CharacterController` | First-person walk: gravity, jump, ground-clamp |
| `Audio`, `Sound` | OpenAL device/context + WAV playback |
| `Hud` | 2D text overlay (stb_easy_font) |
| `Skybox`, `CubemapTexture` | Cubemap sky locked to the camera |
| `Framebuffer`, `PostProcessor` | Render-to-texture + full-screen post effects |
| `InstancedMesh` | One mesh drawn many times (per-instance matrix) |
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

### B. Content & scene structure ✅ DONE
- ✅ **Entity/Component model (mini-ECS)** — `Component`/`Entity`/`World`; `MeshRenderer`, `LightComponent`, and script components (override `update`). Demoed by `EcsScene`.
- ✅ **Scene graph / parenting** — `Entity` parent/children; `worldMatrix()` composes parent × local (multi-level hierarchies: hub → cube → moon).
- ✅ **`Model` (multi-mesh) + material assignment** — `Model.load` splits an OBJ by `usemtl` into one mesh+material per group; `OBJLoader.parseModel` + `MtlLoader` read the .mtl (Kd/Ns/map_Kd). Demoed by `ModelScene` (3 colored cubes from one file).
- ✅ **Resource/asset manager** — `ResourceManager` caches textures/shaders/meshes by key, loads once, disposes all centrally.

### C. Rendering upgrades (partly ✅ DONE)
- ✅ **Skybox / cubemap** — `CubemapTexture` + `Skybox` (6-face cubemap, locked to the camera).
- ✅ **Framebuffer / post-processing** — `Framebuffer` (render-to-texture) + `PostProcessor` (grayscale/invert/vignette; the base for bloom/FXAA later).
- ✅ **Instanced rendering** — `InstancedMesh` draws many objects in one call (per-instance mat4 attribute).
- ✅ **Text rendering** — `Hud` via stb_easy_font (delivered in D).
- **★★ Normal mapping** — surface detail from a texture (needs tangents in the vertex format). *(to do)*
- **★★ Transparency & blend ordering** — glass, foliage, particles. *(to do)*
- **★★★ Advanced shadows** — point/spot cube-map shadows, cascaded shadow maps. *(to do; directional shadows exist)*

### D. Gameplay systems (mostly ✅ DONE)
- ✅ **Input mapping / actions** — `InputMap` binds named actions to keys (`isDown`/`isPressed`). *(gamepad not yet)*
- ✅ **Terrain collision & walking** — `CharacterController` (gravity, jump, ground-clamp to a height field); `WalkScene` walks the terrain via `Terrain.heightAt`.
- **★★★ Physics & collision** — general AABB/sphere broadphase + raycasting. *(still to do — only terrain ground collision exists)*
- ✅ **Audio** — `Audio` (OpenAL device/context) + `Sound` (WAV → buffer/source); `WalkScene` plays a jump SFX.
- ✅ **UI/HUD layer** — `Hud` 2D text overlay (stb_easy_font). *(no widgets/buttons yet)*

### E. Tooling & pipeline
- **★★ Scene serialization** — save/load scenes & entities (JSON), so levels aren't hardcoded in Java.
- **★★★ In-engine editor overlay** — inspect/tweak transforms, lights, materials at runtime (ImGui-style; LWJGL has bindings).
- **★★ Config/settings** — resolution, vsync, key bindings from a file.
- **★★★ Hot-reload** — reload shaders/textures on file change while running.

---

## 4. Suggested next milestones

A pragmatic order to reach "can build a small game":

1. ~~**Foundation:** fixed-timestep loop + resize handling + FPS/time (A).~~ ✅ **DONE**
2. ~~**Structure:** mini-ECS + resource manager + multi-material models (B).~~ ✅ **DONE**
3. ~~**Playable world:** terrain collision + input actions + a character controller (D).~~ ✅ **DONE** (`WalkScene`)
4. ~~**Polish the look:** skybox + post-processing + instancing (C).~~ ✅ **DONE** (`SkyboxScene`; normal maps still open)
5. **Pipeline:** scene serialization + settings (E). Author levels without recompiling. ← next

After that, pick depth features (physics, post-processing, editor) based on the game being built.

---

## 5. Conventions to preserve

- Keep `engine/` free of any dependency on `scenes/` or a specific game.
- New shaders go in `resources/shaders/` and load via `ShaderProgram.fromFiles`.
- Honor the uniform-name contract (see `CLAUDE.md`) so `Material`/`GameObject`/`Light` stay shader-agnostic.
- Shared GPU resources are owned/disposed by the scene, not by `GameObject`/`Material`.
- Everything holding GPU state implements `Disposable`.
