package com.aura.swordbot.client.brain.act.base.attack;

import net.minecraft.client.Minecraft;

public class AttackKeyUp {
    public static void execute(Minecraft client) {
        client.options.keyAttack.setDown(false);
    }
}
