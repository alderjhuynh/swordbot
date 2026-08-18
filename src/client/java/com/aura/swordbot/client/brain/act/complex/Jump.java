package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.jump.JumpOnce;
import com.aura.swordbot.client.brain.info.selfStats;
import net.minecraft.client.Minecraft;

/**
 * Ground-gated jump. A jump can only be initiated from the ground — if the bot
 * is airborne it can't jump, so we simply don't fire the jump at all.
 */
public final class Jump {

	private Jump() {}

	public static void execute(Minecraft client) {
		if (!selfStats.onGround) return;
		JumpOnce.execute(client);
	}
}