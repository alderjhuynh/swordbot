package com.aura.swordbot.client.brain.act.base.item;

import net.minecraft.client.Minecraft;

public final class DropItemOnce {
	private DropItemOnce() {}

	public static void execute(Minecraft client) {
		if (client.player == null) return;
		client.player.drop(false);
	}
}