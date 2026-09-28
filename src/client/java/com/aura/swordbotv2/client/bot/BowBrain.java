package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class BowBrain {
    private int bowStartAge = -100000;
    private boolean drawing;

    boolean tick(Minecraft client, SenseState sense, BotConfig config, Aim aim,
            Intents out, int age) {
        out.clear();
        if (!config.allowBow || client.player == null || sense.target == null
                || !sense.targetVisible) {
            stop(client);
            return false;
        }
        boolean punishEater = sense.target.isUsingItem()
                && sense.hdist >= config.punishEaterDist;
        boolean inWindow = sense.hdist >= config.bowMinDist || punishEater;
        boolean tooClose = sense.reachOp <= 4.5;
        boolean tooFar = sense.hdist > config.bowMaxDist;
        if ((!inWindow || tooClose || tooFar) && !drawing) {
            return false;
        }
        if (tooClose || age - bowStartAge >= 40) {
            stop(client);
            return false;
        }
        int bowSlot = RecoverBrain.findBowSlot(client);
        if (bowSlot < 0 || !RecoverBrain.hasArrows(client)) {
            stop(client);
            return false;
        }
        if (!drawing) {
            drawing = true;
            bowStartAge = age;
            client.player.getInventory().setSelectedSlot(bowSlot);
        }
        out.use = true;
        aimAt(client, sense, aim);
        return true;
    }

    private void aimAt(Minecraft client, SenseState sense, Aim aim) {
        Vec3 eye = sense.eyePos;
        Vec3 feet = sense.targetPos;
        Vec3 aimPoint = new Vec3(feet.x, feet.y + sense.targetHeight * 0.6, feet.z);
        int flight = solveFlightTicks(eye, aimPoint, sense.targetVelEma);
        Vec3 lead = aimPoint.add(
                sense.targetVelEma.x * flight, sense.targetVelEma.y * flight * 0.5, sense.targetVelEma.z * flight);
        double dx = lead.x - eye.x;
        double dy = lead.y - eye.y + arrowDrop(flight);
        double dz = lead.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        aim.wantYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        aim.wantPitch = (float) Math.toDegrees(Math.atan2(-dy, h));
    }

    private static int solveFlightTicks(Vec3 eye, Vec3 point, Vec3 vel) {
        int lo = 1;
        int hi = 90;
        for (int i = 0; i < 7; i++) {
            int mid = (lo + hi) / 2;
            double predicted = point.y + vel.y * mid * 0.5;
            double need = predicted - eye.y;
            if (need > maxRise(mid)) {
                lo = mid;
            } else {
                hi = mid;
            }
            if (hi - lo <= 1) {
                break;
            }
        }
        return Math.max(1, (lo + hi) / 2);
    }

    private static double flightDist(int n) {
        return 3.0 * (1.0 - Math.pow(0.99, n)) / 0.01;
    }

    private static double maxRise(int n) {
        return flightDist(n);
    }

    private static double arrowDrop(int n) {
        double gravityWell = 0.05 * n * (n - 1) / 2.0;
        return gravityWell;
    }

    boolean drawing() {
        return drawing;
    }

    void stop(Minecraft client) {
        drawing = false;
        bowStartAge = -100000;
        RecoverBrain.selectWeapon(client);
    }
}
