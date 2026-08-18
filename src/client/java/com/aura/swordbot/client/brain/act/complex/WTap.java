package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class WTap {

	private WTap() {}

	/**
	 * W-tap sprint reset: release W → attack → re-press W. Resets sprint so the
	 * next hit is a fresh full-knockback sprint hit. Only fires once the sword is
	 * charged enough for a real hit.
	 */
	public static void execute(Minecraft client) {
		Attack.commit(client, () -> {
			MoveKeyUp.execute(client, MoveDirection.FORWARD);
			AttackOnce.execute(client);
			MoveKeyDown.execute(client, MoveDirection.FORWARD);
		});
	}

	/** Face the target first, then W-tap on the next tick so the crosshair tracks it. */
	public static void executeAimed(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> execute(client));
	}
}
