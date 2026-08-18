package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.brain.act.complex.JumpReset;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Escape neuron (§11.4, §9). Fired when we are the one inside a combo. S-hold
 * is the consistent escape; jump resets are layered in on the hit frame and on
 * a cadence. High-ping opponents take delayed knockback, so pure S-hold (§1.5).
 */
public final class EscapeNeuron {

	private EscapeNeuron() {}

	public static void tick(Minecraft client, LivingEntity target) {
		if (target == null) return;

		MoveKeyUp.execute(client, MoveDirection.FORWARD);
		MoveKeyDown.execute(client, MoveDirection.BACK);

		// jump resets can't be timed against delayed KB — pure S-hold (§1.5)
		if (Brain.opponent.highPing) return;

		// jump reset right on the hit frame cuts horizontal KB (§9)
		if (Brain.justTookDamage && Brain.tickCount - Brain.lastDamageTick <= 1) {
			JumpReset.execute(client);
			return;
		}

		// jump while holding S takes even more KB — get out faster (§11.4)
		if (Brain.tickCount % 10 == 0) {
			JumpReset.execute(client);
		}
	}
}