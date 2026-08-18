package com.aura.swordbot.client.brain.act.complex;

import net.minecraft.client.Minecraft;

public final class JumpReset {

	private JumpReset() {}

	/** Jump on the exact moment of the hit to cut horizontal knockback (ground-only). */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		Jump.execute(client);
	}
}