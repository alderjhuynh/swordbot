# Aura (SwordBot v2) — a clientside duelist built to beat Opus

## 1. Purpose

This repo contains **two** fighting bots that exist to fight each other:

| Bot | What it is | Where it lives |
|---|---|---|
| **Opus v1** | A **datapack** bot. It possesses a `minecraft:mannequin` entity and emulates a player *exactly* (movement, look, attack strength, crits, knockback, eating, bows) using mcfunctions + scoreboards. It is the **benchmark / sparring partner**. | `run/saves/Opus v1 Test World/datapacks/opus_v1/` (+ `opus_v1_menu/` duel harness) |
| **Aura** (`swordbot-v2` mod, build id `aura-v35`) | A **clientside Fabric mod** that plays *your* client: it presses your keys, moves your mouse, and clicks your attack button. Its only purpose is to **beat Opus** in a fair melee duel. | `src/client/java/com/aura/swordbotv2/client/` |

The loop is: spawn Opus in the test world → enable Aura (`G`) → Aura senses, aims,
and melee-trades with Opus → per-fight telemetry lands in `run/swordbot-telemetry/fights.jsonl`.
Tune Aura until its win-rate / damage-trade against Opus (same skill preset) is positive.

There is also a throwaway physics prototype in `sim/` (`duel.py`, `analyze_live.py`) that
predates the current in-game bots. It is not wired to anything.

## 2. Repo map

```
src/client/java/com/aura/swordbotv2/client/
  SwordBotV2Client.java        # keybinds (G/H/J/K), END_CLIENT_TICK hook
  BuildInfo.java               # build id string stamped into telemetry ("aura-v35")
  bot/
    BotEngine.java             # per-tick arbitration: sense → aim → brains → motor → log
    SenseState.java            # L0 perception snapshot (self + target geometry + cooldowns)
    Aim.java                   # want-angles + turn-capped stepper, box raycast helper
    Targeting.java             # entity lock (nearest LivingEntity, players preferred)
    MeleeBrain.java            # duel engine: strafe plans, confirms, click gating
    PursueBrain.java           # far-field chase / last-seen pursuit
    RecoverBrain.java          # eat/gap healing + weapon/bow inventory helpers
    BowBrain.java              # ranged window (punish eaters at range)
    Motor.java                 # writes Intents into real keybinds + look rotation
    Intents.java               # f/s/jump/sprint/attack/use output struct
    FightLogger.java           # per-fight JSONL telemetry (fights.jsonl)
  primitives/
    KeyPress.java              # press() = setDown(true) + clickCount++ (a real click)
    mouseactions/Attack.java / UseItem.java / PickBlock.java
    keyactions/*, lookactions/*   # legacy action library, mostly unused by BotEngine
  mixin/KeyMappingAccessor.java   # exposes clickCount for KeyPress
run/saves/Opus v1 Test World/datapacks/
  opus_v1/                     # the Opus bot itself (~509 files)
  opus_v1_menu/                # clickable duel menu: kits, styles, arena, round loop
```

## 3. Aura — how it works

### 3.1 Tick pipeline (`bot/BotEngine.java`)

Every `END_CLIENT_TICK` while enabled (`G` toggles):

1. `Targeting.tick` — validate/drop the locked `LivingEntity` (dead/removed/>64m clears it;
   re-acquire only if `config.autoTarget`, which defaults to **off**).
