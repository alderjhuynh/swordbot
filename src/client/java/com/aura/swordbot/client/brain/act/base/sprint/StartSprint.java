package com.aura.swordbot.client.brain.act.base.sprint;

import net.minecraft.client.Minecraft;

public class StartSprint {
    public static void execute(Minecraft client) {
        client.player.setSprinting(true);
    }
}
