package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class StandingCrit {

	private StandingCrit() {}

	/** Sprint hit → jump → crit on the way down. Punishes punish-critters. */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			StartSprint.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			WTap.execute(client);
		});
		TickScheduler.schedule(6, () -> {
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