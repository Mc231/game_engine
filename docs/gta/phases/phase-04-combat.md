# Phase 4 — Combat & weapons

**Milestone:** starts v0.3 · **Status:** 🔨 built — awaiting playtest · **Pillar:** Combat

> **Built:** `scenes.gta.Weapon` (pistol: cooldown/ammo/damage/range) + `Pedestrian` health/damage
> (stagger → die → recycle) + `PedManager.shoot` (hitscan nearest live ped, blocked by buildings) +
> `engine.Input.isMouseButtonPressed/Down` (new). `GtaScene`: LMB fires along the camera aim, muzzle
> flash + gunshot `Sound` (`sounds/gunshot.wav`, generated), crosshair + health/ammo HUD. Compiles,
> tests green, launches (exit 124). Wanted-level consequence is Phase 5.

On-foot conflict: the player carries a pistol, aims with the camera, and fires a **hitscan** shot;
pedestrians take damage, react, and go down. Muzzle flash + sound sell it. Player has a health value
(no damage source until Phase 5's police — HUD placeholder for now).

## Goal

Point-and-shoot combat on foot that reuses the engine's ray/AABB math and the existing pedestrians.

## Player-visible result

- On foot, a **crosshair** shows; **left-click** fires the pistol along the camera's aim direction.
- A shot **hitscans** to the nearest pedestrian in line (blocked by buildings); a hit staggers them,
  enough hits **kill** (they drop and are cleared). A **muzzle flash** + **gunshot sound** fire each shot.
- The car run-over from before still works; guns are the on-foot version.
- HUD shows **health** and **ammo**.

## Engine pieces reused

- **`Ray` / `Intersect.rayAABB`** — hitscan: cast from the player's chest along the aim direction,
  find the nearest pedestrian AABB, blocked by nearer building AABBs (`SpatialGrid` for the candidates).
- **`OrbitCamera.forwardXZ`** — the aim direction (horizontal shot at chest height; good enough for
  ground targets).
- **`Input`** — new `isMouseButtonPressed` (added this phase) for the trigger.
- **`Pedestrian` DOWN state** (Phase 3.5) — reused for gunshot knockdown/kill reactions.
- **`Audio` / `Sound`** — the gunshot WAV (16-bit PCM). **`Hud`** — crosshair, health, ammo.

## New code (`scenes.gta`)

- **`Weapon`** — fire-rate cooldown, ammo, damage, range. `update(dt)` ticks the cooldown;
  `tryFire()` returns true when it may fire (ready + ammo) and consumes a round. Configurable so more
  weapon types drop in later.
- **`Pedestrian` health** — `takeDamage(amount, dir)`: reduces health, staggers on a non-lethal hit,
  and on death goes DOWN permanently (dead) then is recycled by `PedManager`.
- **`PedManager.shoot(origin, dir, range, damage, walls)`** — ray-picks the nearest live ped in the
  shot line (respecting building blockers) and applies damage; returns whether one was hit/killed.
- **`GtaScene`** — a `Weapon`; on the fire input, build the ray, call `peds.shoot(...)`, trigger a
  brief **muzzle flash** (a bright quad/cube at the muzzle for a few frames) + the gunshot `Sound`;
  render the crosshair; track player `health` (HUD only for now).

## Assets

- **`sounds/gunshot.wav`** — procedurally generated (self-authored short noise burst), like the prison
  SFX. No third-party assets.

## Controls (added)

| Action | Input | Notes |
|---|---|---|
| Fire | Left mouse | on foot; hitscan along camera aim |

## Done when

- On foot you can fire, the crosshair aims, peds react to being shot and die after enough damage, with
  muzzle flash + sound; HUD shows health + ammo. Builds, engine tests green, launches (exit 124).

## Risks / open questions

- **Aim model** — horizontal hitscan at chest height (ignore pitch) is simplest and reads fine for
  ground targets; full pitched aiming is a later refinement.
- **Ped hit AABB** — approximate each ped with a capsule-ish AABB for the ray test; keep generous so
  aiming feels forgiving.
- **Player health has no threat yet** — police (Phase 5) are the damage source; here it's a HUD value.
  Optionally let angered peds shove, but that's scope creep — defer.
- **Wanted consequence** — shooting/killing peds should raise a wanted level, but that's **Phase 5**;
  Phase 4 just does the shooting mechanic.

## Next

**Phase 5 — wanted level & police**: crimes (running over / shooting peds) raise stars; police pursue
and attack (reusing `Weapon`, ped health, `Vision`), and can damage the player's health from here.
