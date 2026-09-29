package com.aura.swordbotv2.client.bot;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class FightLogger {
    private static final int TRACE_EVERY = 10;
    private static final int TRACE_KEEP = 60;
    private static final int TIMEOUT_TICKS = 3600;

    private boolean open;
    private int idx;
    private long t0ms;
    private int ticks;
    private int swings;
    private int hits;
    private double dmgDealt;
    private int taken;
    private double dmgTaken;
    private int eatTicks;
    private int eatSessions;
    private boolean wasEating;
    private int rangeTicks;
    private double rangeSum;
    private int rangeCount;
    private int gapsStart;
    private int foodStart;
    private String foe = "?";
    private String weapon = "?";
    private String skill = "?";
    private String style = "?";
    private double lastFoeHp = -1;
    private double lastMyHp = -1;
    private double lastReach = -1;
    private int lastHurt = -1;
    private int sinceTrace;
    private LivingEntity tracked;
    private final List<String> trace = new ArrayList<>();
    private final int[] gates = new int[8];
    private int airTicks;
    private int strafeTicks;
    private int sprintTicks;
    private int readyTicks;
    private int readyIdleTicks;
    private int airSwings;
    private int fallSwings;
    private int kbSwings;
    private int sprSwings;
    private int brawlTicks;
    private int retreatTicks;
    private String kit = "?";
    private int kbLvl;

    boolean begin(Minecraft client, LivingEntity foeEnt, BotConfig config, int age,
            KitProfile profile, String endReason) {
        close(client, endReason);
        try {
            if (client == null || client.player == null || !client.player.isAlive()
                    || client.player.getHealth() <= 0 || foeEnt == null
                    || !foeEnt.isAlive()) {
                return false;
            }
            open = true;
            idx++;
            t0ms = System.currentTimeMillis();
            ticks = 0;
            swings = 0;
            hits = 0;
            dmgDealt = 0;
            taken = 0;
            dmgTaken = 0;
            eatTicks = 0;
            eatSessions = 0;
            wasEating = false;
            rangeTicks = 0;
            rangeSum = 0;
            rangeCount = 0;
            gapsStart = countGaps(client);
            foodStart = countFood(client);
            tracked = foeEnt;
            foe = foeEnt.getName().getString();
            weapon = heldId(client);
            skill = config.skill.name();
            style = config.style.name();
            lastFoeHp = foeEnt.getHealth();
            lastMyHp = myHp(client);
            lastReach = -1;
            lastHurt = client.player.hurtTime;
            sinceTrace = 0;
            trace.clear();
            java.util.Arrays.fill(gates, 0);
            airTicks = 0;
            strafeTicks = 0;
            sprintTicks = 0;
            readyTicks = 0;
            readyIdleTicks = 0;
            airSwings = 0;
            fallSwings = 0;
            kbSwings = 0;
            sprSwings = 0;
            brawlTicks = 0;
            retreatTicks = 0;
            kit = profile == null ? "?" : profile.kitName();
            kbLvl = profile == null ? 0 : profile.kbLevel;
            return true;
        } catch (Exception ignored) {
            open = false;
            return false;
        }
    }

    boolean isOpen() {
        return open;
    }

    boolean tracks(LivingEntity ent) {
        return open && tracked == ent;
    }

    void onSwing() {
        if (open) {
            swings++;
        }
    }

    void onTick(Minecraft client, SenseState sense, Intents in, int gate, String mode,
            boolean kbWielded, boolean brawling, boolean losing) {
        if (!open) {
            return;
        }
        try {
            ticks++;
            // damage taken
            if (client.player != null) {
                double my = myHp(client);
                if (lastMyHp >= 0 && my < lastMyHp) {
                    dmgTaken += lastMyHp - my;
                }
                lastMyHp = my;
                int hurt = client.player.hurtTime;
                if (lastHurt == 0 && hurt > 0) {
                    taken++;
                }
                lastHurt = hurt;
            }
            // damage landed
            if (tracked != null) {
                double fh = tracked.getHealth();
                double reachNow = sense.valid ? sense.reachMe : -1;
                if (lastFoeHp >= 0 && lastReach >= 0 && sense.valid
                        && fh - lastFoeHp > 5.0
                        && Math.abs(reachNow - lastReach) > 3.0) {
                    lastFoeHp = fh;
                    lastReach = reachNow;
                    close(client, "round");
                    return;
                }
                if (lastFoeHp >= 0 && fh < lastFoeHp) {
                    hits++;
                    dmgDealt += lastFoeHp - fh;
                }
                lastFoeHp = fh;
                if (sense.valid) {
                    lastReach = reachNow;
                }
            }
            // range profile
            if (sense.target != null && sense.valid) {
                rangeSum += sense.reachMe;
                rangeCount++;
                if (sense.reachMe <= 3.0) {
                    rangeTicks++;
                }
            }
            // eating sessions
            boolean eating = in.use && holdingFood(client);
            if (eating) {
                eatTicks++;
                if (!wasEating) {
                    eatSessions++;
                }
            }
            wasEating = eating;
            // gate + motion profile
            if (gate >= 0 && gate < gates.length) {
                gates[gate]++;
            }
            if (sense.valid && !sense.onGround) {
                airTicks++;
            }
            if (in.s != 0) {
                strafeTicks++;
            }
            if (in.sprint) {
                sprintTicks++;
            }
            // crosshair is on the tracked target and vanilla cooldown reads full, but we are NOT swinging this tick
            try {
                boolean xhair = sense.target != null && tracked == sense.target
                        && client.crosshairPickEntity == sense.target;
                boolean full = sense.valid && sense.strength >= 0.999;
                if (xhair && full) {
                    readyTicks++;
                    if (!in.attack) {
                        readyIdleTicks++;
                    }
                }
            } catch (Exception ignored) {
            }
            // crit-attempt telem
            if (in.attack && sense.valid && !sense.onGround) {
                airSwings++;
                if (sense.fallDistance > 0) {
                    fallSwings++;
                }
            }
            if (in.attack && kbWielded) {
                kbSwings++;
            }
            // w-tap telemetry
            if (in.attack && sense.valid && sense.sprinting) {
                sprSwings++;
            }
            if (brawling) {
                brawlTicks++;
            }
            if (losing) {
                retreatTicks++;
            }
            // trace tail
            if (++sinceTrace >= TRACE_EVERY) {
                sinceTrace = 0;
                int flags = (eating ? 1 : 0) | (in.sprint ? 2 : 0)
                        | (in.attack ? 4 : 0)
                        | ((sense.valid && !sense.onGround) ? 8 : 0)
                        | (in.s != 0 ? 16 : 0);
                trace.add(ticks + "," + round1(sense.valid ? sense.reachMe : -1)
                        + "," + round1(lastMyHp) + "," + round1(lastFoeHp)
                        + "," + flags + "," + gate + "," + mode
                        + "," + sense.targetHurtTime + "," + lastHurt
                        + "," + Math.round(sense.strength * 100)
                        + "," + sense.food
                        + "," + (sense.valid && sense.sprinting ? 1 : 0)
                        + "," + Math.round(sense.radialVel * 100));
                if (trace.size() > TRACE_KEEP) {
                    trace.remove(0);
                }
            }
            if (tracked != null && (!tracked.isAlive() || tracked.isRemoved())) {
                close(client, "killed");
                return;
            }
            if (client.player != null
                    && (!client.player.isAlive() || client.player.getHealth() <= 0)) {
                close(client, "died");
                return;
            }
            if (ticks >= TIMEOUT_TICKS) {
                close(client, "timeout");
            }
        } catch (Exception ignored) {
        }
    }

    void abort(Minecraft client) {
        close(client, "aborted");
    }

    void closeRound(Minecraft client) {
        close(client, "round");
    }

    private void close(Minecraft client, String reason) {
        if (!open) {
            return;
        }
        open = false;
        try {
            File dir = new File(client.gameDirectory, "swordbot-telemetry");
            dir.mkdirs();
            double hpLeft = client.player == null ? -1 : round1(client.player.getHealth());
            double foeHp = tracked == null ? -1 : round1(tracked.getHealth());
            int gapsUsed = Math.max(0, gapsStart - countGaps(client));
            int foodUsed = Math.max(0, foodStart - countFood(client));
            StringBuilder sb = new StringBuilder();
            sb.append("{\"v\":2,\"idx\":").append(idx)
                    .append(",\"build\":\"").append(com.aura.swordbotv2.client.BuildInfo.ID)
                    .append("\"")
                    .append(",\"t0\":").append(t0ms)
                    .append(",\"ticks\":").append(ticks)
                    .append(",\"reason\":\"").append(reason).append("\"")
                    .append(",\"skill\":\"").append(skill).append("\"")
                    .append(",\"style\":\"").append(style).append("\"")
                    .append(",\"foe\":\"").append(esc(foe)).append("\"")
                    .append(",\"weapon\":\"").append(esc(weapon)).append("\"")
                    .append(",\"swings\":").append(swings)
                    .append(",\"hits\":").append(hits)
                    .append(",\"dmg_dealt\":").append(round1(dmgDealt))
                    .append(",\"taken\":").append(taken)
                    .append(",\"dmg_taken\":").append(round1(dmgTaken))
                    .append(",\"eat_ticks\":").append(eatTicks)
                    .append(",\"eat_sessions\":").append(eatSessions)
                    .append(",\"gaps_used\":").append(gapsUsed)
                    .append(",\"food_used\":").append(foodUsed)
                    .append(",\"range_ticks\":").append(rangeTicks)
                    .append(",\"avg_range\":")
                    .append(rangeCount > 0 ? round1(rangeSum / rangeCount) : -1)
                    .append(",\"hp_left\":").append(hpLeft)
                    .append(",\"foe_hp_left\":").append(foeHp)
                    .append(",\"gates\":[")
                    .append(gates[0]).append(",").append(gates[1]).append(",")
                    .append(gates[2]).append(",").append(gates[3]).append(",")
                    .append(gates[4]).append(",").append(gates[5]).append(",")
                    .append(gates[6]).append(",").append(gates[7]).append("]")
                    .append(",\"air_ticks\":").append(airTicks)
                    .append(",\"strafe_ticks\":").append(strafeTicks)
                    .append(",\"sprint_ticks\":").append(sprintTicks)
                    .append(",\"ready_ticks\":").append(readyTicks)
                    .append(",\"ready_idle_ticks\":").append(readyIdleTicks)
                    .append(",\"air_swings\":").append(airSwings)
                    .append(",\"fall_swings\":").append(fallSwings)
                    .append(",\"kb_swings\":").append(kbSwings)
                    .append(",\"spr_swings\":").append(sprSwings)
                    .append(",\"brawl_ticks\":").append(brawlTicks)
                    .append(",\"retreat_ticks\":").append(retreatTicks)
                    .append(",\"kit\":\"").append(kit).append("\"")
                    .append(",\"kb\":").append(kbLvl)
                    .append(",\"trace_tail\":[");
            for (int i = 0; i < trace.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append("\"").append(trace.get(i)).append("\"");
            }
            sb.append("]}\n");
            try (FileWriter w = new FileWriter(new File(dir, "fights.jsonl"), true)) {
                w.write(sb.toString());
            }
        } catch (IOException ignored) {
        } finally {
            tracked = null;
        }
    }

    private static double myHp(Minecraft client) {
        return client.player.getHealth() + client.player.getAbsorptionAmount();
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "'");
    }

    private static String heldId(Minecraft client) {
        try {
            ItemStack s = client.player.getInventory().getSelectedItem();
            if (s.isEmpty()) {
                return "empty";
            }
            return s.getItem().toString();
        } catch (Exception e) {
            return "?";
        }
    }

    private static boolean holdingFood(Minecraft client) {
        try {
            ItemStack s = client.player.getInventory().getSelectedItem();
            return !s.isEmpty() && s.getComponents().has(DataComponents.FOOD);
        } catch (Exception e) {
            return false;
        }
    }

    private static int countGaps(Minecraft client) {
        try {
            var inv = client.player.getInventory();
            int n = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty()
                        && (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE))) {
                    n += s.getCount();
                }
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int countFood(Minecraft client) {
        try {
            var inv = client.player.getInventory();
            int n = 0;
            for (int i = 0; i < 9; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && s.getComponents().has(DataComponents.FOOD)) {
                    n += s.getCount();
                }
            }
            return n;
        } catch (Exception e) {
            return 0;
        }
    }
}
