# Phase 2 — Procedural city  → **v0.2** (part 1)

**Milestone:** starts v0.2 · **Status:** 📝 spec written · **Pillar:** Open city

Replace the flat test lot with a real **drivable, walkable city**: a grid of streets and
sidewalks lined with low-poly buildings you collide with. This is the biggest content system in
the project, so it's split into **sub-phases** that each stay runnable. Street *life* (peds,
traffic) is Phase 3 — this phase is the static city + collision.

## Goal

A coherent multi-block city you can drive and walk through: straight streets in a grid, raised
sidewalks, and buildings of varied height that both the car and the character collide with, all at
stable FPS.

## Player-visible result

- Drive/walk out of a starting spot into a grid of streets with intersections and sidewalks.
- Blocks are filled with low-poly buildings of varied heights/colors; you **can't** drive or walk
  through them (the car stops/scrapes along walls instead of passing through).
- The chase/orbit camera pulls in against nearby buildings (it already does — now fed city colliders).
- Stable frame rate with a few hundred buildings (instancing + a spatial grid keep it cheap).

## Design: grid-based city

A rectangular **grid** is the simplest, most readable GTA-III-style layout and maps cleanly to our
primitives:

- **Streets:** flat road strips running along X and Z between blocks (asphalt texture). A grid of
  `N×M` blocks with `roadWidth` gaps. Straight segments + intersection squares — no curves needed
  (our `Road` ribbon is for the countryside track; a grid is simpler as flat textured quads).
- **Sidewalks:** slightly raised concrete strips bordering each block (curb height ~0.15).
- **Blocks:** the squares between streets. Each block is subdivided into a few building **plots**.
- **Buildings:** box buildings — unit cubes scaled per instance to varied footprints/heights, tinted
  for variety, drawn in **one instanced call** (like the Prison Break bars / the forest). Kenney City
  Kit OBJ buildings are an upgrade path once boxes work.
- **Ground:** flat `y = 0` (a city is flat). Terrain/hills are out of scope here.

## Sub-phases (each runnable)

- **2a — Streets & sidewalks.** `CityGenerator` emits the road grid + sidewalk strips + ground as
  meshes; drive/walk on it (no buildings yet). *Done when:* you can roam an empty gridded city.
- **2b — Buildings.** Fill plots with instanced box buildings (varied size/height/tint) + one AABB
  collider each. Rendered, not yet colliding. *Done when:* the city looks like a city.
- **2c — Collision.** Feed building AABBs through a **spatial grid** to on-foot `Collide`, the new
  **car collision**, and the camera pull-in. *Done when:* car and character both stop at buildings
  and the camera behaves. **This is the Phase 2 bar → v0.2 part 1.**
- **2d — Dressing (optional).** Street props (lights, benches, hydrants) via `Scatter` on sidewalks;
  asphalt/concrete/building textures + normal maps; skybox + light fog for depth.

## Engine pieces reused

- **`InstancedMesh`** — one draw call for all box buildings (per-instance `mat4`, per-instance tint if
  we extend it, else a few instanced batches by tint). Same pattern as the forest / prison bars.
- **`Model`** (later) — Kenney City Kit OBJ buildings as the upgrade over boxes.
- **`AABB`** / **`Collide.slideXZ`** — on-foot building collision (already used in `GtaScene`/Prison).
- **`OrbitCamera`** — camera wall pull-in already implemented; just needs the city's nearby colliders.
- **`Scatter`** — street-prop placement (2d). **`Texture`** — asphalt/concrete/building (ambientCG/CC0).
- **`Skybox`** + **fog** (`lit.frag` `uFogDensity`) — horizon/depth (2d).

## New code

- **`engine.SpatialGrid`** ★★ — a uniform grid bucketing `AABB`s by cell, with `nearby(x, z, radius)`
  / `nearby(AABB)` returning only candidate colliders in adjacent cells. A city has hundreds of
  buildings; brute-force collision/camera checks every frame don't scale. Reusable engine class,
  **unit-testable** (pure math — insert boxes, assert query returns the right neighbors).
- **Car collision** ★★ — `CarController` only ground-clamps. Add a resolve step: treat the car as a
  circle/short capsule (radius ~1.2) and push it out of overlapping building AABBs after `update`,
  killing the into-wall velocity component. Best as a reusable helper, e.g.
  `engine.Collide.resolveCircle(pos, radius, walls)` (push position out of penetration), usable for
  the car AND as a sturdier on-foot resolve. `Vehicle` calls it each frame; feeds pos back via
  `CarController.setPosition`.
- **`scenes.city.CityGenerator`** ★★★ — the generator: config (blocks X/Z, block size, road width,
  plots per block, height range, seed) → a `City`. Deterministic from the seed (no `Date.now`/random
  seeded explicitly, matching engine conventions). Produces: ground/road/sidewalk meshes, the building
  `InstancedMesh`(es), a `List<AABB>` of colliders, and spawn points.
- **`scenes.city.City`** — holds the generated content; `render(shader)`, `colliders()`, the
  `SpatialGrid`, and helpers (nearest road, spawn points). `GtaScene` builds a `City` in `init` and
  swaps the hand-built test block for it; walls come from `city.spatialGrid().nearby(playerPos)`.

## Assets

- **Textures (CC0, ambientCG):** asphalt (road), concrete (sidewalk), 1–2 building facade textures
  (+ normal maps optional). PNG/JPG. Add to `CREDITS.txt`.
- **Models (optional, CC0 Kenney City Kit):** OBJ buildings for the 2b upgrade — only if boxes feel
  too plain. Box buildings need **no** new models.

## Done when (Phase 2 bar = sub-phase 2c)

- You can drive and walk a coherent multi-block city; buildings block both the car and the character;
  the camera pulls in against them; FPS stays stable (spatial grid working).
- Builds, engine tests green (incl. `SpatialGridTest` + car-collision math), scene launches (exit 124).

## Risks / open questions

- **Perf** is the headline risk: keep buildings in as few instanced batches as possible; only query
  nearby colliders via the grid; don't rebuild meshes per frame. Log building count.
- **Car collision feel** — arcade push-out can feel sticky or bouncy; tune radius + how much velocity
  is killed. It only needs to feel "you can't drive through walls," not simulate crashes.
- **Instanced per-instance tint** — either extend `InstancedMesh` to carry a per-instance color, or
  batch buildings into a handful of `InstancedMesh`es by palette. Decide when we open the file.
- **Grid vs. more organic layout** — grid first (simple, readable). Curved/varied districts are a
  later stretch, not now.
- **Kenney kit vs. boxes** — start with boxes (reliable, one draw). Promote to OBJ kit only if needed.
- **City size** — start small (e.g. 4×4 blocks) and scale up once perf is proven.

## Next

**Phase 3** (`phase-03-street-life.md`) populates the city with pedestrians and traffic (reusing
`Avatar`, `Vision`, `CarController`, and this phase's `SpatialGrid` for neighbor queries) — reaching
**v0.2**. Combat (Phase 4) follows.
