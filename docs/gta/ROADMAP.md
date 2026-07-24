# Grand Theft LWJGL — Roadmap

The phase ladder. Each phase is a **playable increment** with its own build-ready spec in
`phases/`. We write a phase's spec just before building it, so specs stay honest about the
code as it actually exists. Phases are ordered so the game is fun and runnable at every step.

**Milestone tags:** `v0.1` traversal slice · `v0.2` living city · `v0.3` conflict · `v0.4` game.

| Phase | Name | Pillar | Milestone | Status |
|------:|------|--------|-----------|--------|
| 0 | Third-person on-foot | (foundation) | v0.1 | 🔨 built — playtest |
| 1 | Vehicle entry + driving | Driving | **v0.1** | 📝 spec written |
| 2 | Procedural city | Open city | v0.2 | ⬜ |
| 3 | Street life (peds + traffic) | Open city + life | **v0.2** | ⬜ |
| 4 | Combat & weapons | Combat | v0.3 | ⬜ |
| 5 | Wanted level & police | Combat/police | **v0.3** | ⬜ |
| 6 | Missions & money | Progression | v0.4 | ⬜ |
| 7 | Polish (minimap, day/night, audio, save) | — | **v0.4** | ⬜ |

Status legend: ⬜ not started · 📝 spec written · 🔨 building · ✅ done.

---

