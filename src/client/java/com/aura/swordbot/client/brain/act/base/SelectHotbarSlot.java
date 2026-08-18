package com.aura.swordbot.client.brain.act.base;

import net.minecraft.client.Minecraft;

public class SelectHotbarSlot {
    public static void execute(Minecraft client, Integer slot) {
        client.player.getInventory().setSelectedSlot(slot);
    }
}
