package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;

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

    private boolean enabled;
    private int age;

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
        targeting.tick(client, sense, config);
        sense.tick(client, targeting.locked(), age);
        if (!sense.valid) {
            return;
        }
        if (sense.target == null) {
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
            melee.tick(sense, config, aim, scratch, age, xhair);
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
                melee.tick(sense, config, aim, scratch, age, xhair);
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
                melee.tick(sense, config, aim, intents, age, xhair);
                gate = melee.gate();
                modeStr = "M" + melee.plan() + (melee.punishing() ? "P" : "");
            }
            if (sense.targetVisible && sense.reachOp <= 4.5 && bow.drawing()) {
                bow.stop(client);
            }
        }

        if (sense.target != null && !fights.tracks(sense.target)) {
            fights.begin(client, sense.target, config, age);
        }
        if (intents.attack && !RecoverBrain.ensureWeaponForAttack(client)) {
            intents.attack = false;
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
        fights.onTick(client, sense, intents, gate, modeStr);
        motor.apply(client, intents, aim, config, faceTarget);
        sense.endTick();
    }
}
