package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class Hop {

	private Hop() {}

	/**
	 * Release W, jump almost straight up (minimal forward drift), re-sprint on the
	 * way up, then sprint-hit right before releasing W while falling. Holds combos
	 * from outside the opponent's reach and disguises sprint hits as crits.
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Jump.execute(client);
		});
		TickScheduler.schedule(4, () -> {
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			StartSprint.execute(client);
		});
		TickScheduler.schedule(8, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> {
				if (Attack.airborne()) AttackOnce.execute(client);
			});
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}
}