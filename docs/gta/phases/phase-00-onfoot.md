# Phase 0 — Third-person on-foot *(foundation)*

**Milestone:** part of v0.1 · **Status:** 🔨 built — awaiting playtest · **Pillar:** foundation

> **Built:** `engine.OrbitCamera` (orbit + raycast wall pull-in, unit-tested),
> `scenes.city.Avatar` (animated humanoid, configurable palette),
> `scenes.city.ThirdPersonController` (camera-relative move + wall-slide + smooth facing),
> `scenes.city.GtaScene` (18th scene). Compiles, tests green, launches (exit 124). Feel-tuning pending your playtest.

The first brick. No car, no city yet — just a controllable low-poly avatar in third person in a
small walled test area. Everything else in the game hangs off getting this feel right.

## Goal

Control a character from a chase/orbit camera: walk and run around a flat, walled block; the
avatar animates (procedural walk/run) and faces its movement; the camera orbits with the mouse
and never clips through walls.

## Player-visible result

- Spawn standing in a small walled courtyard, seen from behind (third person).
- **Mouse** orbits the camera around the character (yaw + limited pitch).
- **WASD** moves relative to the camera (W = away from camera), **Shift** runs.
- The avatar turns to face its movement direction; legs/arms swing (walk), faster stride when running; idle when still.
- The character can't walk through the perimeter walls; the camera pulls in when a wall is behind it.
- A HUD line shows the mode (`ON FOOT`) and speed — placeholder for later.

## Engine pieces reused

- **`CharacterController`** — walk physics: horizontal velocity, gravity, ground-clamp, jump. Already
  pure-testable via the `Ground` interface; here the ground is a flat plane (`y = 0`).
- **`Collide.slideXZ(pos, radius, dx, dz, walls)`** — axis-separated wall collision (from Prison Break).
- **Procedural-walk humanoid** — the Prison Break guard's `renderGuard`/limb animation, generalized
  into a reusable `Avatar` (torso/head/arms/legs, `walkPhase`-driven swing + bob).
- **Mouse-orbit camera** — the chase camera math from `DrivingScene` (yaw/pitch → camera position
  around a target), adapted to orbit the character.
- **`InputMap`** — named actions (`forward/back/left/right/run`), keyboard now, gamepad later.
- **`Hud`** — the placeholder mode/speed text.

## New code (game module, `scenes/city/` package)

- **`GtaScene`** (working name) — the scene: owns the world, avatar, camera, input, HUD; the growth
  point for all later phases. Registered in `Main` as a new scene.
- **`ThirdPersonController`** — camera-relative movement. Reads move input + camera yaw → desired
  world-space move dir; drives `CharacterController`; sets the avatar's facing (smoothly turn toward
  move dir); tracks speed → feeds `Avatar.walkPhase` cadence (idle/walk/run).
- **`Avatar`** — the animated player model, generalized from `renderGuard`: `render(shader, pos, facing,
  moveSpeed, dt)`; internally advances `walkPhase` from speed so it works for the player AND later
  for pedestrians/police. Configurable palette (so peds/cops can reuse it with different colors).
- **`OrbitCamera`** (candidate for the **engine** module) — orbit around a target with yaw/pitch,
  distance, and a **raycast pull-in**: cast from target to the ideal camera spot against wall AABBs
  (`Ray`/`Intersect`); if blocked, shorten the distance so the camera sits in front of the wall.

## Assets

- **None new required** for P0 — the avatar is primitive boxes (reused guard geometry). Flat ground
  can use an existing texture (`textures/floor.jpg`) or a plain material.
- Optional stretch: swap the box avatar for a **Kenney low-poly character OBJ** later (P3+), but the
  procedural box avatar is the baseline (no skeletal animation in the engine).

## Controls (this phase)

| Action | Key | Notes |
|---|---|---|
| Move | W/A/S/D | camera-relative |
| Run | Left Shift (hold) | faster move + stride |
| Look / orbit | Mouse | yaw + clamped pitch |
| (Jump) | Space | optional; `CharacterController` supports it |

## Done when

- You can walk and run around the walled block in third person.
- The avatar animates (idle/walk/run) and faces its movement direction.
- Wall collision works (no walking through walls) and the camera never ends up behind a wall.
- Runs at stable FPS; scene `init()` builds all GPU resources (verify: make `GtaScene` first in `Main`,
  `timeout 12 ./gradlew run`, exit 124 = stayed open).

## Risks / open questions

- **Camera-relative movement + facing feel** is the make-or-break of third-person; expect tuning
  (turn speed, camera lag, pitch clamp, distance). Budget iteration here.
- **`OrbitCamera` — engine or game?** Lean engine (reusable, like our other camera code), but it may
  start in the scene and get promoted once stable.
- **Avatar generalization**: extract from the guard cleanly so Prison Break still works, OR copy the
  proven code into `Avatar` and leave the guard as-is. Decide when we open the file.
- Jump is optional for P0 — include only if cheap.

## Next

Phase 1 (`phase-01-vehicles.md`, to be written) adds the parked car + enter/exit state machine on
top of this scene, completing the **v0.1** traversal slice.
