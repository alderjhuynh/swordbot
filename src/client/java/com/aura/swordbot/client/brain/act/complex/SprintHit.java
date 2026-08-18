package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class SprintHit {

	private SprintHit() {}

	/** The workhorse: make sure we are sprinting and deliver a W-tap sprint hit. */
	public static void execute(Minecraft client) {
		StartSprint.execute(client);
		MoveKeyDown.execute(client, MoveDirection.FORWARD);
		WTap.execute(client);
	}

	/** Face the target first, then sprint-hit on the next tick. */
	public static void executeAimed(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> execute(client));
	}
}
