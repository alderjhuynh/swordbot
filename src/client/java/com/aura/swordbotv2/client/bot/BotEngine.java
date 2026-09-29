package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public final class BotEngine {
    private final BotConfig config = BotConfig.defaults();
    private final Targeting targeting = new Targeting();
    private final SenseState sense = new SenseState();
    private final Aim aim = new Aim();
    private final MeleeBrain melee = new MeleeBrain();
    private final PursueBrain pursue = new PursueBrain();
    private final RecoverBrain recover = new RecoverBrain();
    private final BowBrain bow = new BowBrain();
    private final Motor motor = new Motor();
    private final Intents intents = new Intents();
    private final Intents scratch = new Intents();
    private final FightLogger fights = new FightLogger();
    private final KitProfile kit = new KitProfile();

    private boolean enabled;
    private int age;
    private net.minecraft.world.entity.player.Player lastPlayer;

    public BotConfig config() {
        return config;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(Minecraft client, boolean on) {
        enabled = on;
        if (!on) {
            motor.releaseAll(client);
            melee.reset();
            recover.reset();
            bow.stop(client);
            fights.abort(client);
        } else {
            if (client != null && client.player != null) {
                aim.snap(client.player.getYRot(), client.player.getXRot());
                targeting.targetNearest(client, config.autoTargetRange);
                sense.clearTargetMemory();
            }
        }
    }

    public void retarget(Minecraft client) {
        targeting.targetNearest(client, config.autoTargetRange);
        sense.clearTargetMemory();
    }

    public String statusLine() {
        String t = sense.target == null ? "none"
                : sense.target.getName().getString()
                        + String.format(" %.1fb %s", sense.hdist,
                                sense.targetVisible ? "seen" : "hidden");
        return String.format("SwordBot %s skill=%s style=%s target=%s [%s]",
                enabled ? "ON" : "OFF", config.skill, config.style, t,
                com.aura.swordbotv2.client.BuildInfo.ID);
    }

    public void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        if (!enabled) {
            return;
        }
        age++;
        // Respawn/rel commute guard (v44 duel loop): immediate respawn swaps
        // the player entity. Snap the look so the first post-respawn tick
        // doesn't slew from a stale angle, and drop stale per-target memory.
        // The lock itself is kept (the duel bot persists across OUR death).
        if (client.player != lastPlayer) {
            lastPlayer = client.player;
            aim.snap(client.player.getYRot(), client.player.getXRot());
            sense.clearTargetMemory();
        }
        targeting.tick(client, sense, config);
        // v46 crosshair-vs-lock steal (before sense): if the vanilla
        // crosshair sits on an engageable foe that is NOT the lock while the
        // lock is missing/invisible/far, the lock is stale — steal it now so
        // sense/aim gate on the body the click would actually hit.
        try {
            var x = client.crosshairPickEntity;
            if (x instanceof LivingEntity xl && xl != targeting.locked()
                    && xl.isAlive() && Targeting.isEngageable(xl)
                    && !(xl instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
                var cur = targeting.locked();
                boolean stale = cur == null || !cur.isAlive()
                        || cur.distanceTo(client.player) > 8.0
                        || !sense.targetVisible || sense.reachMe > 3.5;
                if (stale && xl.distanceTo(client.player) <= config.autoTargetRange) {
                    targeting.steal(xl);
                    sense.onTargetChanged(xl);
                    sense.clearTargetMemory();
                }
            }
        } catch (Exception ignored) {
        }
        sense.tick(client, targeting.locked(), age);
        if (!sense.valid) {
            return;
        }
        // Kit behavior set (v36): inventory the hotbar on fight begin +
        // periodically; wield state refreshes every tick (trivial read) so
        // brains always know which doctrine is live.
        if (kit.needsScan(age)) {
            kit.rescan(client);
        }
        kit.refreshWield(client);
        // Duel-loop intermission (v44, fixed v45): no foe, OR the foe is ruled
        // out of play — glowing flag (referee countdown/intermission/round
        // end; the one signal that actually syncs), countdown tag, or
        // invulnerable (no damage possible either way). Stand down COMPLETELY:
        // a stuck key would walk us off the home marker during the countdown,
        // and brains must not shadow-box an invulnerable bot. An out-of-play
        // lock closes the open telemetry row as a finished round ("round") so
        // training gets one row per episode; the next lock re-opens fresh at
        // FIGHT. Plain target loss (LOS flicker) does NOT close the row —
        // only the referee's signals do.
        LivingEntity lock = targeting.locked();
        boolean sidelined = hasCountdownTag(lock) || hasGlowingTag(lock);
        if (sense.target == null || sidelined || sense.target.isInvulnerable()) {
            if (sidelined && lock != null && fights.tracks(lock)) {
                fights.closeRound(client);
            }
            motor.releaseAll(client);
            bow.stop(client);
            sense.endTick();
            return;
        }

        boolean faceTarget = sense.targetVisible;
        if (sense.targetVisible) {
            aim.compute(client, sense, config);
        }

        boolean xhair = sense.target != null
                && client.crosshairPickEntity == sense.target;
        int gate = 6;
        String modeStr = "M?";
        if (recover.tick(client, sense, config, intents, age)) {
            modeStr = "R" + recover.mode();
        } else if (bow.tick(client, sense, config, aim, intents, age)) {
            modeStr = "B";
        } else if (pursue.tick(sense, targeting, scratch)) {
            modeStr = "P";
            intents.f = scratch.f;
            intents.s = scratch.s;
            intents.jump = scratch.jump;
            intents.sprint = scratch.sprint;
            intents.attack = false;
            intents.use = false;
            melee.tick(sense, config, aim, scratch, age, xhair, kit);
            gate = melee.gate();
            if (scratch.attack) {
                intents.attack = true;
            }
        } else {
            if (!sense.targetVisible) {
                intents.clear();
                faceTarget = false;
            } else if (!sense.onGround) {
                modeStr = "MA";
                melee.tick(sense, config, aim, scratch, age, xhair, kit);
                gate = melee.gate();
                if (melee.punishing()) {
                    modeStr = "MAP";
                }
                intents.f = scratch.f;
                intents.s = scratch.s;
                intents.jump = false;
                intents.sprint = scratch.sprint;
                intents.attack = scratch.attack;
                intents.use = false;
                melee.onAir(sense, intents, age);
            } else {
                melee.tick(sense, config, aim, intents, age, xhair, kit);
                gate = melee.gate();
                modeStr = "M" + melee.plan() + (melee.punishing() ? "P" : "");
            }
            if (sense.targetVisible && sense.reachOp <= 4.5 && bow.drawing()) {
                bow.stop(client);
            }
        }

        if (sense.target != null && !fights.tracks(sense.target)) {
            kit.rescan(client);
            fights.begin(client, sense.target, config, age, kit, "switched");
        }
        if (intents.attack && !kit.ensureWeaponForAttack(client, sense, config)) {
            intents.attack = false;
        }
        if (!intents.attack) {
            kit.applyDoctrine(client, sense, config, recover, bow, intents);
        }
        if (intents.attack) {
            fights.onSwing();
        }
        if (gate == 6 && sense.target != null && sense.targetVisible) {
            if (intents.attack) {
                gate = 0;
            } else if (intents.use) {
                gate = 5;
            } else if (sense.strength <= 0.9) {
                gate = 2;
            } else if (sense.targetHurtTime > 0) {
                gate = 3;
            } else {
                gate = 4;
            }
        }
        fights.onTick(client, sense, intents, gate, modeStr, kit.wieldKb,
                melee.brawling(), melee.losing());
        motor.apply(client, intents, aim, config, faceTarget);
        sense.endTick();
    }

    /** Referee countdown tag (v44 duel loop): present = round not live. */
    private static boolean hasCountdownTag(LivingEntity ent) {
        if (ent == null) {
            return false;
        }
        try {
            return ent.entityTags().contains("opus.countdown");
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Referee glow flag (v45): the synced out-of-play signal. */
    private static boolean hasGlowingTag(LivingEntity ent) {
        if (ent == null) {
            return false;
        }
        try {
            return ent.hasGlowingTag();
        } catch (Exception ignored) {
            return false;
        }
    }
}
