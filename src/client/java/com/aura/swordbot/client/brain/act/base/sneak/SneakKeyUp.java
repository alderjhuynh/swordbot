package com.aura.swordbot.client.brain.act.base.sneak;

import com.aura.swordbot.client.util.BotKeys;
import net.minecraft.client.Minecraft;

public final class SneakKeyUp {
	private SneakKeyUp() {}

	public static void execute(Minecraft client) {
		BotKeys.up(client.options.keyShift);
	}
}