package com.aura.swordbot.client.brain.act.base.jump;

import net.minecraft.client.Minecraft;

public final class JumpKeyDown {
	private JumpKeyDown() {}

	public static void execute(Minecraft client) {
		client.options.keyJump.setDown(true);
	}
}