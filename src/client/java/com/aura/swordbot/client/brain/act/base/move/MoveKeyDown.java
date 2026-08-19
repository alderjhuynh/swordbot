package com.aura.swordbot.client.brain.act.base.move;

import com.aura.swordbot.client.util.BotKeys;
import net.minecraft.client.Minecraft;

public final class MoveKeyDown {
	private MoveKeyDown() {}

	public static void execute(Minecraft client, MoveDirection direction) {
		switch (direction) {
			case FORWARD -> BotKeys.down(client.options.keyUp);
			case BACK -> BotKeys.down(client.options.keyDown);
			case LEFT -> BotKeys.down(client.options.keyLeft);
			case RIGHT -> BotKeys.down(client.options.keyRight);
		}
	}
}