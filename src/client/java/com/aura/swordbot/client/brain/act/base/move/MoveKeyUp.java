package com.aura.swordbot.client.brain.act.base.move;

import net.minecraft.client.Minecraft;

public final class MoveKeyUp {
	private MoveKeyUp() {}

	public static void execute(Minecraft client, MoveDirection direction) {
		switch (direction) {
			case FORWARD -> client.options.keyUp.setDown(false);
			case BACK -> client.options.keyDown.setDown(false);
			case LEFT -> client.options.keyLeft.setDown(false);
			case RIGHT -> client.options.keyRight.setDown(false);
		}
	}
}