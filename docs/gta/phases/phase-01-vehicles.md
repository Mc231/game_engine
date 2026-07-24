# Phase 1 — Vehicle entry + driving  → **v0.1**

**Milestone:** completes **v0.1** (traversal slice) · **Status:** 📝 spec written · **Pillar:** Driving

The payoff phase. On top of Phase 0's on-foot scene, drop a **parked car** you can walk
up to, **get in**, **drive**, and **get out of**. This is the on-foot ↔ vehicle core that *is*
GTA — once it feels seamless, v0.1 is done.

## Goal

Seamlessly switch between walking and driving: approach a parked car → prompt "[F] enter" →
press F to take the wheel (avatar hides, camera becomes a chase cam behind the car, `CarController`
drives) → press F to step out beside the car and walk away.

## Player-visible result

- A car sits parked in the test lot. Walking near it shows a prompt: **`[F] enter`**.
- Press **F** → you're driving: **W/S** throttle & reverse/brake, **A/D** steer, mouse orbits the
  chase camera behind the car; a speedometer replaces the on-foot readout.
- Press **F** again → the avatar reappears **beside the driver's door** and you're on foot again,
  the car left where you parked it.
- The camera behaves correctly in both modes (behind the character on foot; behind the car, aligned
  to its heading, when driving) and transitions smoothly between them.

## Engine pieces reused (almost everything already exists)

- **`CarController`** — arcade driving: `update(dt, throttle, steer, brake, Ground)`, plus
  `position()`, `heading()`, `speed()`, `forward()`, `setPosition`/`setHeading`/`setRideHeight`.
  Pure + unit-tested. Ground is a flat `y = 0` field here (same as Phase 0).
- **`Model`** — the CC0 Kenney car already in the repo: `Model.load("models/car/car.obj", litShader,
  resources)`, drawn with `uModel = carMatrix` (`translate(pos).rotateY(heading + MODEL_YAW_OFFSET)`,
  offset `0` — the car faces +Z = our forward). Exactly as `DrivingScene` uses it.
- **`OrbitCamera`** (Phase 0) — reused for the chase cam. Needs one small addition (below) so it can
  orbit *behind the car's heading* rather than a fixed world angle.
- **`Avatar`**, **`ThirdPersonController`**, **`GtaScene`** (Phase 0) — on-foot half is unchanged;
  the avatar is simply not drawn while driving.
- **`AABB`** — car proximity + (later) collider. **`Hud`** — prompt + speedometer. **`InputMap`** — actions.

## New code (game module, `scenes/city/`)

- **`Vehicle`** — wraps a `Model` + a `CarController` + local **seat** and **exit** offsets and a
  proximity radius. Helpers: `matrix()` (model transform), `worldExit()` (where the avatar spawns on
  exit — e.g. local `(-1.6, 0, 0)`, left of the car), `nearSeat(playerPos)` (proximity test),
  `update(dt, throttle, steer, brake)`. Keeps all car state in one place so multiple cars are trivial later.
- **`PlayerMode`** — small enum/state in `GtaScene`: `ON_FOOT` / `DRIVING` (start with an instant
  toggle; add a short blend later if it feels abrupt). Holds the active vehicle when driving.
- **`GtaScene` changes** — route input by mode: on foot, F near a vehicle → enter; driving, F → exit
  (place avatar at `worldExit()`, snap `ThirdPersonController` there and face away from the car).
  Feed the camera the right target (player vs car) and the right profile (distance/base-yaw) per mode.
  Always render the car; render the avatar only in `ON_FOOT`.

## Engine addition (small)

- **`OrbitCamera` base-yaw** ★ (trivial) — add an optional base angle so the chase cam can sit behind
  the car's heading: e.g. `update(target, dt, walls)` keeps current behavior (base 0), plus a way to
  set a per-frame base yaw (`setBaseYaw(car.heading() + PI)` while driving, `0` on foot). Mirrors
  `DrivingScene`'s `a = heading + PI + camYaw`. Also expose per-mode `setDistance` (≈6.5 on foot,
  ≈10 driving) and a `setTargetHeight` (≈1.2 for the car). Camera-relative move axes stay for on foot.

## Controls (this phase)

**On foot** (unchanged from Phase 0): WASD move · Shift run · mouse look · **F enter** (near car).
**Driving:**

| Action | Key | Notes |
|---|---|---|
| Throttle / reverse | W / S | W = forward, S = reverse (or brake when moving forward) |
| Steer | A / D | reuse `DrivingScene`'s steer-sign mapping (camera looks +Z) |
| Chase cam | Mouse | orbit around the car; recenters behind heading |
| Exit | F | step out beside the driver's door |

## Assets

- **None new.** Reuse `models/car/car.obj` + `colormap.png` (CC0 Kenney, already credited). The test
  lot reuses the Phase 0 floor/walls. (Later phases add more car models.)

## Done when

- You can walk to the parked car, press F to enter, drive it around the lot, press F to exit beside it,
  and walk away — with the camera correct and smooth in both modes. **This is v0.1.**
- Builds, engine tests green, scene launches and stays open (make `GtaScene` first in `Main`,
  `timeout 12 ./gradlew run`, exit 124).

## Risks / open questions

- **Chase-cam base-yaw** is the main new bit; get the "camera sits behind the car and recenters"
  feel right (copy `DrivingScene`'s proven math). Decide whether mouse-orbit *offset* persists or
  slowly recenters behind the car when driving straight.
- **Exit placement** must not drop the avatar inside a wall/the car. MVP: fixed left-door offset; if
  blocked, nudge to the first free side. Keep simple for the open test lot.
- **Car vs walls:** `CarController` only clamps to ground, not walls — in the open lot that's fine.
  **Building collision for cars is deferred to Phase 2** (city), where colliders exist; note it, don't
  solve it here.
- **Mode input routing:** ensure on-foot keys don't drive the car and vice-versa; F is edge-triggered
  (`isKeyPressed`) so one press = one toggle.
- **Two camera profiles:** on-foot distance/height vs driving distance/height — tune both.

## Next

With v0.1 done, **Phase 2** (`phase-02-city.md`) replaces the test lot with a procedural city — the
first big content system, likely split into sub-phases (road grid → blocks → buildings → colliders).
That's where car-vs-building collision and the spatial grid land.
