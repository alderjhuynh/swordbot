package com.aura.swordbotv2.client.bot;

import java.util.Random;
import net.minecraft.world.phys.Vec3;

final class MeleeBrain {
    private static final int STRAFE_FLIP_TICKS = 14;
    private static final double CONFIRM_REACH = 3.0;
    private static final double GHOST_SANITY = 4.2;
    /** v47 Opus two-tier (attack_rule verbatim): inreach accrues on reach. */
    private static final double INREACH_RANGE = 3.0;
    /** v47 commit: post-step ray <= 2.999m AND reach_me <= 2.7m. */
    private static final double COMMIT_RAY = 2.999;
    private static final double COMMIT_REACH = 2.7;
    /** v47 crosshair fallback: same 2.7m band when the ray glances. */
    private static final double COMMIT_PICK = 2.7;
    /** v47 t_full==1 proxy: strength this high is ~1 tick from full. */
    private static final double FULL_SOON_STR = 0.96;
    /** v46 foe-KB suppression radius: no jump-offs into a set KB foe. */
    private static final double FOE_KB_SUPPRESS_RANGE = 6.0;
    /** v46 combo window: planted W+sprint ticks after our land (finisher). */
    private static final int COMBO_TICKS = 4;
    private static final int HOP_QUIET_TICKS = 16;
    private static final double HUG_RANGE = 1.8;
    private static final double TRANSIT_RANGE = 2.5;
    private static final double TRANSIT_JUMP = 4.5;
    private static final double PICK_SANITY = 3.5;
    private static final double PUNISH_RANGE = 7.0;
    private static final double PUNISH_SPEED = 0.10;
    private static final double CRIT_JUMP_STR = 0.45;

    private final Random random = new Random();
    private int inreachTicks;
    private int thinkId;
    private int plan;
    private int planStep;
    private int planLen;
    private int strafeDir = 1;
    private int strafeFlip;
    private int lastJumpAge = -100000;
    private int lastGate = 6;
    private boolean punishing;
    private double lastFoeHp = -1;
    private int lastHitDealtAge = -100000;
    private int hygieneStreak;
    private boolean lastBrawl;
    private boolean lastLosing;
    /** v46 rising edge: single click per commit, ticker gets a tick to fall. */
    private int lastSwingAge = -100000;

