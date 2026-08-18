package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.info.targetStats;

/**
 * Timing neuron (§12 step 2). Gates every attack on sword charge and on the
 * golden rule of air knockback: never KB-hit a falling target (§1.4).
 */
public final class TimingNeuron {

	private TimingNeuron() {}

	/** Sword charged enough for a full-knockback hit (~85%+, §1.2). */
	public static boolean canAttack() {
		return selfStats.attackCooldown >= FightConfig.SPRINT_HIT_COOLDOWN;
	}

	/** Charge is ready AND the target is grounded or rising (full KB applies). */
	public static boolean canKbHit() {
		if (!canAttack()) return false;
		if (targetStats.targetOnGround) return true;
		if (targetStats.targetVelocity == null) return true;
		return targetStats.targetVelocity.y >= -FightConfig.FALL_TOLERANCE;
	}

	/** Cooldown sits in the deliberate-tick window (chip damage / freeze KB). */
	public static boolean isTickWindow() {
		float cooldown = selfStats.attackCooldown;
		return cooldown >= FightConfig.TICK_COOLDOWN_MIN && cooldown < FightConfig.TICK_COOLDOWN;
	}

	/** The target is coming down — the golden rule says hold your KB hit. */
	public static boolean targetFalling() {
		return !targetStats.targetOnGround
				&& targetStats.targetVelocity != null
				&& targetStats.targetVelocity.y < -FightConfig.FALL_TOLERANCE;
	}

	/** The target is going up — jump-reset stance / crit telegraph. */
	public static boolean targetRising() {
		return !targetStats.targetOnGround
				&& targetStats.targetVelocity != null
				&& targetStats.targetVelocity.y > FightConfig.RISE_TOLERANCE;
	}
}