## Phase 0 — Third-person on-foot *(foundation)*
**Goal:** control a low-poly avatar in third person in a small flat test block.
**Player sees:** their character from behind; mouse orbits the camera; WASD walks (relative to
camera), Shift runs; legs/arms animate (procedural walk/run); character can't walk through walls.
**Reuses:** `CharacterController` (physics), the guard humanoid + procedural walk, `DrivingScene`
mouse-orbit camera, `Collide.slideXZ`.
**New:** `ThirdPersonController` (camera-relative movement), `Avatar` (the animated player model,
generalized from the guard), a camera-collision pull-in (raycast so the camera doesn't clip walls).
**Engine gaps:** orbit-camera wall collision ★, avatar idle/walk/run pose blend ★.
**Done when:** you walk/run around a walled block in third person with an animated avatar and a
camera that stays outside walls.

## Phase 1 — Vehicle entry + driving  → **v0.1**
**Goal:** the traversal core — walk to a parked car, enter, drive, exit.
**Player sees:** a parked car; walk close → prompt "[F] enter"; press F → character hides, camera
snaps to chase-cam, you drive with `CarController`; press F → you step out beside the car.
**Reuses:** `CarController`, the car model, chase camera, `AABB` (proximity).
**New:** `PlayerMode` state machine (ON_FOOT ↔ DRIVING), `Vehicle` wrapper (model + controller +
seat/exit points), shared camera that switches target per mode.
**Engine gaps:** on-foot↔vehicle mode transition ★.
**Done when:** you can seamlessly get in, drive, and get out of a car. **This is v0.1.**

## Phase 2 — Procedural city
**Goal:** replace the test block with a real drivable/walkable city.
**Player sees:** a grid of streets with sidewalks, intersections, and low-poly buildings of
varied heights; solid collision on buildings and curbs.
**Reuses:** `Road`, `InstancedMesh`, `Scatter`, `Terrain` (flat/gentle), `Model`/OBJ (Kenney kits),
fog + skybox.
**New:** `CityGenerator` (grid → road ribbons + block plots + building placement), building collider
generation (AABB per building), spatial partition (grid) for fast collision/AI queries at scale.
**Engine gaps:** city generator ★★★, spatial broadphase grid ★★.
**Done when:** you can drive and walk a coherent multi-block city with proper collision and stable FPS.

## Phase 3 — Street life (pedestrians + traffic)  → **v0.2**
**Goal:** the city feels alive.
**Player sees:** pedestrians walking sidewalks (procedural-walk avatars) who flee/react to the
player and to gunfire/cars; cars driving the road network, stopping/steering around obstacles.
**Reuses:** `Avatar` + procedural walk, `Vision`, `Guard`-style patrol, `CarController` for AI cars,
`Road.centerline` for lanes, the spatial grid.
**New:** `Pedestrian` (wander/flee state machine), `TrafficCar` (follow lane, avoid, brake),
`Spawner` (populate around the player, despawn far away), crowd instancing for perf.
**Engine gaps:** ped/traffic AI ★★, population streaming ★★.
**Done when:** the city is populated with reactive peds and traffic at stable FPS. **This is v0.2.**

## Phase 4 — Combat & weapons
**Goal:** on-foot conflict.
**Player sees:** draw a weapon (pistol first), aim, fire (hitscan); peds/targets take damage, react,
and drop; muzzle flash + sound; player has health.
**Reuses:** `Ray`/`Intersect`/`AABB` (hitscan), `Hud` (ammo/health), `Audio`/`Sound`, `Avatar` (aim pose).
**New:** `Weapon` (fire rate, ammo, damage, hitscan), `Health` component, hit-reaction pose,
simple ped death (fall/despawn), aim-mode camera (over-shoulder).
**Engine gaps:** weapon/hitscan system ★, health & damage ★, hit-reaction pose ★.
**Done when:** you can draw, aim, and shoot; targets react and die; you can be hurt.

## Phase 5 — Wanted level & police  → **v0.3**
**Goal:** the GTA tension loop.
**Player sees:** committing crimes (hurting peds/cops, stealing) raises a star meter; police
avatars/cars pursue and attack; escaping line-of-sight + time lowers stars; busted → respawn.
**Reuses:** ped/traffic AI, `Vision`, `Weapon`, `Health`, `Hud` (stars).
**New:** `WantedSystem` (crime events → stars → response tiers), `Police` AI (pursue/attack/search),
busted/wasted flow.
**Engine gaps:** wanted-level system ★★, police pursuit AI ★★.
**Done when:** crime triggers escalating police response you can survive or escape. **This is v0.3.**

## Phase 6 — Missions & money
**Goal:** structured play and progression.
**Player sees:** mission-start markers; accept → objective chain (go here, drive there, deliver,
eliminate); completion pays money; money shown on HUD and (later) spent.
**Reuses:** JSON pipeline (`SceneData`/`SceneSerializer`) for mission definitions, `Hud`, triggers (AABB).
**New:** `Mission` (objective graph, state), `MissionManager` (triggers, activation, rewards),
`Economy` (money), objective markers/waypoints.
**Engine gaps:** mission scripting/objective graph ★★, JSON mission format ★.
**Done when:** you can accept, complete, and get paid for at least two distinct missions.

## Phase 7 — Polish  → **v0.4**
**Goal:** ship-feel.
**Player sees:** a minimap (top-down city + blips for player/mission/police), a coherent HUD
(health/wanted/money/weapon), day–night lighting, ambient + reactive audio, and a save that
persists money/progress.
**Reuses:** `Hud`, `Framebuffer` (minimap render), `Light` (day/night), `Audio`, `Settings` + JSON (save).
**New:** `Minimap` (2D top-down pass), `DayNightCycle` (animated directional light + skybox tint),
`SaveGame` (JSON), pause/menu scenes.
**Engine gaps:** minimap 2D pass ★★, day/night cycle ★, save system ★.
**Done when:** the game has a minimap, day/night, audio, and persistent progress. **This is v0.4 — a small GTA.**

---

## Cross-cutting engine gaps (reusable engine work, scheduled within phases)

These become **engine classes** (not game-only code) where they generalize:

- **Third-person orbit camera w/ collision** (P0) — camera raycast pull-in. → likely `OrbitCamera` in engine.
- **Avatar animation states** (P0/P3/P4) — procedural idle/walk/run/aim pose blends. → `Avatar` / pose helper.
- **Spatial broadphase grid** (P2) — uniform grid for collision + AI neighbor queries at city scale.
- **Population streaming** (P3) — spawn/despawn around the player.
- **Minimap / 2D top-down pass** (P7) — render city footprint + blips via ortho or a `Framebuffer`.
- **Save system** (P7) — JSON via existing serializer.

## Sequencing notes

- **v0.1 (Phases 0–1)** is the first "it feels like GTA" moment and validates the core. Build it first.
- Phase 2 (city) is the biggest content system — it may itself split into sub-phases (roads → blocks
  → buildings → collision) once we spec it.
- Combat (4) intentionally precedes police (5): police reuse the weapon/health systems.
- Keep each phase shippable; if a phase grows, split it rather than letting it sprawl.
