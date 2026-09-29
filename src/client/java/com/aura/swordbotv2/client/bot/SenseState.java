package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SenseState {
    public boolean valid;
    public boolean targetVisible;
    public int ticksSinceSeen;
    public int visibleTicks;

    public Vec3 selfPos;
    public Vec3 eyePos;
    public Vec3 selfVel;
    public boolean onGround;
    public double fallDistance;
    public double health;
    public double absorption;
    public int food;
    public boolean sprinting;
    public double strength;
    public boolean hurtThisTick;

    public LivingEntity target;
    public Vec3 targetPos;
    public Vec3 targetEye;
    public Vec3 targetVel;
    public Vec3 targetVelEma;
    public double targetWidth;
    public double targetHeight;
    public double targetEyeHeight;
    public double targetHealth;
    public int targetHurtTime;
    public boolean targetUsingItem;

    public double hdist;
    public double reachMe;
    public double rawReach;
    public double reachOp;
    public double radialVel;
    public double tangentialVel;
    public double targetSpeed;
    public boolean critOk;

    public int lastHurtAge = -100000;
    public int foeSwingAge = -100000;
    public boolean justLanded;
    public int foeKbLevel;
    public boolean prevGround = true;

    private double lastHp = -1;
    private Vec3 lastTargetPos;
    private boolean wasFoeSwinging;
    private boolean wasGround = true;

    public void tick(Minecraft client, LivingEntity lockedTarget, int age) {
        valid = false;
        if (client.player == null || client.level == null) {
            return;
        }
        valid = true;

        selfPos = client.player.position();
        eyePos = client.player.getEyePosition();
        selfVel = client.player.getDeltaMovement();
        onGround = client.player.onGround();
        prevGround = wasGround;
        justLanded = onGround && !wasGround;
        wasGround = onGround;
        fallDistance = client.player.fallDistance;
        health = client.player.getHealth();
        absorption = client.player.getAbsorptionAmount();
        food = client.player.getFoodData().getFoodLevel();
        sprinting = client.player.isSprinting();
        try {
            strength = client.player.getAttackStrengthScale(0.5F);
        } catch (NoSuchMethodError | NoClassDefFoundError e) {
            strength = 1.0;
        }
        if (lastHp >= 0) {
            hurtThisTick = health + absorption < lastHp - 0.001;
            if (hurtThisTick) {
                lastHurtAge = age;
            }
        }
        lastHp = health + absorption;

        if (lockedTarget == null || !lockedTarget.isAlive()
                || lockedTarget.distanceTo(client.player) > 64.0) {
            target = null;
            targetVisible = false;
            ticksSinceSeen++;
            visibleTicks = 0;
            return;
        }
        target = lockedTarget;
        boolean seen = hasLos(client, eyePos, target);
        if (seen) {
            targetVisible = true;
            ticksSinceSeen = 0;
            visibleTicks++;
        } else {
            targetVisible = false;
            ticksSinceSeen++;
        }

        Vec3 tp = target.position();
        if (lastTargetPos != null && target == lockedTarget) {
            targetVel = tp.subtract(lastTargetPos);
        } else {
            targetVel = Vec3.ZERO;
            targetVelEma = null;
        }
        if (targetVelEma == null) {
            targetVelEma = targetVel;
        } else {
            targetVelEma = targetVelEma
                    .add(targetVel.subtract(targetVelEma).scale(0.3));
        }
        lastTargetPos = tp;
        targetPos = tp;
        targetWidth = target.getBbWidth();
        targetHeight = target.getBbHeight();
        targetEyeHeight = target.getEyeHeight();
        targetEye = tp.add(0, targetEyeHeight, 0);
        try {
            targetHealth = target.getHealth();
        } catch (Exception e) {
            targetHealth = 20.0;
        }
        targetHurtTime = target.hurtTime;
        boolean foeSwinging = false;
        try {
            foeSwinging = target.isSwinging();
        } catch (Exception ignored) {
        }
        if (foeSwinging && !wasFoeSwinging) {
            foeSwingAge = age;
        }
        wasFoeSwinging = foeSwinging;
        boolean using = false;
        try {
            using = target.isUsingItem();
        } catch (Exception ignored) {
        }
        targetUsingItem = using;
        foeKbLevel = 0;
        try {
            var stack = target.getMainHandItem();
            if (stack != null && !stack.isEmpty() && client.level != null) {
                var lookup = client.level.registryAccess()
                        .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
                var kb = lookup.getOrThrow(
                        net.minecraft.world.item.enchantment.Enchantments.KNOCKBACK);
                foeKbLevel = net.minecraft.world.item.enchantment.EnchantmentHelper
                        .getItemEnchantmentLevel(kb, stack);
            }
        } catch (Exception ignored) {
            foeKbLevel = 0;
        }

        double dx = tp.x - selfPos.x;
        double dz = tp.z - selfPos.z;
        hdist = Math.sqrt(dx * dx + dz * dz);
        reachMe = eyeToBox(eyePos, tp, targetWidth, targetHeight);
        rawReach = reachMe;
        reachOp = eyeToBox(targetEye, selfPos, 0.6, 1.8);

        double speed = Math.sqrt(targetVelEma.x * targetVelEma.x + targetVelEma.z * targetVelEma.z);
        targetSpeed = speed;
        if (hdist > 1e-6) {
            radialVel = -(targetVelEma.x * dx + targetVelEma.z * dz) / hdist;
            tangentialVel = (targetVelEma.x * dz - targetVelEma.z * dx) / hdist;
        } else {
            radialVel = 0;
            tangentialVel = 0;
        }
        critOk = fallDistance > 0 && !onGround && !sprinting && strength > 0.9;
    }

    public void clearTargetMemory() {
        lastTargetPos = null;
        targetVelEma = null;
        foeSwingAge = -100000;
        wasFoeSwinging = false;
    }

    private static boolean hasLos(Minecraft client, Vec3 from, LivingEntity target) {
        Vec3 to = target.position().add(0, target.getEyeHeight(), 0);
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, client.player);
        HitResult hit = client.level.clip(ctx);
        if (hit.getType() == HitResult.Type.MISS) {
            return true;
        }
        Vec3 p = hit.getLocation();
        Vec3 tp = target.position();
        double w = target.getBbWidth() / 2.0 + 0.3;
        return p.x >= tp.x - w && p.x <= tp.x + w
                && p.z >= tp.z - w && p.z <= tp.z + w
                && p.y >= tp.y - 0.5 && p.y <= tp.y + target.getBbHeight() + 0.5;
    }

    static double eyeToBox(Vec3 eye, Vec3 feet, double width, double height) {
        double hx = width / 2.0;
        double dx = clampAxis(eye.x, feet.x - hx, feet.x + hx);
        double dz = clampAxis(eye.z, feet.z - hx, feet.z + hx);
        double dy = clampAxis(eye.y, feet.y, feet.y + height);
        double ox = eye.x - dx;
        double oy = eye.y - dy;
        double oz = eye.z - dz;
        return Math.sqrt(ox * ox + oy * oy + oz * oz);
    }

    private static double clampAxis(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public Vec3 leadFeet(double ticks, double maxLen) {
        if (targetPos == null) {
            return null;
        }
        if (targetVelEma == null || ticks <= 0) {
            return targetPos;
        }
        Vec3 off = targetVelEma.multiply(ticks, ticks, ticks);
        double len = off.length();
        if (len > maxLen && len > 1e-9) {
            off = off.scale(maxLen / len);
        }
        return targetPos.add(off);
    }

    public void endTick() {
        hurtThisTick = false;
    }

    public void onTargetChanged(Entity next) {
        if (target != next) {
            lastTargetPos = null;
            targetVelEma = null;
            visibleTicks = 0;
            foeSwingAge = -100000;
            wasFoeSwinging = false;
        }
    }
}
