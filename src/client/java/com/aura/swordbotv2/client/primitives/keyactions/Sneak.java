package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class Sneak {
    public static void executeDown(Minecraft client) {
        client.options.keyShift.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keyShift.setDown(false);
    }
}
