package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class FallingSprintHit {

	private FallingSprintHit() {}

	/**
	 * Sprint-jump forward and sprint-hit while descending. Momentum extends reach,
	 * out-reaching faders and players who hold S out of a trade.
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			StartSprint.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			Jump.execute(client);
		});
		TickScheduler.schedule(6, () -> {
			StartSprint.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> {
				if (Attack.airborne()) AttackOnce.execute(client);
			});
		});
	}
}
