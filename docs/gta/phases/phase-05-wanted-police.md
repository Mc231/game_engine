# Phase 5 — Wanted level & police  → **v0.3**

**Milestone:** completes **v0.3** · **Status:** 🔨 built — awaiting playtest · **Pillar:** Combat/police

> **Built:** `WantedSystem` (heat → 0–5 stars, decay faster when unseen), `Police` (chase + shoot with
> `Vision` LoS, health/death), `PoliceManager` (spawn to match stars, reinforce, player-shoots-police,
> render). `GtaScene`: crimes (run-overs/shootings) add heat; police damage the player's `health`;
> slow regen when clean; **WASTED** respawn; wanted-stars HUD. `Avatar.police()` palette. Compiles,
> launches (exit 124). **v0.3 functionally complete.**

The GTA tension loop: commit crimes → a **wanted** star meter rises → **police** spawn, pursue, and
shoot at you (finally giving your health a purpose) → escape (heat decays out of sight) or get
**busted/wasted** and respawn. Reuses everything from Phases 3–4.

## Goal

Crime has consequences: an escalating police response you can survive, escape, or die to.

## Player-visible result

- Running over or shooting pedestrians raises a **wanted level** (0–5 stars, shown on the HUD).
- At ≥1 star, **police officers** appear and chase you (on foot and while driving); in line of sight
  and range they **shoot**, lowering your **health**.
- You can shoot police back (they have health and die). Out of sight and over time, the wanted level
  **decays** to zero and police leave.
- If your health hits zero you're **WASTED** — respawn at the start, wanted cleared, health restored.

## Engine / prior pieces reused

- **`WantedSystem`** style heat→stars (new, below) — pure and unit-testable.
- **`Weapon`** (Phase 4) — police carry one; the player's still works on them.
- **`Pedestrian`/`Avatar`** patterns — police are armed, aggressive avatars (`Avatar.police()` palette).
- **`Vision.canSee`** — police line-of-sight for shooting (blocked by buildings).
- **`Collide.slideXZ`** — police pathing around buildings. **`SpatialGrid`** — nearby wall queries.
- **`Audio`/`Sound`** — reuse the gunshot for police fire. **`Hud`** — stars + health.

## New code (`scenes.gta`)

- **`WantedSystem`** — accumulates **heat** from crimes (`addCrime(amount)`), decays it over time
  (faster when unseen), and maps heat → **stars** (0–5) via thresholds. `clear()` on bust. Pure logic
  (unit-testable; the `:game` module has no test source set yet, so no test committed).
- **`Police`** — an armed pursuer: moves toward the target (player/car), stops in range and **fires**
  (a `Weapon`) when it has line of sight, has **health** and a dead state (reuses the flat-avatar
  knockdown). `update(...)` returns the damage it dealt this frame.
- **`PoliceManager`** — spawns/despawns officers to match the star count (reinforcements as they die),
  updates them (accumulating player damage), a `shoot(...)` so the player can hit police, and render.
- **`GtaScene`** — owns `WantedSystem` + `PoliceManager`; reports crimes (run-overs, shootings) as
  heat; applies police damage to `health`; slow health **regen** when clean; **WASTED** respawn flow;
  HUD stars.

## Assets

- **None new** — police reuse the `Avatar` primitives (police palette) and the generated gunshot WAV.

## Rules

- **Crime → heat:** run over a ped (+heat), shoot a ped (+more). Thresholds set the star count.
- **Decay:** heat falls steadily when not committing crime; losing all stars ends the chase.
- **Police count** scales with stars. **Bust:** health 0 → respawn, wanted cleared, health full.

## Done when

- Crime raises stars; police spawn, chase, and shoot (damaging health); you can kill them; heat decays
  to lose them; zero health respawns you. Builds, engine tests green, launches (exit 124). **This is v0.3.**

## Risks / open questions

- **Difficulty balance** — police damage, fire rate, count per star, and decay rate all need tuning so
  it's tense but survivable. Keep them as constants.
- **Police pathing** is direct-chase + wall-slide (no navmesh) — fine for arcade.
- **Health regen** — simple regen when clean; no medkits yet.
- **Driving vs. on-foot** — police target the car when you're driving; drive-bys by the player are a
  later stretch.

## Next

**Phase 6 — missions & money** (mission triggers, objectives, rewards, economy), then **Phase 7 —
polish** (minimap, day/night, audio, save) → **v0.4, a small GTA**.
