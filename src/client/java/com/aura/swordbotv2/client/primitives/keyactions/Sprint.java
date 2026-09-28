package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class Sprint {
    public static void executeDown(Minecraft client) {
        client.options.keySprint.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keySprint.setDown(false);
    }
}
