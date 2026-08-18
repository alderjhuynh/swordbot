package com.aura.swordbot.client.brain.act.base.use;

import net.minecraft.client.Minecraft;

public class UseKeyDown {
    public static void execute(Minecraft client) {
        client.options.keyUse.setDown(true);
    }
}
