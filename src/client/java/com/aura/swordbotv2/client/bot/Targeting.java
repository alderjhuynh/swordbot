package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Targeting {
    private LivingEntity locked;
    private Vec3 lastSeen = Vec3.ZERO;
    private boolean hasLastSeen;

    public LivingEntity locked() {
        return locked;
    }

    public Vec3 lastSeen() {
        return lastSeen;
    }

    public boolean hasLastSeen() {
        return hasLastSeen;
    }

    public void targetNearest(Minecraft client, double range) {
        locked = findNearest(client, range);
        if (locked != null) {
            lastSeen = locked.position();
            hasLastSeen = true;
        }
    }

    public void clear() {
        locked = null;
        hasLastSeen = false;
    }

    public void steal(LivingEntity next) {
        locked = next;
        if (locked != null) {
            lastSeen = locked.position();
            hasLastSeen = true;
        }
    }

    public void tick(Minecraft client, SenseState sense, BotConfig config) {
        if (client.player == null || client.level == null) {
            locked = null;
            return;
        }
        if (locked != null) {
            if (!locked.isAlive() || locked.isRemoved()
                    || locked.distanceTo(client.player) > 64.0) {
                locked = null;
            }
        }
        if (locked == null && config.autoTarget) {
            locked = findNearest(client, config.autoTargetRange);
        }
        sense.onTargetChanged(locked);
        if (locked != null && sense.targetVisible) {
            lastSeen = locked.position();
            hasLastSeen = true;
        }
    }

    static boolean isEngageable(LivingEntity ent) {
        if (ent == null) {
            return false;
        }
        try {
            if (ent.hasGlowingTag()) {
                return false;
            }
        } catch (Exception ignored) {
        }
        try {
            if (ent.entityTags().contains("opus.countdown")) {
                return false;
            }
        } catch (Exception ignored) {
        }
        try {
            if (ent.isInvulnerable()) {
                return false;
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private static LivingEntity findNearest(Minecraft client, double range) {
        if (client.player == null || client.level == null) {
            return null;
        }
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (var entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            if (living == client.player || !living.isAlive()) {
                continue;
            }
            // v46: armor stands are LivingEntity but never foes — grabbing
            // one wedges the lock into permanent gate 7 (§7 cause E).
            if (living instanceof net.minecraft.world.entity.decoration.ArmorStand) {
                continue;
            }
            if (!isEngageable(living)) {
                continue;
            }
            double d = living.distanceTo(client.player);
            if (d > range) {
                continue;
            }
            double score = d + (living instanceof Player ? -1000.0 : 0.0);
            if (score < bestScore) {
                bestScore = score;
                best = living;
            }
        }
        return best;
    }
}
