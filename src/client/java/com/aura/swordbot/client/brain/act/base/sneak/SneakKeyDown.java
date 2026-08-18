package com.aura.swordbot.client.brain.act.base.sneak;

import net.minecraft.client.Minecraft;

public final class SneakKeyDown {
	private SneakKeyDown() {}

	public static void execute(Minecraft client) {
		client.options.keyShift.setDown(true);
	}
}