package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class StrafeRight {
    public static void executeDown(Minecraft client) {
        client.options.keyRight.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keyRight.setDown(false);
    }
}
