package com.aura.swordbotv2.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class MoveForward {
    public static void executeDown(Minecraft client) {
        client.options.keyUp.setDown(true);
    }

    public static void executeUp(Minecraft client) {
        client.options.keyUp.setDown(false);
    }
}
