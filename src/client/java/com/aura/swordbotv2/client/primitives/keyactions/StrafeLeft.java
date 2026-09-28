package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class StrafeLeft {
    public static void executeDown(Minecraft client) {
        client.options.keyLeft.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keyLeft.setDown(false);
    }
}