2. `SenseState.tick` — snapshot self + target (see §3.2).
3. `Aim.compute` — desired yaw/pitch onto the closest point of the target hitbox (+ skill noise).
4. Brain arbitration (first match wins, mirrors Opus's `brain/tick` priority):
   - `RecoverBrain` (heal, mode `R*`) >
   - `BowBrain` (ranged, mode `B`) >
   - `PursueBrain` (far chase, mode `P`; delegates the click to `MeleeBrain` via scratch intents) >
   - `MeleeBrain` grounded (`M<plan>[P]`) / airborne (`MA`, `MAP`).
5. `FightLogger` bookkeeping (`begin` on new target, `onSwing` per dispatched click,
   `onTick` counters + trace tail).
6. `Motor.apply` — rate-limited look (`Aim.step` → `LookAtRotation`), movement keys,
   `keyAttack` click via `KeyPress.press`, `keyUse` hold.

`xhair` (`client.crosshairPickEntity == sense.target`) is threaded into `MeleeBrain`
as the `crosshairOn` confirm input. The `gate` integer (see §3.4) records *why* a tick
did or did not swing and is logged every tick.

### 3.2 Perception (`bot/SenseState.java`)

One snapshot per tick, then cheap derived values:

- Self: pos/eye/vel, `onGround`, `fallDistance`, hp+absorption, food, sprint flag,
  `strength = player.getAttackStrengthScale(0.5F)` (0..1, **1.0 = fully charged**),
  `hurtThisTick` edge + `lastHurtAge`.
- Target: pos/eye/vel (+ EMA smoother), dims, hp, `hurtTime`, `isUsingItem`,
  `hdist` (XZ), `reachMe` (eye→target-box), `reachOp` (target-eye→self-box),
  radial/tangential vel, speed, `critOk = falling && !onGround && !sprinting && strength>0.9`.
- LOS: single eye→eye ray (`ClipContext.Block.COLLIDER`); a hit counts as "seen" only
  if it lands inside an expanded target box.

Note the eye-ray LOS is stricter than the body's crosshair ray — a target can be under
the crosshair while `targetVisible` is false (see §7, cause D).

### 3.3 Aiming (`bot/Aim.java`)

- `compute` clamps the eye position into the target box (± inset 0.06) and converts to
  yaw/pitch, then adds uniform skill noise (`aimErrDeg`, pitch × 0.6).
- `step` slews `current → want` capped at `turnDegPerTick`, applied by `Motor`.
- `LEAD_TICKS = LEAD_MAX = 0` — **no target leading** currently; aim tracks the present
  position. `rayBox` is a slab-method ray-vs-AABB used by `MeleeBrain` to test the
  *planned* (`want`) angles.

Key subtlety: the melee decision tests `wantYaw/wantPitch`, but the actual look and the
vanilla crosshair lag behind by the turn cap (and by one tick, since `Motor` applies
*after* the decision). Three different angles — planned, applied, crosshair — can
disagree transiently (see §7, cause B).

### 3.4 Melee (`bot/MeleeBrain.java`) — the duel engine

Per engaged tick (target visible):

1. **Replan** the strafe plan every `thinkTicks` (every tick when close at high skill,
   plus on hurt-taken / target-airborne / plan-exhausted). Plan 0 = press `W`, plan 1 =
   hold; aggression-weighted, with `mistakePerMille` random overrides.
2. **Confirm** the hit is real: `rayConfirm` (want-ray enters box ≤ 3.0m, `reachMe` ≤ 4.2)
   **or** `pickConfirm` (vanilla crosshair on target, `reachMe` ≤ 3.5). Else `gate = 7`.
3. Movement: out-of-confirm + `hdist > 2.5` → sprint at them (`transit`, jump if far);
   in-confirm/hug → strafe (`STRAFE_FLIP_TICKS = 14`), crit-timed hop (v34): jump
   when grounded with `0.45 < strength < 0.95` so the ~6t rise overlaps the rest of
   the recharge and the descent lands on a full-strength swing (above ~0.95, swing
   now — rate beats crit).
4. Click gating (first match blocks, sets `gate`):
   - `inreachTicks <= reactTicks` → `4` (confirm must be *consecutive*; flicker resets it)
   - `strength <= 0.9` → `2`
   - `targetHurtTime > 0 && age - lastHitDealtAge <= 10` → `3`: the foe's hurt flash
     only blocks us when the last HP drop we observed was our own recent landed hit
     (mirrors Opus `attack_rule` / `sim/melee_click`; v33 fix — see §7). No extra
     land-debounce: `hurtTime` IS the i-frame clock.
   - opponent (Aura) will be hittable in ≥2 ticks and we're sub-1.0 without a crit → `4`
     (greedy wait for full/crit — intentional, reads as stalling)
   - rising (`!onGround`, `fallDistance <= 0`), sprint off, foe's swing recently spent
     (`opponentReadyIn >= 2`) → `4` (v34 rise hold: wait for the falling crit.
     Unreachable while the client sprint flag sticks on — see v35 hygiene below.)
   - sprint flag still on with a swing otherwise imminent (`strength > 0.85`) → `4`
     (v35 hygiene cut: release W + sprint for one tick *without* swinging so the
     server-side STOP precedes the click; streak-capped at 3, then swings anyway.
     Skipped on jump-reset ticks.)
   - else `attack = true`, `gate = 0`, `commitAttack` (forces `W`, kills sprint/jump).

`BotEngine` remaps a residual `gate == 6` (no brain claimed the tick) to
`0/5/2/3/4` from live state for the log. Gate legend: `0` swung, `1` unused,
`2` weak charge, `3` foe i-frames, `4` reacting/waiting, `5` using item,
`6` no-brain/idle, `7` no confirm.

`ensureWeaponForAttack` (in `RecoverBrain`) swaps to a hotbar weapon when a swing is
queued — the swap tick's swing is **swallowed** (`attack = false`). Since v33 the
hurt gate is land-anchored, so a swallowed tick no longer poisons later gating.

Jump reset (v35, in `MeleeBrain` after movement, before gating): took damage this tick
(`hurtThisTick`), grounded, pressing `W` → force sprint-`W` + jump for one tick. This
mirrors Opus `jump_reset` (hurt + ground + sprint + forward ⇒ jump): the impulse
converts horizontal knockback into a vertical pop-up, and the pop-up feeds the pcrit
chain (rise hold ⇒ hygiene cut ⇒ falling swing). Fires even without confirm; airborne
pops skip it (jump needs ground) and go straight to the hold. Does not consume the hop
quiet timer.

### 3.5 Other brains

- `PursueBrain`: claims ticks when far (`hdist ≥ 7`) or the gap is opening
  (`hdist ≥ 5` + closing vel). Sprint-`W`, jump to close. Suppresses its own attack/use
  and re-exposes `MeleeBrain`'s click through scratch intents.
- `RecoverBrain`: gap/food meals. `want` 3/2/1 from hp/food thresholds; needs a quiet
  window (`window ≥ 36` for food, `≥ 20` for gaps, from opponent-readiness + distance).
  Eating backs up (`f = -1`) and holds `use` up to 60 ticks. Hotbar-only slots (0–8).
- `BowBrain`: draws at `hdist ≥ 13` (or punishing an eater ≥ 6m), stops ≤ 4.5m opponent
  reach or after 40 ticks. Leads + gravity-compensates via iterative flight-time solve.
- `Motor`: `keySprint` only when `s == 0`; attack = one `KeyPress.press` per tick
  (down + clickCount++). **Vanilla resets the attack-strength ticker on every such
  click, hit or whiff** — a mistimed click costs a full recharge (see §7, cause A).

### 3.6 Config (`bot/BotConfig.java`)

`Skill` ≈ Opus presets 1:1 (turn cap, think cadence, aim error, mistake rate, react):

| Skill | turn°/tick | think | aimErr° | mistake ‰ | react |
|---|---|---|---|---|---|
| POTATO | 12 | 6 | 6.0 | 120 | 10 |
| AVERAGE | 30 | 3 | 2.5 | 50 | 5 |
| PRO | 60 | 2 | 0.8 | 15 | 2 |
| ELITE | 120 | 1 | 0.3 | 5 | 1 |
| OPTIMAL | 180 | 1 | 0.0 | 0 | 0 |

`Style` = AGGRESSIVE (85/25) / BALANCED (50/50) / DEFENSIVE (30/80) aggression/caution.
`gapHpThreshold = 12`, `critHpThreshold = 7`, bow window 13–30m. `autoTarget` defaults
**off** — target with `H` (nearest, players preferred) after (re)spawns.

Keys: `G` toggle, `H` retarget, `J` cycle skill, `K` cycle style.

### 3.7 Telemetry (`bot/FightLogger.java` → `run/swordbot-telemetry/fights.jsonl`)

One JSON object per fight (`begin` on new locked target, `close` on kill/death/switch/
timeout/abort). Fields: build id, ticks, reason, skill/style, foe/weapon, swings, hits,
dmg dealt/taken, eat ticks/sessions, gaps/food used, range profile, `gates[8]`,
air/strafe/sprint ticks, plus `trace_tail` (10-tick samples:
`tick,reach,hp,foeHp,flags,gate,mode,hurtTimes,strength,food,sprint,radialVel`).
Crit-attempt telemetry (v34): **`air_swings`** = swings released airborne,
**`fall_swings`** = subset released while falling (`fallDistance > 0`) — a falling +
full-charge + sprint-off swing is a vanilla crit, so `fall_swings / swings` is the
crit-attempt rate to watch (v33 fights: 0/9 — zero crits by construction).

Stall telemetry (this change): **`ready_ticks`** = ticks where the vanilla crosshair is
on the tracked target **and** `strength ≥ 0.999` (full recharge); **`ready_idle_ticks`**
= subset where `intents.attack == false`. A high `ready_idle_ticks / ready_ticks`
ratio with `gate` mass on 3/4/7 pinpoints the stall cause without guessing.

## 4. Opus v1 — the benchmark (datapack)

Opus is a **player-exact mannequin duelist**: all bot state lives in scoreboards
(`opus.*`, integer-scaled floats), decisions are mcfunctions, and every output is
re-emitted through player-identical physics/combat math. ~509 files under
`.../datapacks/opus_v1/data/opus/function/`.

### 4.1 Per-tick order (`core/tick`, called from `tick.mcfunction`)

`sense/self` (one NBT snapshot → scores) → `sense/target` → staggered knowledge
(`know/opponent`, `know/melee` every tick; `know/ranged`, `know/terrain`, slow
opponent reads on phase) → `brain/tick` arbitration →
`recover > gear > bow > pursue > melee` (first claimer sets `#took`) → ledge/dodge
strafe overlays → `motor/look → attack → use → sprint → move` → `psim/end`
(food/regen emulation) → `core/post` → fx/rec/debug.

### 4.2 Knowledge (facts, no intents)

- `sense/*`: self pos/vel/yaw/pitch/ground/fall/hp/hurt + knockback intake;
  target relative vector, dims, eye heights, velocity (+EMA), LOS stepping.
- `know/melee`: **exact contact geometry both ways** (`hdist`, `reach_me`, `reach_op`),
  strength timeline (`str`, ticks-to-full `t_full`, ticks-to-90% `t_s90`), `critok`
  (falling, airborne, server-sprint off).
- `know/opponent`: foe equipment/damage reads, hurt-cadence (`o_ready`), airborne flag,
  eater detection; `on_hit`/`took_hit` update the trade model.
- `know/ranged`, `know/terrain`, `know/recovery`: arrows, ledges, heal state.
- `psim/*`: server-side food/saturation/regen/gap-effect prediction so heal timing is exact.

### 4.3 Melee + attack rule (`act/melee/*`, `motor/attack`, `combat/*`)

`act/melee/tick`: aim at closest hitbox point → `attack_rule` → on click `on_attack`
(else air-steer or 17-plan × 4-response search in `plan_step`).

`attack_rule` clicks **iff**: in reach on the *planned-aim* ray (≤ 2.999m), reacted
(`inreach_t > c_react`), `t_s90` elapsed, target outside its 10-tick hurt window *for
our own* recent hits (other sources' flashes still let real hits through), opponent
readable in ≥ 2 ticks, `reach_me` ≤ 2.7 — minus two greedy waits: skip for a falling
crit next tick, or for full strength when no crit is available. Final commit range
**2.7m** with ray ≤ 2.999m (conservative vs. the engine's 3.0 limit).

`motor/attack` then executes one click with exact player rules: blocked while using an
item (`hit_res = 4`); `swing` + `ticker = 0` **even on a whiff**; ray + LOS check or
`combat/whiff`; else `combat/hit` — full damage formula
`A·(0.2+0.8·s²)·(crit?1.5:1) + s·Sharpness`, crit = `s>0.9 && fall>0 && airborne &&
!sprinting(server flag)`, knockback `KB/2 + (sprint&&s>0.9 ? 0.5 : 0)`, difficulty
scaling vs players, attacker-knockback, sounds, `know/opponent/on_hit`, recorder events.

### 4.4 Skills, styles, loadouts, harness

- Skill presets (`api/preset/potato|average|pro|elite|optimal`) set only execution
  knobs — turn cap, think cadence, aim error, mistake rate, react delay. **Reach,
  cooldowns, physics, inventory are identical at every preset** (same as Aura's `Skill`).
- Personalities (`aggressive|balanced|defensive`) tune aggression/caution (same as
  Aura's `Style`). `config/defaults` holds the shared base.
- Loadouts (`api/loadout/*`: iron/diamond/netherite ± knockback, mixed/enchanted bows),
  kits, spawn/despawn/target/stats/stop API (`api/*`, `duel/tags/function/opus/*`).
- `opus_v1_menu` adds the clickable duel UI: kits 1–6, skills 1–5, styles,
  arena/spawn control, heal/creative/survival, best-of round loop with countdown,
  score and stats — the fastest way to stage Aura-vs-Opus fights.
- Recorder (`rec/*`): per-fight rows/sums/sweeps for offline analysis; debug
  (`debug/*`: whiff log, per-tick trace), test scripts (`test/*`).

## 5. Aura vs Opus — parity and deliberate differences

| Axis | Opus | Aura | Note |
|---|---|---|---|
| Skill knobs | turn/think/aimErr/mistake/react | same five, same values | tradecockpit parity |
| Click commit | planned-ray ≤ 2.999m **and** `reach_me` ≤ 2.7m | planned-ray ≤ 3.0m **or** crosshair + `reachMe` ≤ 3.5m | Aura reaches further — whiff risk (§7 A) |
| Hurt gate | only own recent hits block; others' flashes pass | same since v33 (`hurt>0 && age-lastHitDealt<=10`, land-anchored) | v32 over-suppressed: swing-anchored + 8t land shadow (§7 C) |
| Crit timing | 1t wait for falling-crit / full-strength when foe can't punish; W-release in air so the server sprint flag is off at the hit (`act/melee`, `air`) | v35: crit-timed hop + rise hold + 1t W/sprint hygiene cut before imminent swings + jump reset on hurt (`MeleeBrain`) | v33–v34: hopped at ~full with the sprint flag stuck on — `fall_swings` never converted (§7.2) |
| Whiff cost | ticker reset, logged (`whiff_log`) | same ticker reset, invisible until now | new `ready_*` counters close the gap |
| Target mgmt | trid/inv-cache, round-aware | single `LivingEntity` lock, manual `H` | stale/wrong-lock stalls (§7 E) |
| Aim | closest-point + noise, exact turn emu | closest-point + noise, turn cap | Aura has no target leading (`LEAD = 0`) |
| Heal/bow/pursue priority | recover > gear > bow > pursue > melee | recover > bow > pursue > melee | same shape (Aura has no gear/ledge/dodge overlays) |

## 6. Operating the matchup

1. Build the mod (`./gradlew build`) and install the Fabric jar client-side (MC 26.3,
   Java 25, Fabric Loader/API per `gradle.properties`).
2. Open `Opus v1 Test World`, use the menu datapack (`/function opusmenu:menu`) or
   `opus:api/*` to pick kit/skill/style, spawn Opus, and start/duel the round loop.
3. Look at Opus, press `H` to lock, `J`/`K` to match skill/style, `G` to enable Aura.
4. Fight. After each fight read `run/swordbot-telemetry/fights.jsonl`:
   `swings` vs `hits`, `gates` histogram, and now `ready_ticks` / `ready_idle_ticks`.
5. Iterate on `MeleeBrain` gating/ranges, then re-duel at the same preset.

## 7. Known issue: hit stalling at full charge + crosshair on (v33: primary cause fixed)

Symptom (v32 data, 4× OPTIMAL/netherite losses): the vanilla hit indicator shows ready
for extended stretches while Aura never swings — `ready_idle_ticks` 29–40 of
`ready_ticks` 32–45 (~90% idle). Accuracy was 100% (4–6 swings, 4–6 hits, ~3.17/hit),
so this was never a whiff problem in the hug-range regime — it was idle gating.
Opus landed 8–10 hits/fight at ~2.9–3.4/hit: per-hit damage is roughly parity (no
evidence Opus crits systematically heavier — fight 5 shows a ~6% edge at most), the
deficit is entirely **volume**. `analyze_live.py` now prints `rdy`/`idl` columns and a
`STALLING (...)` verdict with the hurt/held/noconf split.

Live gate mass: hurt (gate 3) 35–40%, held (gate 4) **0%**, noconf (gate 7) ~10–13%.
So the "waiting" theory is confirmed with precision — but it is gate-3 land-debounce
waiting, not gate-4 greedy waiting. Smoking-gun samples recur in every trace tail:
`str=92–100, foeHurt=0, gate=foe-hurt` — full charge, foe *not* in i-frames, still
blocked. That is the old unconditional 8-tick post-land shadow (`lastLandAge`), which
has no counterpart in Opus or `sim/melee_click` and can never save a wasted swing
(`hurtTime == 0` means a swing always applies). **v33 deletes it and re-anchors the
hurt guard on landed hits** (`targetHurtTime > 0 && age - lastHitDealtAge <= 10`),
exactly mirroring `sim/duel.py:434` and Opus `attack_rule`. Remaining hypotheses,
kept for the next round of telemetry:

- **A. Whiff → ticker-reset loop (not observed in v32 hug-range data: 100% accuracy).**
  Aura commits out to `reachMe` 3.5 / want-ray 3.0, beyond the engine's reliable ~3.0m
  connect range, and every click — hit or whiff — zeroes the charge. One mistimed click
  (stale 1-tick pick, want-vs-applied skew, aim noise) buys ~10 recharge ticks; at range
  the next commit can whiff again. Opus caps its commit at 2.7m/2.999m precisely to
  avoid this. Revisit if accuracy drops below ~100% at longer ranges.
- **B. Planned-aim vs actual-crosshair skew.** The click decision ray-tests `want`
  angles, but `Motor` applies turn-capped angles *after* the decision and vanilla
  resolves the click later still. `rayConfirm`-true/crosshair-false ticks click into a
  whiff (feeds A); crosshair-true/`rayConfirm`-false ticks rely solely on
  `pickConfirm`, which additionally demands `reachMe ≤ 3.5`.
- **C. (FIXED in v33) Hurt-shadow over-suppression.** The old code stamped
  `lastAttackAge` on *commit* (including swallowed weapon-swap ticks and whiffs), gated
  on *any* `targetHurtTime > 0` within 10 ticks of it, *and* added an unconditional
  8-tick post-land shadow (`lastLandAge`) that fired at full charge with `foeHurt == 0`.
  Opus blocks only when the last damage was its *own* recent hit — v33 matches that.
- **D. Greedy waits reading as stalls.** Post-damage `opponentReadyIn` sub-1.0 waits,
  full-strength/crit-next-tick waits, and `reactTicks` consecutive-confirm demand all
  idle the blade by design; in a mirror matchup both bots wait simultaneously. Lower
  skills (react 2–10 + aim noise flicker resetting `inreachTicks`) stretch this most.
- **E. Stale/wrong target lock.** `autoTarget` is off and the lock is a raw entity
  reference: Opus round respawns, deaths, or a nearer `LivingEntity` (armor stands are
  `LivingEntity`!) at lock time leave Aura tracking the wrong entity forever — HUD pick
  on Opus reads true while `pickConfirm` (vs `sense.target`) and the want-ray (aimed at
  the wrong body) both fail → permanent gate 7. `H` re-locks manually.
- **F. Eye-ray LOS vs body-ray pick.** `hasLos` eye→eye can report hidden (freezing aim
  and attacks) while the body crosshair ray still picks Opus, e.g. low cover.

Remaining fixes (accepted, not implemented — re-run duels on v34 and let
`ready_idle_ticks` + `gates[3/4/7]` rank them): commit only on true crosshair inside
3.0m (ray as steering, not trigger); re-order look-before-decide or ray-test post-step
angles; crosshair-vs-lock mismatch auto-retarget; single rising-edge click per commit.
Next telemetry watch-items: gate-7 residual (~10% noconf at 1.3–1.7m hug range in v32;
v33 fights instead show ~45% noconf at 2.4–3.1m with accuracy down to 60–89% — range
discipline regressed and needs explaining), `fall_swings / swings` (v34 crit timing),
and whether Opus per-hit weight changes once it stops getting free trades.

### 7.1 v34: the crit gap (Opus crits continually, Aura never does)

Live v33 data confirms the mechanism, not just the outcome: `air_swings = fall_swings
= 0` in every fight despite ~50% airtime — every swing is released grounded, so zero
crits by construction (~2.9/hit, uncharged-or-armor tier). Root cause is jump/swing
phasing: the old hop fired at `strength > 0.8`, so full charge arrived mid-rise and the
swing released into the ascent (`fallDistance == 0`), while nothing ever held a charged
swing for the descent. Opus instead phases descents onto full charge (crit-wait in
`attack_rule`, W-release in `air.mcfunction` for server-sprint hygiene) and additionally
converts Aura's own KB pop-ups into falling swings. v34 mirrors that with two coupled
changes: jump *early* (`0.45 < strength < 0.95`, rise overlaps recharge at ~zero cycle
cost) and *hold* charged swings through the rise when safe (gate 4,
`opponentReadyIn >= 2`; unsafe → swing now, rate over quality). New `sim/duel.py`
`hold_rise` opt mirrors the hold for headless runs — with one caveat: the sim does not
reproduce this matchup (sim baseline already crits 2.3/fight and sim-Opus crits 0.0,
vs live 0 and continual), so sim win-rates cannot validate the fix; only live
`fall_swings`, dmg/hit, and wins vs elite-Opus (then optimal-Opus) count.

### 7.2 v35: sticky sprint ate every crit (jump reset + pcrit chain)

Live v34 (10 fights): `fall_swings` rose to 1–3/fight, but dmg/hit stayed ~2.8–3.3 —
the falling swings never converted. Trace forensics: client sprint flag ON at 6/6
sampled swing ticks, and the v34 rise hold fired only 13/1638 ticks. Two coupled
causes, both in the sprint flag:

1. **Sticky sprint.** Aura holds `W` near-constantly and sprints on approach; once
   started, vanilla keeps the *server* flag on. Worse, the attack packet (sent from
   key handling early in the tick) beats the STOP_SPRINT packet (sent with movement
   later) whenever both leave on the swing tick — so even a same-tick key release
   resolves as a sprint-hit (+KB, never a crit). Every Aura swing was a sprint-hit.
2. **Hold starved.** The v34 hold requires `!sprinting`, so with the flag stuck on it
   could never fire — the phasing fix was in place but unreachable.

And the direct answers: **no, Aura did not jump-reset** (nothing jumped on
`hurtThisTick`; hurt only replanned) and **pcrits only worked incidentally** (post-hit
pop-ups flowed into the same flag-blocked hold). v35 adds all three Opus-mirrored
pieces: a 1-tick W+sprint hygiene cut before otherwise-imminent swings (streak-capped
at 3, degrades to non-crits never a stall), a hurt-tick sprint-`W` jump reset
(preempts the hygiene cut that tick — the KB-cancel matters more), and the resulting
chain reset ⇒ pop-up ⇒ hold ⇒ cut ⇒ falling swing. Judge by `fall_swings` converting
(dmg/hit → ~4.8 for netherite-SharpV vs armor 20/toughness 12) and a new analyzer
verdict that fires when falling swings don't convert.

## 8. Dev notes

- Aura runs fully clientside: no server fund, no packets beyond what vanilla key/look
  handling already sends. `KeyPress.press` + `clickCount` is what makes a held `attack`
  intent land as discrete vanilla clicks.
- Opus pack metadata: `pack.mcmeta` (`min/max_format: 121`, MC 26.3). Safe `/reload`:
  `load.mcfunction` re-declares objectives/constants idempotently.
- Style: Java 25 (`release = 25`), Fabric Loom, 2-space-agnostic existing style —
  match surrounding files. mcfunction comments document intent; keep them.
