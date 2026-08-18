package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class CritDeflect {

	private CritDeflect() {}

	/**
	 * Sprint-hit the opponent right after their attack lands (swing on cooldown,
	 * we're in their space). Timing + spacing both matter.
	 */
	public static void execute(Minecraft client) {
		execute(client, false);
	}

	/** Crit deflect followed by an S-tap for extra stun (beats even no-KB crits). */
	public static void execute(Minecraft client, boolean sTapAfter) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			StartSprint.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> AttackOnce.execute(client));
			if (sTapAfter) {
				TickScheduler.schedule(1, () -> STap.execute(client));
			}
		});
	}
}