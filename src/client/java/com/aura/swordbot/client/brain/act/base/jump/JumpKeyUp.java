package com.aura.swordbot.client.brain.act.base.jump;

import net.minecraft.client.Minecraft;

public final class JumpKeyUp {
	private JumpKeyUp() {}

	public static void execute(Minecraft client) {
		client.options.keyJump.setDown(false);
	}
}