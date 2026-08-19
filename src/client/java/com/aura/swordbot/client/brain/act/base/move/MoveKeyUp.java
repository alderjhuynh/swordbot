package com.aura.swordbot.client.brain.act.base.move;

import com.aura.swordbot.client.util.BotKeys;
import net.minecraft.client.Minecraft;

public final class MoveKeyUp {
	private MoveKeyUp() {}

	public static void execute(Minecraft client, MoveDirection direction) {
		switch (direction) {
			case FORWARD -> BotKeys.up(client.options.keyUp);
			case BACK -> BotKeys.up(client.options.keyDown);
			case LEFT -> BotKeys.up(client.options.keyLeft);
			case RIGHT -> BotKeys.up(client.options.keyRight);
		}
	}
}