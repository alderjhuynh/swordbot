package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class ThreeBlockHit {

	private ThreeBlockHit() {}

	/**
	 * Sprint at full speed toward a sprinting opponent and hit them before they can
	 * hit back (~3 block reach). Head aim extends the range.
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.atHead(client);
		TickScheduler.schedule(1, () -> {
			StartSprint.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			WTap.execute(client);
		});
	}
}