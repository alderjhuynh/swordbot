package com.aura.swordbot.client.brain.act.base.sneak;

import net.minecraft.client.Minecraft;

public final class SneakKeyUp {
	private SneakKeyUp() {}

	public static void execute(Minecraft client) {
		client.options.keyShift.setDown(false);
	}
}