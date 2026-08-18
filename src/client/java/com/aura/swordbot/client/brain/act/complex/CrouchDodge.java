package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.sneak.SneakKeyDown;
import com.aura.swordbot.client.brain.act.base.sneak.SneakKeyUp;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;

public final class CrouchDodge {

	private CrouchDodge() {}

	/** Hold Shift to duck a hit. Dodges crits and falling sprint hits aimed at the head. */
	public static void execute(Minecraft client) {
		if (client.player == null) return;
		SneakKeyDown.execute(client);
		TickScheduler.schedule(10, () -> SneakKeyUp.execute(client));
	}
}