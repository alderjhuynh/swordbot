package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class PeakCrit {

	private PeakCrit() {}

	/**
	 * Peak crit: a W-release crit held slower than the standard crit to wait out
	 * the opponent's hit. Closes in on S-critters who are building distance, then
	 * leads into the crit game (§10.1, §10.3).
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Jump.execute(client);
		});
		TickScheduler.schedule(11, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> {
				if (Attack.airborne()) AttackOnce.execute(client);
			});
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}
}