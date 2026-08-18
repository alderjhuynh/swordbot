package com.aura.swordbot.client.brain.act.base.move;

import net.minecraft.client.Minecraft;

public final class MoveKeyDown {
	private MoveKeyDown() {}

	public static void execute(Minecraft client, MoveDirection direction) {
		switch (direction) {
			case FORWARD -> client.options.keyUp.setDown(true);
			case BACK -> client.options.keyDown.setDown(true);
			case LEFT -> client.options.keyLeft.setDown(true);
			case RIGHT -> client.options.keyRight.setDown(true);
		}
	}
}