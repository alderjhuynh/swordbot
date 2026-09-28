package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class MoveBack {
    public static void executeDown(Minecraft client) {
        client.options.keyDown.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keyDown.setDown(false);
    }
}
