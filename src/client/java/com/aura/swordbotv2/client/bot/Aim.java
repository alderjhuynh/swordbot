package com.aura.swordbotv2.client.bot;

import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class Aim {
    public static final double LEAD_TICKS = 0.0;
    public static final double LEAD_MAX = 0.0;

    private final Random random = new Random();
    private float currentYaw;
    private float currentPitch;
    private boolean initialized;
    public float wantYaw;
    public float wantPitch;

    public void snap(float yaw, float pitch) {
        currentYaw = yaw;
        currentPitch = pitch;
        initialized = true;
    }

    public void compute(Minecraft client, SenseState sense, BotConfig config) {
        if (client.player == null || sense.target == null) {
            return;
        }
        if (!initialized) {
            snap(client.player.getYRot(), client.player.getXRot());
        }
        Vec3 eye = sense.eyePos;
        Vec3 feet = sense.targetPos;
        Vec3 lead = sense.leadFeet(LEAD_TICKS, LEAD_MAX);
        if (lead != null) {
            feet = lead;
        }
        double hx = sense.targetWidth / 2.0;

        double px = clamp(eye.x, feet.x - hx + 0.06, feet.x + hx - 0.06);
        double pz = clamp(eye.z, feet.z - hx + 0.06, feet.z + hx - 0.06);
        if (Math.abs(eye.x - feet.x) < 0.05 && Math.abs(eye.z - feet.z) < 0.05) {
            px = feet.x;
            pz = feet.z;
        }
        double py = clamp(eye.y, feet.y + 0.06, feet.y + sense.targetHeight - 0.06);

        double dx = px - eye.x;
        double dy = py - eye.y;
        double dz = pz - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (Math.toDegrees(Math.atan2(-dy, h)));

        double err = config.skill.aimErrDeg;
        if (err > 0) {
            yaw += (random.nextDouble() * 2 - 1) * err;
            pitch += (random.nextDouble() * 2 - 1) * err * 0.6;
        }
        wantYaw = yaw;
        wantPitch = Mth.clamp(pitch, -90.0F, 90.0F);
    }

    public float[] step(BotConfig config) {
        double cap = config.skill.turnDegPerTick;
        currentYaw = approachAngle(currentYaw, wantYaw, cap);
        currentPitch = approach(currentPitch, wantPitch, cap);
        currentPitch = Mth.clamp(currentPitch, -90.0F, 90.0F);
        return new float[] {currentYaw, currentPitch};
    }

    public void applyTo(Minecraft client, float yaw, float pitch) {
        if (client.player == null) {
            return;
        }
        currentYaw = Mth.wrapDegrees(yaw);
        currentPitch = Mth.clamp(pitch, -90.0F, 90.0F);
        client.player.setYRot(currentYaw);
        client.player.setXRot(currentPitch);
    }

    private static float approach(float cur, float want, double cap) {
        double d = want - cur;
        if (d > cap) {
            d = cap;
        } else if (d < -cap) {
            d = -cap;
        }
        return (float) (cur + d);
    }

    private static float approachAngle(float cur, float want, double cap) {
        double d = Mth.wrapDegrees(want - cur);
        if (d > cap) {
            d = cap;
        } else if (d < -cap) {
            d = -cap;
        }
        return Mth.wrapDegrees((float) (cur + d));
    }

    private static double clamp(double v, double lo, double hi) {
        if (lo > hi) {
            return (lo + hi) / 2.0;
        }
        return Math.max(lo, Math.min(hi, v));
    }

    static double rayBox(Vec3 eye, float yawDeg, float pitchDeg, Vec3 feet,
            double width, double height) {
        double y = Math.toRadians(yawDeg);
        double p = Math.toRadians(pitchDeg);
        double dx = -Math.sin(y) * Math.cos(p);
        double dy = -Math.sin(p);
        double dz = Math.cos(y) * Math.cos(p);
        double hx = width / 2.0;
        double tmin = 0.0;
        double tmax = Double.POSITIVE_INFINITY;
        double[] o = {eye.x, eye.y, eye.z};
        double[] d = {dx, dy, dz};
        double[] lo = {feet.x - hx, feet.y, feet.z - hx};
        double[] hi = {feet.x + hx, feet.y + height, feet.z + hx};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-12) {
                if (o[i] < lo[i] || o[i] > hi[i]) {
                    return Double.POSITIVE_INFINITY;
                }
            } else {
                double t1 = (lo[i] - o[i]) / d[i];
                double t2 = (hi[i] - o[i]) / d[i];
                if (t1 > t2) {
                    double t = t1;
                    t1 = t2;
                    t2 = t;
                }
                if (t1 > tmin) {
                    tmin = t1;
                }
                if (t2 < tmax) {
                    tmax = t2;
                }
                if (tmin > tmax) {
                    return Double.POSITIVE_INFINITY;
                }
            }
        }
        return tmin;
    }
}
