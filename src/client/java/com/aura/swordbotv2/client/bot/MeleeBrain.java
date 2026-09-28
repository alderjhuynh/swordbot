package com.aura.swordbotv2.client.bot;

import java.util.Random;
import net.minecraft.world.phys.Vec3;

final class MeleeBrain {
    private static final int STRAFE_FLIP_TICKS = 14;
    private static final double CONFIRM_REACH = 3.0;
    private static final double GHOST_SANITY = 4.2;
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

    void tick(SenseState sense, BotConfig config, Aim aim, Intents out, int age,
            boolean crosshairOn) {
        out.clear();
        lastGate = 6;
        punishing = false;
        if (sense.target == null || !sense.targetVisible) {
            inreachTicks = 0;
            lastFoeHp = -1;
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

        Vec3 lead = sense.leadFeet(Aim.LEAD_TICKS, Aim.LEAD_MAX);
        double rayT = lead == null ? Double.POSITIVE_INFINITY
                : Aim.rayBox(sense.eyePos, aim.wantYaw, aim.wantPitch, lead,
                        sense.targetWidth, sense.targetHeight);
        boolean rayConfirm = rayT <= CONFIRM_REACH && sense.reachMe <= GHOST_SANITY;
        boolean pickConfirm = crosshairOn && sense.reachMe <= PICK_SANITY;
        boolean confirm = rayConfirm || pickConfirm;
        if (confirm) {
            inreachTicks++;
        } else {
            inreachTicks = 0;
        }

        boolean transit = !confirm && sense.hdist > TRANSIT_RANGE;
        if (transit) {
            out.set(1, 0, false, true);
            if (sense.hdist > TRANSIT_JUMP && sense.onGround) {
                out.jump = true;
            }
        } else {
            int effPlan = (punishing || foeWeak || sense.hdist > HUG_RANGE) ? 0 : plan;
            switch (effPlan) {
                case 1 -> out.set(0, 0, false, false);
                default -> out.set(1, 0, false, false);
            }
            if (!punishing && confirm && sense.onGround
                    && sense.strength > CRIT_JUMP_STR && sense.strength < 0.95
                    && sense.hdist >= 1.0 && sense.hdist <= 4.5
                    && age - lastJumpAge >= HOP_QUIET_TICKS) {
                out.jump = true;
                lastJumpAge = age;
            }
            if (confirm || sense.hdist <= HUG_RANGE) {
                strafeFlip++;
                if (strafeFlip >= STRAFE_FLIP_TICKS) {
                    strafeFlip = 0;
                    strafeDir *= -1;
                }
                out.s = strafeDir;
                out.sprint = false;
            }
            if (out.f == 0 && out.s == 0) {
                out.f = 1;
            }
        }
        if (!sense.sprinting) {
            hygieneStreak = 0;
        }
        boolean resetJump = false;
        if (sense.hurtThisTick && sense.onGround && out.f > 0) {
            out.set(1, 0, false, true);
            out.jump = true;
            resetJump = true;
        }

        if (!confirm) {
            lastGate = 7;
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
        if (!sense.onGround && sense.fallDistance <= 0 && !sense.sprinting
                && opponentReadyIn(sense, age) >= 2) {
            lastGate = 4;
            return;
        }
        // sprint-flag hygiene (v35, mirrors Opus air.mcfunction's W-release):
        // the attack packet leaves before the STOP_SPRINT packet when both are
        // generated on the swing tick, so any click released while the sprint
        // flag is on resolves server-side as a sprint-hit (+KB, never a crit),
        // and sticky vanilla sprint keeps the flag on once we ever sprint with
        // W held. If a swing is otherwise imminent and the flag is still on,
        // cut W + sprint for one tick WITHOUT swinging so STOP precedes the
        // click. Streak-capped: a stuck flag degrades to non-crits, never to a
        // full stall. Skipped on jump-reset ticks (reset wins).
        if (!resetJump && sense.sprinting && sense.strength > 0.85) {
            if (hygieneStreak < 3) {
                hygieneStreak++;
                out.set(0, 0, false, false);
                out.jump = false;
                lastGate = 4;
                return;
            }
        }
        if (opponentReadyIn(sense, age) >= 2) {
            if (sense.strength < 1.0 && !sense.critOk) {
                lastGate = 4;
                return;
            }
        }
        out.attack = true;
        lastGate = 0;
        hygieneStreak = 0;
        commitAttack(out);
    }

    private void commitAttack(Intents out) {
        if (out.s == 0) {
            out.f = 1;
        }
        out.sprint = false;
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

    static int opponentReadyIn(SenseState sense, int age) {
        return Math.max(0, 11 - (age - sense.lastHurtAge));
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
    }
}
