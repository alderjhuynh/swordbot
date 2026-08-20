package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.neurons.FightConfig;
import com.aura.swordbot.client.telemetry.Telemetry;
import net.minecraft.client.Minecraft;

/**
 * Single choke point for every attack the bot throws. Enforces the sword-charge
 * rules from the guide (§1.2): a full-commit hit (sprint hit / crit) only fires
 * when the sword is charged to at least {@code SPRINT_HIT_COOLDOWN}, and a
 * deliberate tick (chip / KB-freeze) only fires inside the sub-full-charge
 * window. The threshold is checked at execution time so a scheduled attack can
 * never land while the sword is still recharging.
 */
public final class Attack {

	private Attack() {}

	/**
	 * The bot is airborne right now. Crits and falling sprint hits only exist in
	 * the air, so scheduled attacks verify this before swinging (§1.2, §10.1).
	 */
	public static boolean airborne() {
		return !selfStats.onGround;
	}

	/**
	 * Runs {@code action} (the actual {@code AttackOnce}) only if the sword is
	 * charged enough for a real hit. Returns whether it fired.
	 */
	public static boolean commit(Minecraft client, Runnable action) {
		if (selfStats.attackCooldown < FightConfig.SPRINT_HIT_COOLDOWN) {
			Telemetry.refused(selfStats.attackCooldown);
			return false;
		}
		action.run();
		return true;
	}

	/**
	 * Runs {@code action} only inside the deliberate-tick window (cooldown between
	 * {@code TICK_COOLDOWN_MIN} and {@code TICK_COOLDOWN}). Returns whether it fired.
	 */
	public static boolean chip(Minecraft client, Runnable action) {
		float cooldown = selfStats.attackCooldown;
		if (cooldown < FightConfig.TICK_COOLDOWN_MIN || cooldown >= FightConfig.TICK_COOLDOWN) return false;
		action.run();
		return true;
	}
}