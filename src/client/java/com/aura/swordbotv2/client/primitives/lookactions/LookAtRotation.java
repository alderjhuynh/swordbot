package com.aura.swordbotv2.client.primitives.lookactions;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class LookAtRotation {
    public static void execute(Minecraft client, float yaw, float pitch) {
        if (client.player == null) {
            return;
        }
        client.player.setYRot(Mth.wrapDegrees(yaw));
        client.player.setXRot(Mth.clamp(Mth.wrapDegrees(pitch), -90.0F, 90.0F));
    }
}