    void tick(SenseState sense, BotConfig config, Aim aim, Intents out, int age,
            boolean crosshairOn, KitProfile kit) {
        out.clear();
        lastGate = 6;
        punishing = false;
        if (sense.target == null || !sense.targetVisible) {
            inreachTicks = 0;
            lastFoeHp = -1;
            lastBrawl = false;
            lastLosing = false;
            return;
        }

        boolean replan = (age + thinkId) % Math.max(1, config.skill.thinkTicks) == 0;
        if (sense.hdist <= 4.5 && config.skill.thinkTicks <= 1) {
            replan = true;
        }
        if (sense.hurtThisTick || planStep >= planLen) {
            replan = true;
        }
        if (sense.targetVelEma.lengthSqr() > 0 && !sense.target.onGround()) {
            replan = true;
        }
        if (replan) {
            replan(sense, config, age);
        }
        planStep++;

        punishing = sense.hdist < PUNISH_RANGE && sense.targetSpeed < PUNISH_SPEED;
        boolean foeWeak = sense.targetHealth <= 8.0;
        // Kit behavior set (v36): the KB sword is a displacement tool, not a
        // combo/crit weapon. While it is wielded, plant feet and maximize
        // rate + knockback (sprint-hit); all crit phasing belongs to plain.
        boolean kb = kit != null && kit.kbMode();
        // Brawl mode (v42, combo escape): hurt within the last 6t with the foe
        // still in string range (<=4.5m) means THEIR combo is ongoing. All
        // quality waits below are skipped — swing the first available tick and
        // let the knockback break their string (displacement > damage when a
        // single extra traded hit kills us). Beyond 4.5m the string is already
        // broken by distance, so normal (crit-seeking) logic resumes.
        boolean brawl = age - sense.lastHurtAge <= 6 && sense.hdist <= 4.5;
        lastBrawl = brawl;
        // Losing the exchange (v43): brawling + foe healthy + nothing landed
        // in a full cycle. Our returns aren't connecting (or aren't happening)
        // while theirs are — staying trades HP we don't have. Answer: ride
        // the KB OUT instead of resetting back IN (see retreat below). Still
        // swinging (fighting retreat); a land, a weak foe, space, or time all
        // end it by construction. First blood also qualifies (no land yet):
        // aborting a stuffed approach early beats starting a spiral.
        boolean losing = brawl && sense.targetHealth > 8.0
                && age - lastHitDealtAge > 12;
        lastLosing = losing;

        // v47 Opus two-tier (attack_rule verbatim): inreach accrues on
        // reach_me <= 3.0m alone; the swing commits on post-step planned-ray
        // <= 2.999m AND reach_me <= 2.7m. Crosshair is a fallback for the
        // same 2.7m band when the EMA-lead ray glances — never a veto.
        // (v46 crosshair-only was stricter than the benchmark and collapsed
        // volume into noconf; this restores Opus trust in the planned ray.)
        // Look-before-decide kept: the ray uses predictStep angles Motor is
        // about to apply, not raw want.
        float[] stepped = aim.predictStep(config);
        Vec3 lead = sense.leadFeet(Aim.LEAD_TICKS, Aim.LEAD_MAX);
        double rayT = lead == null ? Double.POSITIVE_INFINITY
                : Aim.rayBox(sense.eyePos, stepped[0], stepped[1], lead,
                        sense.targetWidth, sense.targetHeight);
        boolean rayConfirm = rayT <= CONFIRM_REACH && sense.reachMe <= GHOST_SANITY;
        boolean pickConfirm = crosshairOn && sense.reachMe <= PICK_SANITY;
        boolean confirm = rayConfirm || pickConfirm;
        // v49 point-blank veto: with the eye inside the foe's box column
        // the closest-point clamp degenerates (aim point ~= eye => yaw 0 /
        // pitch +-90 garbage) while rayBox from inside still returns t=0.
        // Trusting that ray whiffs every recharge cycle into a 60t hug
        // stalemate (live v48: range 0.0-0.8, str churning 68->20, foe
        // static). Inside the column the ray is unknown — crosshair only.
        double hxHalf = sense.targetWidth / 2.0;
        boolean eyeInsideColumn = sense.hdist < hxHalf + 0.1
                && sense.eyePos.y > sense.targetPos.y - 0.2
                && sense.eyePos.y < sense.targetPos.y + sense.targetHeight + 0.2;
        boolean rayCommit = !eyeInsideColumn && rayT <= COMMIT_RAY
                && sense.reachMe <= COMMIT_REACH;
        boolean pickCommit = crosshairOn && sense.reachMe <= COMMIT_PICK;
        boolean commitConfirm = rayCommit || pickCommit;
        if (sense.reachMe <= INREACH_RANGE) {
            inreachTicks++;
        } else {
            inreachTicks = 0;
        }

        // Walk-band transit (v39): unconfirmed mid-range is approached on FOOT.
        // Sprint-jumping only pays past 4.5m (genuine chase, shared with
        // PursueBrain). Inside 2.5-4.5m, sprinting re-presses the sprint key
        // every unconfirmed tick (live v38: flag on at 100% of swings), which
        // both latches the flag (starving holds/crits) and arrives airborne
        // into the foe's turn-and-hit — the exact arrival Opus's chase refuses
        // ("a jumper arriving in range is hit in the air"). Walking arrives
        // grounded (jump-reset live), charged (ticker untouched by feet), flag
        // off (crit chain armed), crosshair stable. Pursue (far/opening) keeps
        // full sprint: catching a runner still wants the flag on for the
        // sprint-hit that stops the re-escape.
        // v46 foe-KB suppression (§7.3 drafted): into a set KB foe inside
        // 6m, arrive grounded — a transit jump is a free juggle starter.
        // Walk (no sprint, no jump) so the flag decays and the arrival is
        // reset-ready; genuine far chase past 6m still sprint-jumps.
        boolean foeKbClose = sense.foeKbLevel > 0 && sense.hdist <= FOE_KB_SUPPRESS_RANGE;
        // v48 deny-the-opener: Opus chase jumps ONLY when the ~12t hop lands
        // outside 3.6m ("a jumper arriving in range is hit in the air").
        // v46 gated this on KB foes only; live shows PLAIN Opus punishes
        // airborne arrivals identically, so the rule is universal now.
        double hopClosing = 0.35 + Math.max(0, sense.radialVel);
        boolean hopLandsOutside = sense.hdist - 12.0 * hopClosing >= 3.6;
        boolean transit = !confirm && sense.hdist > TRANSIT_RANGE;
        if (transit) {
            if (foeKbClose) {
                out.set(1, 0, false, false);
            } else if (sense.hdist > TRANSIT_JUMP) {
                out.set(1, 0, false, true);
                if (sense.onGround && hopLandsOutside) {
                    out.jump = true;
                }
            } else if (sense.radialVel < -0.1) {
                // Opening retreat (v41): the foe is backing out faster than a
                // walk closes (their recharge kiting — optimal lives here),
                // so match them on foot: SPRINT, no jump. Grounded keeps the
                // arrival reset-ready and unstuffable; the rebuilt flag turns
                // the catch exchange into sprint-hits that stop the NEXT
                // escape. Same -0.1 threshold as PursueBrain's opening.
                out.set(1, 0, false, true);
            } else if (age - lastHitDealtAge > 40 && age - sense.lastHurtAge > 40) {
                // v49 pristine opening: nothing landed or taken for 40t+
                // (round start, long chase) — sprint the final 2m instead of
                // walking them. The walk band (v39) costs 2-4t on the last
                // meters vs Opus's full-sprint close, donating first-hit
                // (live: Opus lands 2 before our first TAKES). No jump: the
                // hop rule above already refuses it inside 4.5m. Mid-fight
                // neutrals keep the walk (flag off, crit armed).
                out.set(1, 0, false, true);
            } else {
                out.set(1, 0, false, false);
            }
        } else {
            int effPlan = (punishing || foeWeak || sense.hdist > HUG_RANGE) ? 0 : plan;
            switch (effPlan) {
                case 1 -> out.set(0, 0, false, false);
                default -> out.set(1, 0, false, false);
            }
            if (!punishing && !kb && !brawl && confirm && sense.onGround
                    && sense.strength > CRIT_JUMP_STR && sense.strength < 0.95
                    && sense.hdist >= 1.0 && sense.hdist <= 2.8
                    && age - lastJumpAge >= HOP_QUIET_TICKS) {
                out.jump = true;
                lastJumpAge = age;
            }
            // strafe discipline (v37, mirrors Opus k_s=0 in melee): lateral
            // motion swings the crosshair off at range and flickers confirm.
            // Strafe only on SOLID confirm (2+ consecutive ticks — the same
            // ticks we'd swing on) in true pocket (<=3.0m), or point-blank
            // (hug) for the dodge. Otherwise feet planted, crosshair glued.
            boolean strafeOk = (confirm && inreachTicks >= 2 && sense.hdist <= 3.0)
                    || sense.hdist <= HUG_RANGE;
            if (strafeOk) {
                strafeFlip++;
                if (strafeFlip >= STRAFE_FLIP_TICKS) {
                    strafeFlip = 0;
                    strafeDir *= -1;
                }
                out.s = strafeDir;
                out.sprint = false;
            }
            // v49 separation: unconfirmed inside 0.9m means the geometry is
            // degenerate (see veto above) — pushing W just grinds boxes.
            // Step OUT to 1m+ where aim/ray/crosshair agree again. Costs 2-3t;
            // the 60t hug stalemate costs everything. Confirmed trades stay.
            if (!commitConfirm && sense.hdist < 0.9) {
                out.f = -1;
                out.s = 0;
                out.sprint = false;
            }
            if (out.f == 0 && out.s == 0) {
                out.f = 1;
            }
        }
        // post-land reset (v37, mirrors Opus plan16/fam5: back 4t, then jump
        // re-engage): we just landed (foe in i-frames, our charge resetting —
        // dead time), foe healthy, pocket range: step OUT to break their
        // confirm/aim, then walk back in. Costs nothing inside i-frames. Never
        // backs off a finisher (foe weak), a stationary foe (free hits), or
        // with KB (they are already gone; re-approach costs more than saved).
        if (!kb && !punishing && sense.targetHealth > 8.0
                && age - lastHitDealtAge <= 3 && sense.hdist <= 3.0) {
            out.f = -1;
            out.s = 0;
            out.sprint = false;
        }
        // v46 combo-window strafe suppression (finisher side of the backoff):
        // we just landed and the foe is weak or stationary — kill pressure
        // beats spacing. Plant W+sprint, drop strafe for COMBO_TICKS so the
        // sprint flag survives mid-trade and follow-ups go out as sprint-hit
        // chains instead of strafe-broken neutrals. Healthy-mobile foes keep
        // the backoff above; KB mode already sprint-hits by construction.
        if (!losing && !punishing && sense.targetHealth <= 8.0
                && age - lastHitDealtAge <= COMBO_TICKS && sense.hdist <= 3.0) {
            out.f = 1;
            out.s = 0;
            out.sprint = true;
        } else if (!losing && punishing && age - lastHitDealtAge <= COMBO_TICKS
                && sense.hdist <= 3.0) {
            out.f = 1;
            out.s = 0;
            out.sprint = true;
        }
        if (!sense.sprinting) {
            hygieneStreak = 0;
        }
        // Jump reset (v48: first pop always fires): the knockback just
        // arrived while grounded + pressing W toward the foe => sprint-jump
        // NOW, converting horizontal KB into a vertical pop-up that feeds the
        // pcrit chain. Opus gates on hurt==10 && gnd==1 && spr==1 && k_f==1;
        // v47 mirrored that including the sprint flag, but Aura walks the
        // pocket (flag off) so the reset was dead exactly when the opener
        // lands — v48 drops the sprint requirement on the immediate edge:
        // hurtThisTick + grounded + forward resets unconditionally, even
        // while losing. The stale touchdown edge (justLanded within 5t, hurt
        // came mid-air) keeps the v43 losing skip: re-jumping a sustained
        // lost exchange re-feeds it; that case belongs to the retreat below.
        // Takes precedence over hygiene.
        boolean resetJump = false;
        if (sense.onGround && out.f > 0 && (sense.hurtThisTick
                || (!losing && sense.justLanded && age - sense.lastHurtAge <= 5))) {
            out.set(1, 0, false, true);
            out.jump = true;
            resetJump = true;
        }
        // v47 jump-late (Opus kblate/pgnd): the hit landed after our motor
        // ran last tick, so we are airborne NOW but were grounded then. No
        // jump key helps mid-air — drift IN (W+sprint, Opus air k_f=1) to
        // land fast and stay in range instead of backpedalling through the
        // juggle with 0.02 air control. Suppresses the retreat below while
        // airborne on a fresh hurt.
        boolean jumpLateDrift = !sense.onGround && sense.prevGround && sense.sprinting
                && age - sense.lastHurtAge <= 1;
        if (jumpLateDrift && !resetJump) {
            out.f = 1;
            out.sprint = true;
            out.jump = false;
        }
        // Fighting retreat (v43, v47 grounded-only, v48 sustained-only):
        // losing => let the KB carry us OUT — but hold ground for ~2t after
        // a fresh hurt so the first-pop reset above (and the touchdown edge)
        // gets its chance. Retreating on pop #1 opened range the foe walks
        // down for free; retreating on pop #2 with no land breaks a real
        // string. Diagonal withdrawal, flag decays, retreat swings stay live.
        boolean sustainedLosing = losing && age - sense.lastHurtAge >= 2;
        // Telemetry tracks actual retreats (sustained), not the v43 state.
        lastLosing = sustainedLosing;
        if (sustainedLosing && sense.onGround && !jumpLateDrift) {
            out.f = -1;
            out.s = strafeDir;
            out.sprint = false;
        }

        // v47 commit gate (two-tier): steering confirm keeps feet alive
        // above; the swing needs rayCommit (post-step ray <= 2.999m AND
        // reach <= 2.7m, Opus attack_rule) or the same-band crosshair
        // fallback. Ray-true/pick-false ticks steer without spending ticker.
        if (!commitConfirm) {
            lastGate = 7;
            return;
        }
        // v46 rising edge: one click per commit. The ticker needs a tick to
        // fall after a swing; a stale full-strength read must not double-click
        // into a reset.
        if (age - lastSwingAge <= 1) {
            lastGate = 4;
            return;
        }
        if (inreachTicks <= config.skill.reactTicks) {
            lastGate = 4;
            return;
        }
        if (sense.strength <= 0.9) {
            lastGate = 2;
            return;
        }
        // i-frame guard, anchored on *landed* hits (mirrors Opus attack_rule and
        // sim/melee_click): a foe hurt flash only blocks us when the last
        // damage was our own recent hit. Whiffs and other sources' flashes let
        // real hits through. Deliberately no extra land-debounce: hurtTime IS
        // the i-frame clock, so any further shadow is pure idle at full charge
        // (live finding: ready_idle ~90%, str>=92 + foeHurt=0 + gate=3).
        if (sense.targetHealth < lastFoeHp - 0.001) {
            lastHitDealtAge = age;
        }
        lastFoeHp = sense.targetHealth;
        if (sense.targetHurtTime > 0 && age - lastHitDealtAge <= 10) {
            lastGate = 3;
            return;
        }
        // rise hold (v34): charged (>0.9, see above) + rising + sprint off +
        // foe's swing recently spent => wait for the falling crit instead of
        // swinging into the rise. Extends Opus's 1t crit-wait across the whole
        // ascent. Unsafe (foe ready) => fall through and swing now.
        // NOTE: unreachable while the client sprint flag sticks on, which is
        // why the hygiene cut below must run first (live v34: hold fired
        // 13/1638t with the flag on at 6/6 sampled swings).
        if (!kb && !brawl && !sense.onGround && sense.fallDistance <= 0
                && !sense.sprinting && opponentReadyIn(sense, age) >= 2) {
            lastGate = 4;
            return;
        }
        // sprint-flag hygiene (v35 refined v38, mirrors Opus air.mcfunction's
        // W-release): the attack packet leaves before the STOP_SPRINT packet
        // when both are generated on the swing tick, so any click released
        // while the sprint flag is on resolves server-side as a sprint-hit
        // (+KB, never a crit), and sticky vanilla sprint keeps the flag on
        // once we ever sprint with W held. v38 doctrine: the flag is now a
        // FEATURE (combo pressure — see commit below), so cut it ONLY for a
        // real crit: airborne + charged + foe on cooldown. Grounded imminent
        // swings go through WITH the flag (sprint-hit, combo continues).
        // Unsafe (foe ready) => swing now, disruption over quality.
        // Streak-capped: a stuck flag degrades to sprint-hits, never a stall.
        // Skipped on jump-reset ticks (reset wins) and with KB (flag wanted).
        if (!kb && !brawl && !resetJump && sense.sprinting && sense.strength > 0.85
                && !sense.onGround && opponentReadyIn(sense, age) >= 2) {
            if (hygieneStreak < 3) {
                hygieneStreak++;
                out.set(0, 0, false, false);
                out.jump = false;
                lastGate = 4;
                return;
            }
        }
        // Greedy full-strength wait (v47 Opus t_full==1 semantics): Opus
        // waits only when full strength is ONE tick away (t_full==1) with no
        // crit available — never a multi-tick idle. v42-46 waited on ANY
        // sub-1.0 strength, donating 2-5t per cycle. FULL_SOON_STR ~= 1 tick
        // from full for an iron-class cooldown. Skipped while brawling.
        if (!brawl && opponentReadyIn(sense, age) >= 2) {
            if (!sense.critOk && sense.strength < 1.0
                    && sense.strength >= FULL_SOON_STR) {
                lastGate = 4;
                return;
            }
        }
        out.attack = true;
        lastGate = 0;
        hygieneStreak = 0;
        lastSwingAge = age;
        // W-tap combo (v38): preserve the live sprint flag through the click
        // instead of forcing it off. Flag on (combo pressure, post-chase) =>
        // sprint-hit: KB/2 + 0.5 bonus knockback that chains the combo, exactly
        // like Opus on_attack (W + sprint held). Flag off (post-release,
        // post-hygiene) => neutral/crit swing with the full crit chain intact.
        // The rhythm self-alternates: land => backoff clears flag => strafe
        // pocket => hop/hold => crit => land ... vs chase => flag on =>
        // sprint-hit chains. KB mode always sprint-hits (displacement tool).
        commitAttack(out, kb || sense.sprinting);
    }

