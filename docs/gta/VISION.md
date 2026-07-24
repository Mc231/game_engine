# Grand Theft LWJGL — Vision

> Working title / codename: **MiniCity**. Rename anytime.

The main project goal for this engine: **a small, stylized, third-person open-world
crime sandbox** — a "small copy of GTA III" — built entirely on our own LWJGL engine.
We build it in playable increments (phases), each with its own build-ready spec, so the
game is fun and runnable at every step, never a big-bang.

## The pitch

You're on foot in a low-poly city block. You walk up to a parked car, get in, and drive.
You can roam procedurally-generated streets, dodge (or hit) pedestrians and traffic, draw
a weapon, cause chaos, watch a wanted level climb as police respond, and run missions for
money. Stylized, arcadey, readable — not a simulation.

## Target feel

- **Reference:** GTA III (third-person, over-the-shoulder-ish orbit camera), stylized low-poly.
- **Camera:** orbit/chase camera behind the character on foot, and behind the car when driving.
  The same mouse-orbit camera we already use in `DrivingScene`, adapted per mode.
- **Tone:** clean modern city, bright Phong lighting, exaggerated arcade handling. Readability
  over realism. Think toy-city, not photoreal.

## The four pillars (all in scope)

1. **Driving & vehicles** — multiple cars, arcade handling, enter/exit. The heart of the game.
2. **Open city + street life** — procedural streets/blocks/buildings; pedestrians on sidewalks
   and traffic that reacts to the player.
3. **Combat, weapons & police** — hitscan weapons, hit reactions, and the wanted-level tension
   loop (commit crime → stars rise → police chase).
4. **Missions, money & progression** — mission triggers, objectives, rewards, a simple economy —
   the reason to keep playing.

## Art direction

**Modern city, stylized low-poly.** Engine sweet spot. Sourcing is CC0-friendly:

- **Kenney City Kit / City Kit (Roads) / Car Kit** — OBJ buildings, road pieces, vehicles.
- **ambientCG** — road/asphalt/concrete/glass textures (JPG) + normal maps.
- Procedurally-generated textures / WAVs where a kit gap exists.

Every third-party asset is CC0 and OBJ/PNG/JPG/WAV-compatible, with provenance in `CREDITS.txt`.

## What the engine already gives us (≈60% of the primitives)

| GTA pillar | Engine status |
|---|---|
| Drivable cars + chase camera | ✅ `CarController` + car model + mouse-orbit cam (`DrivingScene`) |
| On-foot movement + collision | ✅ `CharacterController` (walk, gravity, ground-clamp, wall-slide via `Collide`) |
| Animated character | ✅ Procedural-walk humanoid (the Prison Break guard) — our pedestrian/avatar base |
| NPC AI (patrol, sight, react) | 🟡 `Guard` patrol + `Vision` cone/LoS → peds & cops |
| Shooting | 🟡 `Ray`/`Intersect`/`AABB` raycast (`PhysicsScene`) → hitscan weapons |
| City / streets | 🟡 `Road` (spline/loop), `Scatter`, `InstancedMesh`, `Terrain` → need a block/building generator |
| World rendering | ✅ Skybox, fog, MSAA, instancing, directional shadow map, post-FX |
| HUD text | ✅ `Hud` (stb_easy_font); **minimap** needs a new 2D pass |
| Levels / data | ✅ `SceneData`/`SceneSerializer`/`SceneBuilder` (JSON), `Settings` (properties) |

## Hard engine constraints (define the art, not blockers)

- OpenGL 3.3 core; **models are OBJ/MTL only** (no glTF/FBX).
- **No skeletal animation** — character motion is *procedural* (per-limb matrix animation, as
  proven on the guards). Idle/walk/run/aim are pose blends, not rigged clips.
- **No physics engine** — collision is AABB slide + ground-clamp + raycasts. Arcade, not simulated.
- One **directional** shadow map (no soft/point/cascaded shadows).
- **Phong** lighting (no PBR).
- Text is stb_easy_font (blocky). Fine for an arcade HUD.

These push us toward exactly the stylized low-poly look we want.

## Non-goals (explicitly out, to keep scope sane)

- No online/multiplayer. No character customization. No interiors-as-separate-levels (streets only for now).
- No radio/licensed music (procedural/CC0 SFX + ambient only).
- No ragdoll physics, no destructible buildings, no realistic vehicle damage model.
- No swimming/flying/aircraft in the core game (possible stretch far later).

## How we work

- Each phase ships a **playable increment** with its own spec in `phases/phase-XX-*.md`.
- Big new systems get a design deep-dive in `systems/*.md` before we build them.
- We reuse existing engine classes wherever possible; genuinely new capability is flagged as an
  **engine gap** with an effort estimate, and often becomes a reusable engine class (not game-only code).
- Verify-by-running (make the scene first in `Main`, `timeout ./gradlew run`, exit 124 = it stayed open).

See **ROADMAP.md** for the phase ladder.
