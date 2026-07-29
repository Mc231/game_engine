# Phase 7 — Polish (minimap, day/night, audio, save)  → **v0.4**

**Milestone:** completes **v0.4 — a small GTA** · **Status:** 🔨 built — awaiting playtest · **Pillar:** —

> **Built:** `DayNightCycle` (moving sun → light/ambient/sky, feeds both shaders + clear color),
> `Minimap` (2D ortho pass: panel, nearby buildings, player/police/mission blips), `SaveGame`
> (money + completed missions in `gta-save.properties`, loaded on init / saved on payout + exit),
> `sounds/cash.wav` + `sounds/siren.wav` (cash on payout, siren while wanted). Compiles, launches
> (exit 124). **v0.4 — a small GTA — reached.**

The finale: the touches that make it feel like a game rather than a tech demo — a **minimap**, a
**day/night cycle**, more **audio**, and a **save** so money/progress persists.

## Player-visible result

- A **minimap** (corner) shows nearby buildings top-down with **blips**: player (center), police (red),
  mission start (gold) / active objective (green).
- A **day/night cycle** slowly rotates the sun: warm dawn/dusk, bright noon, dim blue night — lighting,
  fog, and sky all shift together.
- **Audio**: a cash chime on mission payout and a siren while wanted.
- **Save**: money + completed missions persist across runs (a properties file).

## New code

- **`scenes.gta.DayNightCycle`** — advances a 0–1 time of day; computes sun direction, light color,
  ambient, and sky/fog color. `GtaScene` feeds these into the directional light, both shaders, and the
  clear color each frame.
- **`scenes.gta.Minimap`** — a 2D top-down overlay: its own ortho 2D shader + unit quad; draws the map
  background, nearby building rects, and entity blips. `Disposable`.
- **`scenes.gta.SaveGame`** — load/save money + completed-mission names via `java.util.Properties`
  (`gta-save.properties` in the working dir).
- Small hooks: `Economy(int start)`, `Mission.markComplete()`, `MissionManager.markCompleted/completedNames`,
  `PoliceManager.positions()`; generated `sounds/cash.wav` + `sounds/siren.wav`.

## Engine pieces reused

- **`ShaderProgram`/`Mesh`** for the minimap 2D pass (ortho); **`Light`** re-created per frame for the
  moving sun; the **fog** uniforms already on both shaders; **`Audio`/`Sound`**; **`Hud`**.

## Done when

- Minimap with blips, a visible day/night cycle, cash/siren audio, and persistent money/missions.
  Builds, engine tests green, launches (exit 124). **This is v0.4 — a small GTA.**

## Risks / open questions

- **Minimap** is a screen-space quad pass (depth off, blended) drawn last; keep it cheap (only nearby
  buildings). North-up, no rotation for the MVP.
- **Day/night** color model is approximate (hand-tuned), not physically based — just needs to read as
  a cycle.
- **Save location** — working dir (`game/`, the Gradle run cwd); no cloud/slots, just one file.
- **Siren** replays on a short timer while wanted (avoids needing a stop-sound API); a true looping
  siren is a later nicety.

## Next (beyond v0.4)

v0.4 is the north-star MVP. Future: more mission types + JSON authoring, weapon/vehicle variety,
interiors, a proper HUD/menu, `:game` unit tests, and performance streaming for a bigger city.