    private void commitAttack(Intents out, boolean sprintHit) {
        if (sprintHit) {
            // Combo / KB click, mirrors Opus on_attack: keep W + sprint held
            // for momentum (reach) and maximum knockback. Strafe is dropped on
            // the click so the sprint key engages.
            out.f = 1;
            out.s = 0;
            out.sprint = true;
        } else {
            if (out.s == 0) {
                out.f = 1;
            }
            out.sprint = false;
        }
        out.jump = false;
    }

    void onAir(SenseState sense, Intents out, int age) {
        out.f = 1;
        out.jump = false;
    }

    private void replan(SenseState sense, BotConfig config, int age) {
        thinkId = (thinkId + 1) & 0xFF;
        double aggr = config.style.aggression / 100.0;
        double r = random.nextDouble();
        if (sense.hdist > HUG_RANGE) {
            plan = 0;
        } else if (r < aggr) {
            plan = 0;
        } else {
            plan = 1;
        }
        if (config.skill.mistakePerMille > 0
                && random.nextInt(1000) < config.skill.mistakePerMille) {
            plan = random.nextInt(2);
        }
        planStep = 0;
        planLen = Math.max(1, config.skill.thinkTicks * 2);
    }

    int plan() {
        return plan;
    }

    int gate() {
        return lastGate;
    }

    boolean punishing() {
        return punishing;
    }

    boolean brawling() {
        return lastBrawl;
    }

    boolean losing() {
        return lastLosing;
    }

    static int opponentReadyIn(SenseState sense, int age) {
        // Most recent evidence the foe swung: their arm-swing edge (true
        // cooldown clock, v37) or our hurt tick (old proxy). Unobserved (both
        // init) => 0, foe assumed ready — identical to the old behavior.
        int ref = Math.max(sense.lastHurtAge, sense.foeSwingAge);
        return Math.max(0, 11 - (age - ref));
    }

    void reset() {
        inreachTicks = 0;
        plan = 0;
        planStep = 0;
        planLen = 1;
        strafeDir = 1;
        strafeFlip = 0;
        lastGate = 6;
        punishing = false;
        lastFoeHp = -1;
        lastHitDealtAge = -100000;
        hygieneStreak = 0;
        lastBrawl = false;
        lastLosing = false;
        lastSwingAge = -100000;
    }
}
