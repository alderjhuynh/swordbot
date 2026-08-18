package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.complex.Lunge;
import com.aura.swordbot.client.brain.act.complex.STap;
import com.aura.swordbot.client.brain.act.complex.SprintHit;
import com.aura.swordbot.client.brain.act.complex.ThreeBlockHit;
import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.info.targetStats;
import com.aura.swordbot.client.util.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Counter layer (§12 step 3, §4.4, §6, §7). Detects opponent telegraphs and
 * responds: deflection right after their swing, S-tap vs jump-resetters, the
 * crit game vs airborne critters, lunge vs faders, three-block vs sprinting /
 * sneaking opponents. Holds the hit on a falling target (golden rule, §1.4).
 */
public final class CounterNeuron {

	private CounterNeuron() {}

	public static boolean respond(Minecraft client, LivingEntity target) {
		if (target == null) return false;
		double dist = PriorityNeuron.distance();

		// every counter below commits an attack — don't while the sword recharges
		if (!TimingNeuron.canAttack()) {
			return false;
		}

		// 1. deflection — they just swung (health dropped), their swing is on
		//    cooldown and we're in their space (§4.4, §6.1)
		if (Brain.justTookDamage && dist <= FightConfig.COMBO_RANGE + 0.5) {
			ActionLock.lock(3);
			SprintHit.executeAimed(client);
			return true;
		}

		// 2. jump-reset stance — opponent jumps into our hits: S-tap, max stun (§3.2, §9)
		if (TimingNeuron.targetRising() && dist <= FightConfig.COMBO_RANGE) {
			ActionLock.lock(3);
			STap.executeAimed(client);
			return true;
		}

		// 3. falling target — golden rule: don't KB-hit them mid-fall, hold it (§1.4)
		if (TimingNeuron.targetFalling()) {
			return false;
		}

		// 4. airborne critter in neutral — respond in the crit game (§10.2)
		if (Brain.state != FightState.COMBATING
				&& !targetStats.targetOnGround
				&& dist <= FightConfig.CRIT_RANGE) {
			return RpsNeuron.counter(client, target);
		}

		// 5. backing out / fader — lunge out-reaches their retreat (§5.4)
		if (isBackingOut(target)) {
			if (dist <= FightConfig.CHASE_RANGE) {
				ActionLock.lock(8);
				Lunge.execute(client);
			} else {
				SpacingNeuron.approach(client, target);
			}
			return true;
		}

		// 6. sneaker — shrunk hitbox, three-block aims low on the head anyway (§8.6, §4.3)
		if (targetStats.targetSneaking && dist <= FightConfig.COMBO_RANGE + 0.5) {
			ActionLock.lock(3);
			ThreeBlockHit.execute(client);
			return true;
		}

		// 7. three-block — opponent sprints at us from just outside reach (§4.3)
		if (targetStats.targetSprinting
				&& dist <= FightConfig.THREE_BLOCK_RANGE + 0.5
				&& dist > FightConfig.MIN_COMBAT_RANGE) {
			ActionLock.lock(3);
			ThreeBlockHit.execute(client);
			return true;
		}

		return false;
	}

	/** True when the target's velocity carries them away from us. */
	public static boolean isBackingOut(LivingEntity target) {
		if (targetStats.targetVelocity == null || selfStats.pos == null) return false;
		Vec3 toTarget = target.position().subtract(selfStats.pos);
		double len = toTarget.length();
		if (len < 0.001) return false;
		double dot = toTarget.x * targetStats.targetVelocity.x
				+ toTarget.y * targetStats.targetVelocity.y
				+ toTarget.z * targetStats.targetVelocity.z;
		return dot / len > 0.2 && targetStats.targetOnGround;
	}

	/** A gentler back-off than isBackingOut — drives the pausing W-tap cadence. */
	public static boolean driftingBack() {
		if (targetStats.targetVelocity == null || selfStats.pos == null) return false;
		LivingEntity target = TargetManager.getTarget();
		if (target == null) return false;
		Vec3 toTarget = target.position().subtract(selfStats.pos);
		double len = toTarget.length();
		if (len < 0.001) return false;
		double dot = toTarget.x * targetStats.targetVelocity.x
				+ toTarget.y * targetStats.targetVelocity.y
				+ toTarget.z * targetStats.targetVelocity.z;
		return dot / len > 0.05;
	}
}