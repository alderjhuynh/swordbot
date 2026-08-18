# Sword PvP — Composite Guide (bot-ready)

Synthesized from transcripts of tutorial videos (Intelligent's advanced guide, Combo Civics' high-tier guide, a beginner combo tutorial, a copy-the-pros guide, and the initial Sword course) plus the `swordbot (reconsituted)` codebase. Terminology is normalized; the "detect → do" notes are written against the bot's existing sensors (`selfStats`, `targetStats`, `WorldStats`) and action primitives.

**Game mode in one line:** no pots, no food-regen crutch — it's a damage race where whoever chains the cleaner, longer combo wins. The recovery is your health bar.

---

## 1. Core engine mechanics

These are the rules the whole game runs on. A bot wins by obeying these precisely; a human wins by approximating them.

### 1.1 Reach is tied to momentum

> "Your reach is partially tied to your momentum. So when you move forward, you have more range than your opponent."

- Forward momentum extends your effective hit range. A falling sprint hit (momentum) out-reaches a stationary or retreating opponent.
- Body-level aim caps reach at ~**2.7 blocks**. Aiming at the **head** extends range (needed for three-block hits). If the opponent sneaks/shifts to shrink their hitbox, aim at the lower section of the head.
- Implication for the bot: pick the aim point based on what you're trying to do (see §8.4 three-block hits).

### 1.2 Attack cooldown (sword charge)

- An attack only registers as a full knockback **sprint hit** when the sword is charged to at least **~85%** (`attackCooldown >= 0.85`).
- Below that it's a "tick" (aka "kit") — a low-damage, low-KB hit. Ticks have two *deliberate* uses, don't spam them:
  1. **Stop your opponent's knockback before they escape your range**, then convert into a stun-combo.
  2. **Free chip damage** when you're already in a combo.
- On multiplayer servers, an uncharged hit **freezes their knockback** (bad if unintended — it ruins your combo spacing).
- Bot gate: for normal comboing, only call `AttackOnce` when `selfStats.attackCooldown >= 0.85`. Save `>=0.1..0.85` hits for deliberate ticks.

### 1.3 Sprint-hit knockback rules

- A **sprint hit** = you are sprinting **at the exact moment** the hit lands. This is the only kind of hit that produces combo-starting knockback.
- While sprinting, your knockback on them is reduced by **40%** — that reduction is *why* you stay close enough to chain the first hits.
- **Only the first sprint hit after sprinting starts** delivers sprint-hit KB. To deliver full KB again you must **reset sprint** — release `W` (W-tap) or tap `S` (S-tap).
- If you don't reset, the hit becomes a **sweep** (see §7). **If you see sweep marks, you're sprint-hitting wrong.**
- Bot gate: manage sprint as a resource. `StartSprint` before approach, and reset via `MoveKeyUp(FORWARD)` → `MoveKeyDown(FORWARD)` (W-tap) or `MoveKeyDown(BACK)` → back (S-tap) at each hit boundary.

### 1.4 The golden rule of air knockback

> "Don't KB-hit someone who's falling down... hitting someone midair knocks them in the direction they're already going."

