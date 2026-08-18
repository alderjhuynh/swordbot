package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.attack.AttackOnce;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class TickHit {

	private TickHit() {}

	/**
	 * Deliberate sub-90% charge hit. Stops the opponent's knockback before they
	 * escape your range, or free chip damage while already in a combo. Fires only
	 * inside the deliberate-tick cooldown window.
	 */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		FaceTarget.instant(client);
		TickScheduler.schedule(1, () -> Attack.chip(client, () -> AttackOnce.execute(client)));
	}
}