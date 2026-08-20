package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.FightController;
import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.info.targetStats;
import com.aura.swordbot.client.telemetry.Telemetry;
import com.aura.swordbot.client.util.BotKeys;
import com.aura.swordbot.client.util.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * The orchestration hub. Runs the fight loop every client tick while fighting:
 * tracks health deltas, advances the fight state, reads the opponent profile,
 * then delegates the tick to the priority engine.
 */
public final class Brain {

	public static FightState state = FightState.NEUTRAL;
	public static int tickCount = 0;
	public static int lastDamageTick = -100;
	public static int lastHitTick = -100;
	public static boolean justTookDamage = false;
	public static boolean justLandedHit = false;

	/** Long-term reads on the opponent: ping estimate and crit-game counters. */
	public static final OpponentProfile opponent = new OpponentProfile();

	private static float lastSelfHealth = Float.NaN;
	private static float lastTargetHealth = Float.NaN;

	private Brain() {}

	public static void tick(Minecraft client) {
		if (client.player == null) return;
		tickCount++;

		if (!FightController.enabled) {
			state = FightState.NEUTRAL;
			justTookDamage = false;
			justLandedHit = false;
			return;
		}

		LivingEntity target = TargetManager.getTarget();
		if (target == null) {
			state = FightState.NEUTRAL;
			justTookDamage = false;
			justLandedHit = false;
			// the fight is over (target died/deselected) — let go of any keys the
			// bot was holding, but never the player's own input.
			BotKeys.releaseAll();
			return;
		}

		trackHealth();
		opponent.tick(target);

		if (state == FightState.COMBATING && tickCount - lastHitTick > FightConfig.COMBO_TIMEOUT_TICKS) {
			state = FightState.NEUTRAL;
		}
		if (state == FightState.ESCAPING && tickCount - lastDamageTick > FightConfig.ESCAPE_TIMEOUT_TICKS) {
			state = FightState.NEUTRAL;
		}

		PriorityNeuron.tick(client, target);
		Telemetry.stateChanged(state);
	}

	private static void trackHealth() {
		if (Float.isNaN(lastSelfHealth)) lastSelfHealth = selfStats.health;
		justTookDamage = selfStats.health < lastSelfHealth - 0.001f;
		if (justTookDamage) {
			lastDamageTick = tickCount;
			Telemetry.hitTaken(clampPositive(lastSelfHealth - selfStats.health));
		}

		if (targetStats.targetHealth != null) {
			if (!Float.isNaN(lastTargetHealth) && targetStats.targetHealth < lastTargetHealth - 0.001f) {
				justLandedHit = true;
				lastHitTick = tickCount;
				state = FightState.COMBATING;
				Telemetry.hitLanded(clampPositive(lastTargetHealth - targetStats.targetHealth));
			} else {
				justLandedHit = false;
			}
			lastTargetHealth = targetStats.targetHealth;
		}

		// getting hit at close range while we're NOT landing hits back means the
		// opponent has the momentum — we're the one inside the combo (§11.4).
		// Escape is only for when we genuinely can't fight back: never while we're
		// the aggressor in a combo (keep pressing, §11.2) and never while the sword
		// is ready (deflect instead, §4.4). Otherwise every counter-hit mid-combo
		// throws a winnable trade and hands the chase to the opponent.
		if (justTookDamage
				&& state != FightState.COMBATING
				&& PriorityNeuron.distance() <= FightConfig.COMBO_RANGE
				&& !TimingNeuron.canAttack()
				&& tickCount - lastHitTick > FightConfig.ESCAPE_HIT_WINDOW) {
			state = FightState.ESCAPING;
		}

		lastSelfHealth = selfStats.health;
	}

	private static float clampPositive(float v) {
		return Math.max(0.0f, v);
	}
}