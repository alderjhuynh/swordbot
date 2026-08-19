package com.aura.swordbot.client.brain.act.base.sneak;

import com.aura.swordbot.client.util.BotKeys;
import net.minecraft.client.Minecraft;

public final class SneakKeyDown {
	private SneakKeyDown() {}

	public static void execute(Minecraft client) {
		BotKeys.down(client.options.keyShift);
	}
}