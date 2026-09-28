package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class Jump {
    public static void executeDown(Minecraft client) {
        client.options.keyJump.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keyJump.setDown(false);
    }
}
