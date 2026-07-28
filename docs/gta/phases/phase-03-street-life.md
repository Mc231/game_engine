# Phase 3 — Street life (pedestrians + traffic)  → **v0.2**

**Milestone:** completes **v0.2** · **Status:** 🔨 built (3a+3b) — **v0.2** functionally complete, awaiting playtest · **Pillar:** Open city + street life

> **3a built:** `scenes.gta.Pedestrian` (wander ↔ flee state machine, slides off buildings, own
> walk phase) + `PedManager` (pool of 24 around the player, spawn on free ground, recycle when far).
> Peds flee the on-foot player and moving car.
>
> **3b built:** `scenes.gta.TrafficCar` (drives the road-grid node graph via `CarController`, steers
> toward the next intersection, picks a non-reversing neighbour on arrival, pushes out of buildings) +
> `TrafficManager` (fleet of 8 sharing one car `Model`). `City` now exposes the road-intersection node
> grid. Compiles, launches (exit 124). **v0.2 functionally complete** — feel/AI tuning pending playtest.

Make the city feel alive: **pedestrians** wandering the streets who react to the player and the
car, and **traffic** driving the road grid. Reuses the animated `Avatar`, `Vision`, `CarController`,
and Phase 2's `SpatialGrid` for cheap neighbor queries.

## Goal

Populate the city with reactive pedestrians and moving traffic that stay performant by only
simulating what's near the player (spawn around, despawn far away).

## Player-visible result

- Pedestrians (varied colors, procedural walk) wander sidewalks/streets; when the player or car gets
  close they **flee**; they don't walk through buildings.
- Cars drive along the streets, following the grid, braking to avoid what's ahead.
- Population appears around the player and thins out in the distance — stable FPS.

## Sub-phases (each runnable)

- **3a — Pedestrians.** `Pedestrian` (wander ↔ flee) + a `PedManager`/`Spawner` populating around
  the player; peds collide with buildings and react to player/car. *Done when:* the streets have
  reactive walkers. **← building now.**
- **3b — Traffic.** `TrafficCar` (follow a grid lane path via `CarController`, brake/avoid) + spawn
  on the road network. *Done when:* cars drive the grid and react to obstacles. **This is the v0.2 bar.**
  *(Also done: hazard braking — cars stop for pedestrians and other cars — and car↔car separation so
  vehicles never overlap. Counts raised to 40 peds / 14 cars.)*
- **3c — Traffic lights (built).** `TrafficLights` — a global signal cycle (N–S green → amber →
  E–W green → amber). A `TrafficCar` approaching its target intersection brakes at the stop line
  when its road's signal isn't green, and resumes on green (reuses the hazard-brake path). Rendered
  as colored signal heads on posts at every intersection. *(Ped-waits-at-red is a later stretch.)*
- **2d — Street dressing (built).** Distance fog on both the lit and city shaders (buildings fade
  to a horizon color that matches the sky), signal/lamp **posts** at intersections, and decorative
  **street lamps** (warm emissive heads) along the road edges. Adds depth + vertical street furniture.

## Engine pieces reused

- **`Avatar`** — the animated humanoid (per-instance walk); pedestrians are Avatars with varied palettes.
- **`Vision`** — cone + line-of-sight (already used by the Prison guard) → later cop/ped awareness.
- **`SpatialGrid`** (Phase 2) — building colliders for ped/car collision + (later) neighbor queries.
- **`Collide.slideXZ` / `resolveCircle`** — peds slide along buildings; traffic cars push out.
- **`CarController`** — drives each `TrafficCar` (same arcade physics as the player's car).
- The city's road grid gives lane centers for traffic paths.

## New code (`scenes.gta`)

- **`Pedestrian`** — state machine `WANDER` (walk to a roaming target, pick a new one on arrival) /
  `FLEE` (move directly away from the nearest threat within a radius; car is scarier than the player).
  Owns position/facing/speed + an `Avatar`; slides against nearby building colliders.
- **`PedManager`** (a.k.a. Spawner) — maintains a pool of peds around the player: spawns on free ground
  near the player, despawns/recycles ones that get too far (population streaming). Updates + renders all.
- **`TrafficCar`** (3b) — follows a lane path (grid centerlines), throttles toward the next node,
  brakes when blocked; `CarController` + `resolveCircle`.
- **`TrafficManager`** (3b) — spawns/recycles traffic on the road network around the player.
- **`GtaScene`** — owns the `PedManager` (and later `TrafficManager`); updates them with the player/car
  positions and the city, renders them with the lit shader after the player avatar.

## Assets

- **None new.** Pedestrians are `Avatar` primitives with randomized palettes; traffic reuses the CC0
  car `Model`. (Distinct CC0 ped/car models are a later upgrade.)

## Done when (v0.2 bar = 3b)

- Reactive pedestrians on the streets **and** traffic driving the grid, both spawning around the
  player and staying performant. Builds, engine tests green, launches (exit 124). **This is v0.2.**

## Risks / open questions

- **Perf/scale** — cap active peds/cars; only simulate near the player; recycle far ones. Log counts.
- **Avatar cost** — each ped currently owns a small mesh + materials; fine for a few dozen. If it
  grows, share one mesh across avatars (a later optimization, noted).
- **Ped pathing** is intentionally dumb (wander + flee, slide off buildings) — no navmesh. Good enough
  for background life; smarter routing is a stretch.
- **Traffic on a grid** — keep lanes simple (drive to the next intersection, turn or continue); full
  traffic rules (lights, right-of-way) are out of scope.
- **Reaction fairness** — flee radius/speed tuned so peds scatter believably without teleporting.

## Next

**Phase 4 — combat & weapons** (hitscan via `Ray`/`Intersect`, `Avatar` aim pose, ped health + hit
reactions), which then feeds **Phase 5 — wanted level & police**.
