# Phase 6 — Missions & money

**Milestone:** starts v0.4 · **Status:** 🔨 built — awaiting playtest · **Pillar:** Progression

> **Built:** `Economy` (money), `Objective`/`Mission` (GOTO chain, AVAILABLE→ACTIVE→COMPLETE),
> `MissionManager` (3 courier missions from city nodes, enter-marker to start, pays reward on
> completion). `GtaScene`: gold start markers / green objective marker (emissive pillars), money +
> objective/distance HUD, "MISSION COMPLETE +$" flash. Works on foot and by car. Compiles, launches
> (exit 124).

A reason to play beyond chaos: **mission markers** in the city you activate to start an objective chain
(go here, then there), completing them for **money**. A simple economy + HUD.

## Player-visible result

- Glowing **mission markers** stand at a few intersections. Enter one (on foot or by car) to **start**
  a mission.
- The active mission shows the current **objective** (a labeled marker + on-HUD distance); reaching it
  advances to the next; finishing the chain pays a **cash reward** (brief "MISSION COMPLETE +$" flash).
- The HUD always shows your **money**.

## Engine / prior pieces reused

- **`AABB`/distance** triggers — marker entry + objective reached (simple radius checks).
- **`Hud`** — money, objective label + distance, prompts. **Marker rendering** reuses the prop cube +
  emissive material from the street furniture.
- The city's **road-node grid** to place markers/objectives on streets.
- Works with both on-foot and driving (uses the active entity position).

## New code (`scenes.gta`)

- **`Economy`** — money balance: `add`, `spend`, `money()`.
- **`Objective`** — a target position + radius + label; `reached(pos)`.
- **`Mission`** — name, reward, an ordered `Objective` list, and a state machine
  (`AVAILABLE → ACTIVE → COMPLETE`); `begin()`, `update(pos)` (advance/complete), `current()`.
- **`MissionManager`** — builds a few missions from city nodes, detects entering an available mission's
  start marker, drives the active mission, pays the reward on completion, and exposes what to render
  (start markers + current objective) and HUD state.
- **`GtaScene`** — owns `Economy` + `MissionManager`; updates with the active position; renders markers
  (gold = available start, green = current objective); money + objective HUD; reward flash.

## Assets

- **None new** — markers are emissive prop cubes; text via `Hud`.

## Rules

- Enter a start marker → mission ACTIVE. Reach each objective in order → next; last → COMPLETE + reward.
- MVP missions are simple "courier" chains (2 objectives). Completed missions stay done; others remain.

## Done when

- You can enter a marker, follow the objective chain, complete it, and get paid; money shows on the HUD.
  Builds, engine tests green, launches (exit 124).

## Risks / open questions

- **Scope** — keep mission types to simple GOTO chains for the MVP; ELIMINATE/DELIVER-vehicle variants
  are easy follow-ups on the same `Objective`/`Mission` structure.
- **Marker placement** — on road nodes so they're reachable by car; avoid the exact spawn.
- **No JSON authoring yet** — missions are defined in code; a `SceneSerializer`-style JSON mission
  format is a later nicety (noted, not required).
- **Money sinks** — nothing to spend on yet; spending (weapons/garages) is a future phase.

## Next

**Phase 7 — polish**: minimap (top-down + blips), day/night cycle, more audio, and a save (money/
progress) → **v0.4, a small GTA**.
