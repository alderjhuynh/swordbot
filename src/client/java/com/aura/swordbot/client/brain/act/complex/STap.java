package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class STap {

	private STap() {}

	/**
	 * S-tap sprint reset: release W, press S + click, snap back to W. Most stun of
	 * all resets — counters jump resetters, but costs all forward momentum.
	 * Only fires once the sword is charged enough for a real hit.
	 */
	public static void execute(Minecraft client) {
		Attack.commit(client, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			MoveKeyDown.execute(client, MoveDirection.BACK);
			AttackOnce.execute(client);
			MoveKeyUp.execute(client, MoveDirection.BACK);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}

	/** Face the target first, then S-tap on the next tick. */
	public static void executeAimed(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> execute(client));
	}
}