- If you hit an opponent who is **falling**, they take *little* knockback (they're already moving down / into the hit).
- Wait until they are **on the ground or jumping up** to get full KB.
- Conversely, **space yourself so the opponent is forced to hit you while you are falling** — this minimizes the KB you take, lets you crit repeatedly, and can start a combo.
- Bot gate: before committing a sprint hit, check `targetStats.targetOnGround` and `targetStats.targetVelocity.y`. Don't KB-hit a falling target; do KB-hit a rising/grounded one.

### 1.5 Ping is a parameter, not a bug

- Deflection timing must be adjusted per opponent ping.
- High-ping players take **delayed knockback** and will hit-select you out of your combo. Don't try to combo them with standard sprint hits.
- High ping → use **no-KB crits** and **double crits**, stay close, play the crit game.
- Low ping → normal combos, and prefer **back sweep** (less KB) over falling sprint hit so they don't fly out of range before you can chain.
- Bot gate: measure ping from the connection (or `targetStats.targetSprinting`/hit behavior as a proxy) and switch the combo flavor accordingly.

---

## 2. Hit types

| Hit | How | Purpose |
|---|---|---|
| **Sweep** | Attack while moving without sprint reset | Accidental → avoid. Deliberate sweeps are a whole system (§7). |
| **Sprint hit** | Sprinting at the moment of the hit | KB, stuns, combos. The workhorse. |
| **Crit** | Attack while falling | Max damage, minimal/no KB. Not for comboing. |
| **Falling sprint hit** | Sprint-hit while descending from a jump | Momentum → extra reach, out-reaches faders, extends combos. |
| **Uppercut** | Jump + sprint hit at the **peak** of the jump | Reach someone too far away; surprise offbeat timing. |
| **Lunge** | Sprint-jump forward, sprint-hit while falling | Chasing players who hold S out of a trade. |
| **Hop** | Release W, jump almost straight up, minimal forward drift | Hold combos from outside opponent reach (§8.3). |
| **Tick (kit)** | Attack below ~90% charge | Stop their KB / chip damage (deliberate). |

---

## 3. Sprint resetting & combo archetypes

### 3.1 W-tap
- Hold `W`; **let go of `W` at the same moment you click** to hit. Re-press `W` immediately.
- Resets sprint → next hit is a fresh full-KB sprint hit.
- Two flavors:
  - **Pausing W-tap**: W-tap slightly slower. *Least* stun, but lets you follow up after they back out.
  - **Normal W-tap**: standard cadence. Slightly more stun, still follow-up capable.
- Default combo for most opponents.

### 3.2 S-tap
- Hold `W`, then at the hit: let go of `W`, press `S` and click simultaneously, snap back to `W`.
- **Most stun of all.** Costs you all forward momentum, so **no follow-up** if they escape.
- **Counters jump resets.** Great vs opponents who refuse to back out of combos.
- Trade-off rule: use W-tap for pressure + follow-up; use S-tap when you need max stun and the opponent fights back instead of fleeing.

### 3.3 Strafe combo
- W-tap while strafing (A/D) through the combo.
- See §8.1 for why strafing is worth it.

### 3.4 Choosing a combo mid-fight
- Vary the combo **based on what the opponent does**:
  - Opponent starts moving back → slow your timing, use pausing W-taps.
  - Opponent stays glued → faster W-taps or S-taps for more stun.
  - Opponent jump-resets → S-taps.
- Predict **when they'll back out**. If you successfully read the back-out, you win the game.

---

## 4. Starting a combo

Priority order for openings (from neutral / longer distance):

1. **Crit** (leading) — most reward, most options.
2. **Three-block hit** — even a failed three-block gives a double hit → hit advantage.
3. **Bait** — don't spam; opponents will learn to punish it.

### 4.1 Hit select (getting hit first, then winning anyway)
- Let the opponent hit you first, then with *your* momentum manage to hit them and start the combo. Nobody fully knows why it works; it does.
- **Best used right after a crit** — a crit brings you close enough to hit-select. Doing it at match start gets you countered by an S-tap.
- Skilled players counter hit-select by **moving back out of your range** while you swing.

### 4.2 Baiting / forcing a whiff
- When they jump in for a crit: **back away slightly** so they miss; their sword is on cooldown → free hit → free combo start.
- Get opponents to miss by **stopping movement / holding S** as they come in, or by **strafing side-to-side** mid-fight.
- Every whiffed attack is an opportunity for a **free stun**.

### 4.3 Three-block hits (the opening stun)
- Sprint at full speed toward a sprinting opponent and hit them **before they're close enough to hit you back** (~3-block reach). Works best when they're not at full sprint.
- Aim at the **head**; body aim caps you at 2.7 blocks. If they shift, aim low on the head.
- If the opponent is **jumping at you**, hit them as they're **rising** — usually a free stun.
- Read the telegraph: opponents lining up a three-block stop jumping and sprint straight at you.

### 4.4 Deflection (the premier starter)
- Sprint-hit the opponent **immediately after they attack** (their swing is on cooldown, you're in their space).
- "If you know what hit selecting is, this is pretty much the exact same thing."
- Timing + spacing both matter: too late or too close → they hit you **through** your deflection and you don't stun them.

---

## 5. Holding & extending combos

Three things break combos — check all three:

### 5.1 Sprint hitting
- If you see sweeps, you aren't W-tapping properly. Reset sprint every hit (§3).

### 5.2 Spacing
- Opponent hits you back → you're **too close**.
- Opponent escapes / combo just stops → you're **too passive**; they've walked out of reach.
- Ideal hold distance is just outside their reach but inside yours (**~3 blocks**), using hops/S-taps to keep that ring.

### 5.3 Timing
- **Faster hits → more stun** (opponent stays farther). **Slower hits → less stun** (they close in).
- Hit **too fast** → you hit them *while falling* → low/no KB (violates the golden rule).
- The "correct" cadence tracks their landing: hit as they touch ground.
- Stun is what keeps the combo alive — fast, well-timed hits chain into a **stun-lock**; slow hits let them escape or punish.

### 5.4 Extending / punishing the back-out
- **Lunge**: when they hold S to escape, sprint-jump forward + sprint-hit while falling. Momentum out-reaches their retreat.
- **Follow-up sweep**: don't reset sprint, predict the back-out, keep sweeping them (§7.2).
- **Uppercut**: extend the combo offbeat / catch them in the air.
- **Sprint jump** toward an S-holder to stay in range.
- Alternate **hops and lunges** to hold players who S-hold — one of the hardest techs, needs prediction.

---

## 6. The stun system

**Stun** = a sprint hit that knocks the opponent far enough away that they *cannot hit you back* for the recovery window. Combos are just chained stuns.

### 6.1 Initiating stuns
- **Deflection** — sprint hit immediately after their attack (preferred, §4.4).
- **Reverse deflection** — sprint-hit them immediately after *they* deflection you. Effective vs high-ping players. Only works if their deflection didn't already stun you.
- **Punish a whiff** — hold S / stop / strafe to make them miss, then sprint-hit (§4.2).
- **Three-block at trade start** (§4.3).
- **Jump-reset into their attack** — jump-reset their sprint hit → you get a **free crit** as you fall (§9).

### 6.2 Countering stuns
1. **Hold S (early)**: the #1 counter. If you can predict the stun, hold S *before* it lands so you fly out of the trade instead of eating the chain.
   - Attacking **right before** you hold S kills their momentum and stops them sprint-jumping after you.
   - Many players only S-hold *after* getting hit — that works for escaping combos but you eat 1–2 hits of avoidable damage. Experienced players need only one stun to shred you.
2. **Jump + hold S**: fly out of the trade.
3. **Sprint-jump toward them as you attack**: if they S-hold when hit and you're close enough, keep them in range.
4. **Jump reset**: press jump exactly when their hit lands → minimal KB (§9).

---

## 7. Sweeps — the deliberate kind

A sweep = attacking *without* sprint reset. Accidental sweeps are a mistake; **deliberate** sweeps are mixups that beat some of the best options in the game.

### 7.1 Opponent sweep / punish sweep
- Sweep to force a hit-select situation. Sweeping puts the opponent in a bad spot to hit-select you, so you start the combo.
- Throw it out in neutral by **not resetting sprint** while holding W.

### 7.2 Follow-up sweep
- Don't reset sprint, **predict the back-out**, keep sweeping.
- **Beats two of the most common options:** if they move back → you keep sweeping them; if they try to sprint-hit you → you hit-select them.

### 7.3 Side sweep (hold A or D + sweep)
- Good vs **aggro players** and **no-KB critters** (harder to stun those).
- **Low risk / high reward:** fail → you only traded a sweep for a crit; succeed → free crits.

### 7.4 Sweep combo
- Sweep-strafing extremely close is hard to aim against (aim is prediction-based). If they manage to sprint-hit you, **punish-sweep them** and start your combo.
- After they adapt to your sweeps, **mix in sprint hits** (the sweep KB throws off their aim) or crits.

### 7.5 Back sweep (S-sweep) — crit-trade counter
- **Crit trade counter**: when both of you are airborne and they try to W-release crit, hold S and, using **deflection timing**, sweep them. Starts your combo → hit advantage.
- Works as the first crit in an exchange too.

---

## 8. Movement toolkit

### 8.1 Strafing
- When you strafe you deviate from the straight line → **apply less resistance** → you take *more* KB, but so does your opponent when *you* hit *them* (your hits do more KB).
- Net: strafing in a combo is strictly good — harder for them to hit you, harder for them to aim (especially close-range), and your hits sting more.
- Also applies to three-blocks and baits (more KB, harder to hit you).
- **Vary your strafe cadence** or you get read and punished.

### 8.2 Repositioning
- Change position to create a **stun hit** — used heavily vs no-KB critters and when "test critting" (gathering data on their crit pattern).

### 8.3 Hops (advanced)
- **Mechanics:** while sprinting forward, release `W`, then **immediately jump** (kills forward momentum → you rise almost straight up). After the jump, hold `W` to re-sprint. Release `W` before landing and jump again — never sprint-jump forward by accident.
- **Time the sprint hit right before you release W while falling.**
- Why hops are strong:
  1. **Disguise sprint hits as crits** — good players don't hold S when they expect a crit (crits deal little KB), so they stay in a combo you're actually sprint-hitting them out of.
  2. **Build momentum to lunge** — a hop right before a forward jump lets you cover much more distance, so you can chase an S-holder and keep landing hits.

### 8.4 Double jump
- Jump twice (rebound) to build momentum → harder to sprint-hit and bait you. Forces the opponent to act or lose the positioning war.
- **Mix it up** — sometimes only one jump, sometimes bait-and-sprint-hit instead of critting. Predictable double jumps = free punish.

### 8.5 Turn-back movement
- Holding S while turned away = "S on steroids" — retreat much faster, but you can't see the opponent.
- Use when holding S isn't enough; **fake it** by starting the turn-back then snapping a crit back at them.

### 8.6 Crouch dodge
- Hold Shift (crouch) to duck a hit. Near-zero punish risk — they can't punish a crouch dodge, and if they *do* adapt they must aim lower, which **shrinks their reach**.
- Use when: forced into a bad position, going for three-blocks, or at ~1 HP as a hail-mary (you die anyway, might as well).
- Crits and falling sprint hits aim for your head — crouching dodges exactly those.

### 8.7 Uppercut
- Jump + sprint hit at the **peak** of the jump. Reaches targets who'd otherwise be out of range.
- Best uses: right **after you stun them** (deflection → uppercut starts easy combos on weaker players), or as an **offbeat mixup** when they're airborne (the falling sprint hit is more common and slower to execute).
- Often chained with an S immediately after to keep distance.

---

## 9. Jump resetting (the strongest escape / punish tool)

- **Do:** press jump at the **exact moment you take damage** (hit connects). Reduces **horizontal** KB, not vertical.
- It's a tool with four faces:
  1. **Counter stuns** — jump-reset a deflection/three-block.
  2. **Escape combos** — comboers hit on a rhythm (as you touch ground); jump exactly then.
  3. **Initiate stuns** — jump-reset a three-block attack → you get a free crit on the way down.
  4. **Get crits** — the reset state puts you where crits work.
- **Best situation:** mid distance, when the opponent is trying to land a stun hit on you.
- **Don't:** spam it or make the pattern obvious — treat it like shielding, only when necessary. When stuck *inside* a combo, S-out instead (exception: high-ping opponents).
- **Counters to jump resetting:**
  1. **Standing crit** — sprint hit, jump straight up, crit on the way down. If they jump-reset your sprint hit they jump *into your crit*. Only works if your sprint hit actually stuns (else they sprint-hit you away first).
  2. **Delayed sprint hit** — wait through their jump window; they jump early, you hit them airborne → they fly.
- **Practice signals:** Feather client jump-reset module for timing; damage-tilt on VOD review.

### 9.1 KB combos (jump-reset *their* sprint hit)
- Predictable hit timers get counter-comboed: jump-reset their sprint hit, then convert.
- Beats both **double crits** and **deflections**. Against a double crit it leaves you in a W-release-crit position that out-trades them; against a deflection it prevents the stun and lets you start a no-KB combo or switch into no-KB crits mid-chain.

---

## 10. The crit system & the RPS table

### 10.1 The crit family
| Crit | Execution | Notes |
|---|---|---|
| **W-release crit** | Jump and crit while jumping | Most common; the standard crit. |
| **Punish crit (P crit)** | Crit using the KB of their hit | Strong, but leaves you vulnerable (see double crit). |
| **No-KB crit** | Crit, then **hold W** (do NOT jump) | Reduces the KB you'd take; don't jump or you take *more* KB. |
| **Double crit** | Punish crit → **immediately jump and crit again** | Second crit can be no-KB or standard. Counters punish crits. |
| **Standing crit** | Sprint hit → jump → crit on the way down | Punishes punish-critters & jump resetters. |
| **Peak crit** | Crit timed to wait for the opponent to hit you | Slower than standard crits; out-traded by them. |
| **S crit** | Crit while backing (holding S) | Escape tool; lets you build distance. |
| **Sprint crit** | Crit **while sprinting** (don't reset sprint) | Common struggle; counterable. |

### 10.2 Counter chart — who beats whom

| You use | Beats | Loses to |
|---|---|---|
| **W-release crit** | double crits | falling sprint hit, crit deflect |
| **Falling sprint hit** | W-release crits, faders, baiters | no-KB crits |
| **No-KB crit** | falling sprint hits | W-release critting, double crits, crit deflect (+S-tap) |
| **Double crit** | no-KB crits, punish crits | sprint hits, W-release crits |
| **Crit deflect** (sprint hit after they land a crit) | W-release crits, no-KB crits | — (safest, most info) |
| **Sprint hit follow-up** | double crits | no-KB crits |
| **No-KB crit → P crit** | — (free damage) | double crits; even vs no-KB crits |
| **Back sweep** | W-release crit trade | — |

### 10.3 Decision tree (from Combo Civics)
- **Unknown opponent** → throw a **crit deflect** (crit-deflection). Safest option, and it feeds you information. Even a *wrong* crit deflect only costs a sprint-hit-for-a-crit trade; a wrong double crit gets you out-critted.
- **Crit deflects not working** → they're no-KB critting → **switch to double crit**.
- **They adapt to your double crits with no-KB crits** → **double crit reaction** (sprint hit them before the second crit of *their* double crit — but note this is countered by no-KB crits).
- **No-KB crit reaction** — if they keep no-KB critting through your double-crit reactions, out-trade with a double crit reaction.
- **They go for P crits** (detectable: their timing slows down) → **react**: double crit reaction (sprint hit before their second crit) or no-KB crit reaction.
- **They S crit to build distance** → **peak crit** to close in, typically followed by a **double crit**.
- **They S crit to reposition** → sprint-hit them as they re-enter your range (S-critters walk back inside your distance).
- **Out of range after a no-KB crit** → go for the **sprint hit**.
- **S crit on the exit** — when someone tries to exit a trade with an S crit, **get close** (peak crit closes the distance), or **S crit yourself / back off** before they can.

### 10.4 Double crits — the advanced punish (from Intelligent's guide)
- Purpose: counter **punish crits** (punish crits leave you vulnerable; double crits exploit the window).
- **Double crit** = your first crit (usually the punish crit) → **immediately jump and crit again**.
- **Countered by sprint hits** — if they sprint-hit instead of punish-critting, you get sent flying. **If that happens, immediately hold S** → you fly safely out of the trade, up on damage.
- **Bait it:** jump close to them with your first crit — provokes a punish crit → free double crit.
- **Messes with their aim:** go *around/behind* them mid-double-crit for a near-guaranteed hit.
- Initiate with a **punish crit** too (punish crit → crit again).
- **Only throw double crits when you know what the opponent will do** — they're countered many ways and every counter hurts.
- **Strafe during double crits** — close-range aiming is hard, you'll farm free hits.

### 10.5 Standing crits
- Initiate with a **sprint hit** instead of a crit: hit → jump → crit on the way down.
- Punishes punish-critters the same way double crits do, but is ideal when your sprint hit **doesn't knock them far enough to combo** — don't let them repeatedly punish-crit you, counter with the standing crit.

### 10.6 Crit deflect (+ S-tap)
- Crit deflect = sprint-hit the opponent **after they land** their crit (so they don't get reduced falling KB), then go for a combo.
- **Crit deflect + S-tap** after = enough stun to beat even no-KB crits. A *held* crit deflect loses the follow-up sprint hits (you get stunned); a clean one lets you hit-select into a combo.
- After any crit deflect, **look for the combo/punish** as they get knocked back.

---

## 11. Situational play

### 11.1 Neutral (long distance, no advantage)
- Goal: **first hit** and a **combo start**.
- **Lead with crits** — more options, more reward.
- **Close range** → look for **sprint hits** (a crit here gets three-blocked).
- **Longer range** → **crits** (you'll be falling by the time it lands — much harder to three-block) and **baits** (you're in baiting distance).
- Opponent goes for a crit? Three options: crit back (safe-ish, neutral), **three-block** (even a failed one gives a double hit → hit advantage), or **bait** (don't overuse).
- Vs **baiters**: momentum = reach. **Falling sprint hits** are your best friend — when they crit, falling-sprint-hit them; if they keep holding S, you outreach them. Watch out: if they re-enter, they can hit-select you. **Double jump** in to force the issue.

### 11.2 HP advantage (you're up)
- **Almost never go in with a crit** — you get baited, deflected, or knocked away and comboed, losing your lead.
- QZ's style: **lead with sprint hits, but jump whenever the opponent jumps** — safe *and* never out-traded by their crits.
- Play defensive/grounded — it's far easier to bait a crit than a sprint hit, and staying grounded takes away their angles.

### 11.3 HP disadvantage (you're down)
- Stay **long distance**, play **crits and baits**, try to land the combo.
- **Avoid exchanging sprint hits** — that's a losing battle.
- At ~1 HP: **crouch-dodge hail-maries** (you die anyway).

### 11.4 Inside a combo (you're the one being comboed)
- Options: **jump**, **hold S**, or **jump while holding S** (takes even more KB).
- **S-hold** is the more consistent escape; **jump reset** is higher risk / higher reward.
- Opponent **S-taps**? Jump resets stop working (good spacing makes them un-jump-resettable) → S-hold. Opponent **follows up aggressively**? S-holding gets punished → jump reset.

### 11.5 High-ping opponents
- **Don't combo them** with sprint hits — delayed KB means they hit-select you.
- Use **no-KB crits and double crits**, stay as close as possible, otherwise they combo you to death.

---

## 12. Bot implementation mapping

Everything in the guide maps onto primitives that already exist in the repo.

### Sensors (already ticking every tick)
| Need | Read from |
|---|---|
| Sword charge | `selfStats.attackCooldown` (0..1) |
| Sprint state | `selfStats.sprinting`, `targetStats.targetSprinting` |
| On-ground / airborne | `selfStats.onGround`, `targetStats.targetOnGround` |
| Falling / rising | `targetStats.targetVelocity` (`.y` sign), `selfStats.velocity` |
| Distance / spacing | `targetStats.distanceToTargetSq` |
| Attack telegraph | `targetStats.targetSprinting` + `targetStats.targetOnGround` + `targetStats.targetVelocity` |
| Aim / strafe | `selfStats.yaw`, `selfStats.pitch`, `targetStats.targetYaw/Pitch` |
| Server health | `WorldStats.tps` |
| Target store | `TargetManager.getTarget()` (set by `AttackMixin`) |

### Actions (already implemented)
| Move | Primitive |
|---|---|
| W-tap | `MoveKeyUp(FORWARD)` → `AttackOnce` → `MoveKeyDown(FORWARD)` |
| S-tap | `MoveKeyDown(BACK)` + `AttackOnce` → `MoveKeyUp(BACK)` + `MoveKeyDown(FORWARD)` |
| Strafe | `MoveKeyDown(LEFT/RIGHT)` held through W-tap cadence |
| Jump reset | `JumpOnce` fired on the hit frame (poll damage events or predict from `targetStats`) |
| Double crit | `AttackOnce` (punish crit) → `JumpOnce` → `AttackOnce` on descent |
| Crit deflect | `MoveKeyDown(FORWARD)` + `AttackOnce` timed to their landing frame |
| Crouch dodge | `SneakKeyDown` / `SneakKeyUp` |
| Look | `SmoothLook.lookAt(entity, ms)` (aim head vs body per intent) |
| Tick / chip | `AttackOnce` gated at `attackCooldown < 0.9` deliberately |
| Fallback timing | `TickScheduler.schedule(ticks, ...)` / `MillisScheduler.schedule(ms, ...)` |

### Suggested decision loop (per tick)
1. **Spacing check** — compute desired range from state (comboing ≈ 3 blocks; baiting ≈ ~3–4; neutral ≈ longest). If wrong, strafe/approach/retreat.
2. **Timing check** — only attack at `attackCooldown >= 0.85` (except deliberate ticks), and only KB-hit a **grounded or rising** target.
3. **Counter layer** — detect opponent telegraphs (jump-reset stance → S-tap or delayed sprint hit; falling → hold the hit; backing out → lunge/follow-up sweep; airborne crit → falling sprint hit / crit deflect).
4. **Combo sustain** — W-tap cadence tuned to their landing frames; read their back-out and switch (pausing W-tap → pausing W-tap slower; S-tap vs jump-resetters; hop/lunge alternation vs S-holders).
5. **Priority engine** — HP advantage → sprint hits + reactive jumps, no leading crits. HP disadvantage → long-range crits/baits. Unknown opponent → crit deflect first to gather data, then switch.

---

## 13. Common mistakes checklist

- Over-committing the opening (no pot to save you).
- Holding W instead of W-tapping → capped KB, no combos.
- Panic-clicking while comboed instead of jump-resetting / S-holding.
- KB-hitting a falling opponent (golden rule violation).
- Fighting with a wall behind you (removes your own escape).
- Ignoring spacing — chasing into their range instead of owning the ~3-block ring.
- Predictable double crits / jump resets / double jumps (all get countered hard).
- Exchanging sprint hits from behind.

---

## 14. Glossary

- **KB** — knockback.
- **Stun** — a sprint hit that knocks them out of hitting-you-back range; the unit combos are made of.
- **Sprint hit** — hit landed while sprinting (only the first after sprint-reset does full KB).
- **Sweep** — hit without sprint reset; accidental (bad) or deliberate (mixup).
- **W-tap / S-tap** — sprint reset methods; S-tap = more stun, no follow-up.
- **Three-block** — hitting from beyond their reach to start a trade (aim head, ~3 blocks).
- **Deflection** — sprint-hit immediately after their attack.
- **Reverse deflection** — sprint-hit immediately after their deflection.
- **Jump reset** — jump on the hit frame to cut horizontal KB.
- **Punish crit (P crit)** — crit generated from their knockback.
- **No-KB crit** — crit then hold W (don't jump) to reduce taken KB.
- **Double crit** — punish crit into a second immediate crit.
- **Standing crit** — sprint hit → jump → crit on the way down.
- **Peak crit** — crit timed waiting for their hit (slow, but closes distance).
- **S crit** — retreating crit while holding S.
- **Crit deflect** — sprint hit right after they land a crit.
- **Hops / lunges / uppercut / falling sprint hit** — jump-based movement/hits (see §2, §8).
- **Tick (kit)** — sub-90% charge hit, used to freeze KB / chip.
- **Faders** — opponents who "fade" (retreat) — falling sprint hits outreach them.
