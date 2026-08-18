package com.aura.swordbot.client.brain.act.base.jump;

import net.minecraft.client.Minecraft;

public class JumpOnce {
    public static void execute(Minecraft client) {
        client.player.jumpFromGround();
    }
}
