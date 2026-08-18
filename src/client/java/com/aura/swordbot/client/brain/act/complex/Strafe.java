package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import net.minecraft.client.Minecraft;

public final class Strafe {

	private static MoveDirection side = MoveDirection.LEFT;
	private static int switchTicks = 0;

	private Strafe() {}

	/** Picks a strafe side, switching cadence every ~20–50 ticks so we don't get read. */
	public static MoveDirection pickSide() {
		if (switchTicks-- <= 0) {
			switchTicks = 20 + (int) (Math.random() * 30);
			side = Math.random() < 0.5 ? MoveDirection.LEFT : MoveDirection.RIGHT;
		}
		return side;
	}

	public static void start(Minecraft client, MoveDirection direction) {
		if (direction == MoveDirection.FORWARD || direction == MoveDirection.BACK) return;
		MoveKeyDown.execute(client, direction);
	}

	public static void startRandom(Minecraft client) {
		start(client, pickSide());
	}

	public static void stop(Minecraft client, MoveDirection direction) {
		if (direction == MoveDirection.FORWARD || direction == MoveDirection.BACK) return;
		MoveKeyUp.execute(client, direction);
	}
}