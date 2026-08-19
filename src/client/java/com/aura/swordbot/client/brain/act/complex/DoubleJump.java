package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class DoubleJump {

	private DoubleJump() {}

	/**
	 * Double jump (§8.4): sprint-jump forward, then rebound as soon as we touch
	 * down (ground-gated tries). Builds momentum and makes us harder to sprint-hit
	 * or bait. Mix it up so it never becomes a predictable pattern.
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			StartSprint.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			Jump.execute(client);
		});
		// rebound: re-jump on the first frame we're back on the ground
		TickScheduler.schedule(8, () -> Jump.execute(client));
		TickScheduler.schedule(11, () -> Jump.execute(client));
	}
}