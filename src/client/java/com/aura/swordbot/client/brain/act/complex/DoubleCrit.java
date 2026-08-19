package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class DoubleCrit {

	private DoubleCrit() {}

	/** Punish crit → immediately jump and crit again. Counters punish crits. */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> Attack.commit(client, () -> {
			if (Attack.airborne()) AttackOnce.execute(client);
		}));
		TickScheduler.schedule(5, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Jump.execute(client);
		});
		TickScheduler.schedule(12, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> {
				if (Attack.airborne()) AttackOnce.execute(client);
			});
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}
}