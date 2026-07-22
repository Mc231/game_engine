# Prison Break — game spec

_First-person stealth escape, built on this LWJGL engine._

## 1. Pitch
You're an inmate. Sneak out of your cell, slip past patrolling guards' vision cones, grab the keycards you need, and reach the front gate — **without being seen**. Get spotted and it's back to the cell. Fun comes from reading patrol timings and threading the gaps.

## 2. Genre, perspective, target feel
- **Genre:** first-person stealth / escape.
- **Perspective:** first-person (reuses the engine's FP walk + mouse-look).
- **Feel:** tense, quiet, "hold your breath and move" — punctuated by the panic of a near-miss. Reference: _Metal Gear_ / _Splinter Cell_ light-stealth, but simplified.

## 3. Core loop
- **15-second loop:** observe a guard's patrol → wait for it to turn away → move to the next cover/doorway → repeat. Interact (E) with keys/doors along the way.
- **Session loop:** cell → cell block corridor → collect keycard A → yard → collect keycard B → gate → **freedom**. Getting fully spotted resets you to the level start (or last checkpoint). Track escape time for a personal best.

## 4. Controls (→ `InputMap` named actions)
| Action | Keyboard | Gamepad |
|---|---|---|
| move | W/A/S/D | left stick |
| look | mouse | right stick |
| interact (`use`) | E | A |
| crouch/sneak (`sneak`) | Left Ctrl | B |
| pause | Esc | Start |

Sneaking = slower + smaller detection radius. All via `InputMap` + `Gamepad`.

## 5. Game states & scenes (each a `Scene`, registered via `Application`)
- **`MenuScene`** — title + "Play" / controls; `Enter` starts.
- **`PrisonScene`** — the level (the game). Owns the world, guards, player, HUD.
- **`CaughtScene`** — "CAUGHT!" → restart the level.
- **`WinScene`** — "YOU ESCAPED" + time.
Scene changes are driven in code (not the debug number keys): the active scene calls back into `Application`/`Engine` to switch. **Engine gap:** scenes can't currently request a switch (see §15).

## 6. World & content
- **Full level, code-built rooms** for v1: the level is assembled from box `MeshRenderer` entities (walls, floors, cell bars, props) placed by a `PrisonLevel` builder class, each with an `AABBCollider`. Layout: **cell → cell block corridor → guard room (keycard A) → yard (open, skybox) → gate room (keycard B) → exit gate**.
- **Authoring:** hand-placed in code first; a JSON level schema is a stretch (see §15). Reuse `Geometry.cubeWithNormalsAndUV` for boxes; textures make them read as concrete/metal.
- **Scale:** ~6–8 rooms, corridors ~3 m wide, so 2–4 guards have room to patrol.

## 7. Entities & components (mini-ECS)
Reuse `Entity`/`World`/`MeshRenderer`; new **game-side script components** (extend `engine.Component`):
- **`AABBCollider`** — a world-space `AABB` for walls/props; queried for player collision and guard line-of-sight.
- **`GuardComponent`** — patrol waypoints, move speed, facing, vision cone (FOV°, range), alert state + detection meter; `update` walks the route and checks the player.
- **`DoorComponent`** — `locked`, `requiredKeyId`, open/closed; opens on `use` if the player holds the key.
- **`PickupComponent`** — `keyId`; on `use` (or overlap) adds to the player's inventory, then despawns.
- **`TriggerComponent`** — an `AABB` zone firing an event on player enter (the exit gate, checkpoints).
- **Player** — a `CharacterController`-style controller + a small key inventory (a `Set<String>`).

## 8. Player & camera
- **Controller:** extend the FP feel of `CharacterController`, but it must resolve **wall collisions** (slide along `AABBCollider`s), not just ground-clamp to a heightfield → a `PrisonPlayerController` (or an engine upgrade — see §15). Crouch lowers eye height + speed.
- **Camera:** first-person; `controller.viewMatrix()` straight to the render.

## 9. Rendering & look
- **Art direction:** stylized low-poly + textured boxes (the engine's sweet spot). Grimy concrete/metal indoors, open sky in the yard.
- **Lighting:** a few `Light` **point** lights (cell/corridor bulbs) + a `directional` light for the yard. `Phong` (no PBR).
- **Sky:** `Skybox` for the outdoor yard (an overcast/dusk cubemap).
- **Extras:** **MSAA** (on), gentle **distance fog** in long corridors, **`InstancedMesh`** for repeated props (cell bars, ceiling lights). **Normal maps** on wall/floor textures (as we did for terrain) for grime detail.
- **Shadows:** only one directional shadow map exists → usable for the **yard** (guards/props cast shadows outdoors); indoors stays unshadowed for v1 (see §15).

## 10. Assets (prefer CC0; each third-party asset needs a `CREDITS.txt`)
**Models (OBJ/MTL only):**
- Guard humanoid — **needs a CC0 OBJ** character. Source from Kenney ("Mini Characters"/"Blocky Characters") or OpenGameArt; **risk:** many character kits ship glTF/FBX only → fallback is a simple boxy guard we model from primitives (§15).
- Props — bed, toilet, table, door, keycard: Kenney "Furniture Kit"/"Prototype Kit" (CC0 OBJ). Cell bars = thin instanced boxes.
**Textures (PNG/JPG, tiled):** concrete floor, brick/metal wall, rusted door — **ambientCG (CC0)**; grab Color + NormalGL like we did for the driving world.
**Audio (16-bit PCM WAV):** footstep, keycard pickup, door unlock, alarm/siren, "caught" sting — generate simple WAVs or source CC0.
**HUD:** stb_easy_font text (objective, key count, alerts) — no assets.

## 11. UI / HUD (`Hud`, 2D ortho)
- Top-left: current **objective** ("Find keycard A").
- Top-right: **keys held**.
- Center alert: **"SPOTTED!"** flash + a detection meter bar (drawn as a `Hud` quad) that fills while a guard sees you.
- Screens: menu, caught, win (with escape time). A crosshair/interaction prompt ("E: open door") when near an interactable.

## 12. Audio (`Sound`)
- Looping alarm when alertness is high; footsteps tied to movement; positional-ish volume by distance (simple attenuation, since OpenAL supports it — stretch). Pickup/unlock one-shots; a musical sting on caught/win.

## 13. Rules
- **Win:** reach the exit `TriggerComponent` after opening the required doors.
- **Lose:** a guard's detection meter fills (in cone + in range + line-of-sight clear for ~1.5 s) → **caught** → restart from level start (or last checkpoint).
- **Difficulty:** guard count, cone width/range, patrol speed, and whether crouch is required.
- Detection is gated by **line-of-sight** (raycast player→guard vs. `AABBCollider`s) so walls actually hide you.

## 14. Persistence
- `Settings` (`.properties`): resolution, vsync, mouse sensitivity, volume.
- Best escape time + "escaped" flag saved via a small properties file or `SceneSerializer` JSON.

## 15. Engine gaps (what to add; ★ = effort; MVP = needed for first playable)
| Gap | Effort | For |
|---|---|---|
| **Player wall collision** (AABB slide, not just ground-clamp) | ★★ | **MVP** |
| **Trigger/zone system** (AABB enter events) — trivial with `AABB` | ★ | **MVP** |
| **Interaction system** ("use" ray/overlap on nearest interactable) | ★ | **MVP** |
| **Guard AI** (patrol waypoints + vision cone + LoS via `Ray`/`Intersect` + detection meter) — game-side, no engine change | ★★ | **MVP** |
| **Scene→scene switch request** (a scene asks `Engine` to change scene) — small `Engine` API | ★ | **MVP** |
| **CC0 OBJ humanoid guard** (or model a boxy stand-in) | ★ | MVP |
| **Level authoring in JSON** (extend `SceneData` for walls/guards/doors/triggers) | ★★★ | stretch |
| **Indoor/point-light shadows** (only 1 directional map today) | ★★★ | stretch |
| **Skeletal animation** (guards currently rigid — slide/turn, no walk cycle) | ★★★★ | stretch |

## 16. Build plan (MVP-first, then layers)
- **Phase 0 — MVP (smallest playable stealth):** one room + corridor + exit trigger; FP controller **with wall collision**; **one** patrolling guard with a vision cone + line-of-sight; get-spotted-→-restart; reach-exit-→-win. Boxy placeholder art. → `PrisonScene`, `PrisonPlayerController`, `GuardComponent`, `AABBCollider`, `TriggerComponent`, engine scene-switch API.
- **Phase 1 — Interaction + keys:** `use` action, `PickupComponent` (keycard), `DoorComponent` (locked door), player inventory. Gate the exit behind a key.
- **Phase 2 — Full level:** the `PrisonLevel` builder (cells, corridors, guard room, yard, gate), 2–4 guards, checkpoints, point/directional lighting + yard `Skybox`.
- **Phase 3 — Look & feel:** wall/floor textures + normal maps, `InstancedMesh` props (bars/lights), MSAA/fog, guard shadows in the yard.
- **Phase 4 — Presentation:** `Hud` (objective, keys, detection meter, prompts), `MenuScene`/`CaughtScene`/`WinScene`, `Audio` (footsteps/alarm/unlock/stings).
- **Phase 5 — Polish/stretch:** best-time persistence, JSON level authoring, animated guards, indoor shadows.

## 17. Open questions
- **Guard model:** OK to start with a **boxy placeholder guard** (built from primitives) if no clean CC0 OBJ humanoid turns up? (Keeps Phase 0 unblocked.)
- **On caught:** hard restart to level start, or **checkpoints** at each area?
- **Time pressure:** pure stealth, or also a **countdown/alarm timer** once first spotted?
- **Yard:** genuinely open (skybox + a wall perimeter) or a small enclosed courtyard?
