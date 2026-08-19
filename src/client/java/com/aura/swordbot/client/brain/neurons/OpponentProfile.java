package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.info.targetStats;
import com.aura.swordbot.client.util.TargetManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Long-term reads on the opponent (guide §1.5, §10). Rebuilds the crit-game
 * counters and the ping estimate from the same sensors the rest of the brain
 * uses: airborne windows = crits, rising right after our hit = jump reset,
 * sprinting through our knockback = delayed KB = high ping.
 */
public final class OpponentProfile {

	public boolean highPing = false;
	public int airCrits = 0;
	public int noKbCrits = 0;
	public int doubleCrits = 0;
	public int punishCrits = 0;
	public int jumpResets = 0;
	public int sweeps = 0;
	public int sCrits = 0;

	private int lateKbSigns = 0;
	private int pingWindowStart = 0;
	private long prevAirborneTick = -100;
	private boolean wasAirborne = false;

	public void tick(LivingEntity target) {
		readPing();
		readCritGame(target);
	}

	/** §1.5 — high-ping players take delayed knockback and hit-select you. */
	private void readPing() {
		if (Brain.tickCount - pingWindowStart >= FightConfig.PING_SAMPLE_TICKS) {
			highPing = lateKbSigns >= FightConfig.HIGH_PING_THRESHOLD;
			lateKbSigns = 0;
			pingWindowStart = Brain.tickCount;
		}
		// a sprint hit that doesn't stop them sprinting → they kept moving → delayed KB
		if (Brain.justLandedHit && targetStats.targetSprinting) {
			lateKbSigns++;
		}
		// they hit us again inside the escape window right after our own hit → hit-select
		if (Brain.justTookDamage && Brain.tickCount - Brain.lastHitTick <= FightConfig.ESCAPE_HIT_WINDOW) {
			lateKbSigns++;
		}
	}

	/** §10 — reads the crit family from airborne windows and hit timing. */
	private void readCritGame(LivingEntity target) {
		boolean airborne = !targetStats.targetOnGround;
		double dist = PriorityNeuron.distance();

		if (airborne && !wasAirborne) {
			long now = Brain.tickCount;
			airCrits++;
			// double crit: a second jump inside the window right after the first
			if (now - prevAirborneTick <= FightConfig.DOUBLE_CRIT_WINDOW) {
				doubleCrits++;
			}
			prevAirborneTick = now;
			// punish crit: they go airborne right after we knock them back
			if (now - Brain.lastHitTick <= FightConfig.PUNISH_CRIT_WINDOW) {
				punishCrits++;
			}
			// no-KB crit: they crit while sprinting (don't reset) — stays glued
			if (targetStats.targetSprinting) {
				noKbCrits++;
			}
			// S crit: they go airborne while drifting away from us (builds distance)
			if (driftingAway()) {
				sCrits++;
			}
		}

		// jump reset: they rise right as our hit lands
		if (Brain.justLandedHit && airborne
				&& targetStats.targetVelocity != null
				&& targetStats.targetVelocity.y > FightConfig.RISE_TOLERANCE) {
			jumpResets++;
		}

		// sweep: they chip us while grounded and not sprinting at close range
		if (Brain.justTookDamage && !airborne && !targetStats.targetSprinting
				&& dist <= FightConfig.COMBO_RANGE) {
			sweeps++;
		}

		wasAirborne = airborne;
	}

	/** The target's velocity carries them away from us (§10.3 S-crit detection). */
	private boolean driftingAway() {
		if (targetStats.targetVelocity == null || selfStats.pos == null) return false;
		LivingEntity target = TargetManager.getTarget();
		if (target == null) return false;
		Vec3 toTarget = target.position().subtract(selfStats.pos);
		double len = toTarget.length();
		if (len < 0.001) return false;
		double dot = toTarget.x * targetStats.targetVelocity.x
				+ toTarget.y * targetStats.targetVelocity.y
				+ toTarget.z * targetStats.targetVelocity.z;
		return dot / len > 0.2;
	}
}