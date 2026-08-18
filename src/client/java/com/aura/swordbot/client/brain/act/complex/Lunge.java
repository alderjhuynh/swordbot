package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class Lunge {

	private Lunge() {}

	/** Sprint-jump forward and sprint-hit while falling. Chases players holding S. */
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
			Attack.commit(client, () -> AttackOnce.execute(client));
		});
	}
}