---
name: game-spec
description: Write a concrete, engine-grounded design spec for a NEW game built on this LWJGL engine. Use when the user wants to design/scope/plan a game (e.g. "design a game", "spec out a <genre> game", "let's plan a new game on the engine"). Produces docs/specs/<slug>.md — a build-ready spec mapping the game to the engine's actual capabilities — then offers to implement it.
---

# Game spec generator (for this LWJGL engine)

Turn a game idea into a **build-ready specification** that only relies on what this engine can actually do (or clearly flags what must be added). The output is a markdown spec in `docs/specs/<slug>.md`.

## Procedure

### 1. Ground yourself in the engine
Read `ENGINE.md` (feature inventory + roadmap) and `CLAUDE.md` (architecture, conventions, asset/loading rules) before writing anything. The spec must reference the **real** building blocks below and never invent capabilities. If the game needs something not in this list, put it in the **Engine gaps** section with a rough effort estimate — don't pretend it exists.

**Engine building blocks (as of now):**
- **Core:** `Application` (fluent bootstrap) → `Engine` (fixed-timestep loop, scene switching with `1`–`9`/`0` and `[`/`]`) → `Scene` (`init/update/fixedUpdate/render/resize/dispose`). `Time`, `Log`, `GLDebug`, `Settings` (`.properties`).
- **Input:** `Input` (keys/mouse), `InputMap` (named actions, keyboard + gamepad), `Gamepad`.
- **Rendering:** `Mesh` (attribute layouts), `InstancedMesh` (one draw for many), `ShaderProgram` (`fromFiles` + hot-reload via `ShaderReloader`), `Material`, `Texture` (PNG/JPG), `Camera`. `Skybox`+`CubemapTexture`, `Framebuffer`+`PostProcessor` (post FX), `ShadowMap` (directional shadows), MSAA + distance fog. Lighting is **Phong** (`Light`: directional/point/spot) — no PBR.
- **Scene/content:** mini-ECS (`Entity`/`Component`/`World`, `MeshRenderer`, `LightComponent`), `Transform`, `GameObject`, `ResourceManager` (cache + central disposal), `Geometry`.
- **Models/world:** `OBJLoader`/`MtlLoader`/`Model` (**OBJ + MTL only — NOT glTF/FBX**), `Terrain`+`Noise` (procedural heightmap), `Road` (loop/spline ribbon), `Scatter` (instance placement).
- **Gameplay:** `CharacterController` (FP walk), `CarController` (arcade driving), `AABB`/`Ray`/`Intersect` (raycast/collision math), `Audio`+`Sound` (OpenAL WAV), `Hud` (stb_easy_font text).
- **Pipeline:** `SceneData`/`SceneSerializer`/`SceneBuilder` (JSON levels → ECS `World`).

**Hard constraints to respect in the spec:** OpenGL 3.3 core; models must be OBJ/MTL; textures PNG/JPG; audio 16-bit PCM WAV; text via stb_easy_font (no fancy fonts); one directional shadow map (no soft/point/cascaded shadows yet); no physics engine (only AABB/ray + ground-clamp controllers).

### 2. Pin down the concept
If the user already described the game, extract genre, core mechanic, camera/perspective, and scope from their message. Otherwise ask **2–4 focused questions** (use AskUserQuestion) covering: genre/reference, the single core mechanic, perspective (first-person / third-person / top-down), and scope (tiny prototype vs. bigger). Keep it short — one round.

### 3. Write the spec to `docs/specs/<slug>.md`
`<slug>` = kebab-case game name. Use exactly these sections:

1. **Pitch** — one or two sentences: what it is and why it's fun.
2. **Genre, perspective, target feel** — and the closest reference game.
3. **Core loop** — the 15-second gameplay cycle, then the session/progression loop.
4. **Controls** — keyboard + gamepad, as named actions (maps to `InputMap`).
5. **Game states & scenes** — each state as a `Scene` (menu / play / pause / game-over), how they register in `Main`/`Application`, and how switching works.
6. **World & content** — procedural (`Terrain`/`Road`/`Scatter`) vs. authored (`SceneData` JSON levels via `SceneBuilder`); size/layout.
7. **Entities & components** — the ECS entities and their components (reuse `MeshRenderer`/`LightComponent`, define new script components).
8. **Player & camera** — reuse `CharacterController`/`CarController` or spec a new controller; the camera behavior (chase, orbit, fixed).
9. **Rendering & look** — which engine features to use (lighting, skybox, fog, MSAA, instancing, post-FX, shadows) and the intended art direction (note: stylized low-poly is the sweet spot).
10. **Assets** — concrete list of models / textures / audio / HUD needs. For each, prefer **CC0** sources (Kenney for OBJ models: kenney.nl / OpenGameArt; ambientCG for textures; generate simple WAVs/textures procedurally). Note that every third-party asset needs a `CREDITS.txt` and must be OBJ/PNG/JPG/WAV-compatible.
11. **UI / HUD** — screens and on-screen info (`Hud` text; 2D via ortho).
12. **Audio** — SFX and music cues (`Sound`).
13. **Rules** — win/lose, scoring, timing, difficulty.
14. **Persistence** — settings (`Settings`) and saved data (best times/scores) via `SceneSerializer`/JSON or a properties file.
15. **Engine gaps** — anything the game needs that the engine lacks, each with a ★ effort estimate and whether it's required for the MVP or a stretch.
16. **Build plan** — a phased milestone list (MVP first: get *something playable*, then layers). Each phase names the engine classes/scenes/assets involved.
17. **Open questions** — decisions still needing the user.

### 4. Keep it real
- Favor **reusing** existing systems over inventing new ones; call out the exact classes.
- Scope the **MVP ruthlessly** — the first milestone should be the smallest playable thing.
- Every asset must be sourceable as CC0 and loader-compatible; if a needed model only exists as glTF/FBX, flag it (either find an OBJ, or add a glTF loader as an engine gap).

### 5. Wrap up
Print a short summary (pitch + MVP + top engine gaps) and the spec's path, then **offer to start building the MVP** (or to spawn subagents for the parallel parts). Do not start building until the user approves the spec.
