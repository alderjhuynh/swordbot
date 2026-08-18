package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyDown;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.brain.act.base.sprint.StartSprint;
import com.aura.swordbot.client.brain.act.complex.Strafe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spacing neuron (§12 step 1). Owns the 2.5–3 block combat ring: backs up when
 * the opponent gets inside {@code MIN_COMBAT_RANGE}, closes in when they drift
 * past {@code COMBO_RANGE}, and strafes in between. Strafing is strictly good:
 * harder to hit us, and our hits sting more.
 */
public final class SpacingNeuron {

	private SpacingNeuron() {}

	public static boolean inDesiredRange(Minecraft client, LivingEntity target, boolean advantage, boolean disadvantage) {
		double dist = PriorityNeuron.distance();
		return switch (Brain.state) {
			case COMBATING -> dist >= FightConfig.MIN_COMBAT_RANGE && dist <= FightConfig.COMBO_RANGE;
			case BAITING -> dist >= FightConfig.BAIT_RANGE - 0.5 && dist <= FightConfig.BAIT_RANGE + 1.0;
			default -> disadvantage
					? dist >= FightConfig.MIN_COMBAT_RANGE && dist <= FightConfig.CRIT_RANGE
					: dist >= FightConfig.MIN_COMBAT_RANGE && dist <= FightConfig.COMBO_RANGE;
		};
	}

	public static void tick(Minecraft client, LivingEntity target, boolean advantage, boolean disadvantage) {
		double dist = PriorityNeuron.distance();
		double max = switch (Brain.state) {
			case BAITING -> FightConfig.BAIT_RANGE + 1.0;
			default -> disadvantage ? FightConfig.CRIT_RANGE : FightConfig.COMBO_RANGE;
		};

		if (dist > max) {
			approach(client, target);
		} else if (dist < FightConfig.MIN_COMBAT_RANGE) {
			retreat(client);
		} else {
			strafe(client);
		}
	}

	public static void approach(Minecraft client, LivingEntity target) {
		MoveKeyUp.execute(client, MoveDirection.BACK);
		MoveKeyDown.execute(client, MoveDirection.FORWARD);
		StartSprint.execute(client);
	}

	public static void retreat(Minecraft client) {
		MoveKeyUp.execute(client, MoveDirection.FORWARD);
		MoveKeyDown.execute(client, MoveDirection.BACK);
	}

	public static void strafe(Minecraft client) {
		Strafe.start(client, Strafe.pickSide());
	}
}