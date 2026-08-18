package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class Crit {

	private Crit() {}

	/** W-release crit: release W, jump, crit on the way down. The standard crit. */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Jump.execute(client);
		});
		TickScheduler.schedule(7, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> AttackOnce.execute(client));
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}

	/** No-KB crit: hold W (do NOT jump) so the crit deals less knockback on us. */
	public static void noKb(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> {
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
			Attack.commit(client, () -> AttackOnce.execute(client));
		});
	}

	/** Punish crit: crit using the knockback of their hit (we're already airborne). */
	public static void punish(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> Attack.commit(client, () -> AttackOnce.execute(client)));
	}
}
