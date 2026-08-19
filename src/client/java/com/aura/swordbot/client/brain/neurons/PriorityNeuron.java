package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.complex.Crit;
import com.aura.swordbot.client.brain.act.complex.CrouchDodge;
import com.aura.swordbot.client.brain.act.complex.FallingSprintHit;
import com.aura.swordbot.client.brain.act.complex.Uppercut;
import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.info.targetStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * The priority engine (§12 step 5). Reads HP situation, spacing and opponent
 * telegraphs, then dispatches the tick to the right neuron:
 * HP advantage → sprint hits + reactive jumps, no leading crits.
 * HP disadvantage → long-range crits/baits.
 * Unknown opponent → probe with the crit game first.
 */
public final class PriorityNeuron {

	private PriorityNeuron() {}

	public static void tick(Minecraft client, LivingEntity target) {
		if (client.player == null || target == null) return;

		double dist = distance();
		boolean advantage = selfStats.health > target.getHealth();
		boolean disadvantage = selfStats.health < target.getHealth();

		if (Brain.state == FightState.ESCAPING) {
			EscapeNeuron.tick(client, target);
			return;
		}

		// hail-mary at ~1 HP — you die anyway, might as well crouch-dodge (§8.6)
		if (selfStats.health <= FightConfig.HAIL_MARY_HP) {
			CrouchDodge.execute(client);
			if (TimingNeuron.canAttack()) Crit.execute(client);
			return;
		}

		// a multi-tick action sequence (crit, double crit, hop, lunge, ...) is
		// still unfolding — keep spacing, don't stack a second attack on top
		if (ActionLock.isLocked()) {
			SpacingNeuron.tick(client, target, advantage, disadvantage);
			return;
		}

		// 3. counter layer — opponent telegraphs win the tick (§12 step 3)
		if (CounterNeuron.respond(client, target)) {
			if (Brain.state != FightState.COMBATING) {
				Brain.state = FightState.ENGAGING;
			}
			return;
		}

		// 4. combo sustain — keep the chain alive, but own the ring first (§12 step 4)
		if (Brain.state == FightState.COMBATING) {
			if (SpacingNeuron.inDesiredRange(client, target, advantage, disadvantage)) {
				ComboNeuron.tick(client, target);
			} else if (PriorityNeuron.distance() <= FightConfig.CHASE_RANGE) {
				// they slipped out of the ring — momentum reach keeps us landing hits
				// (falling sprint hit, with the occasional offbeat uppercut, §5.4, §8.7)
				ActionLock.lock(8);
				if (TimingNeuron.targetFalling() || Brain.tickCount % 4 != 0) {
					FallingSprintHit.execute(client);
				} else {
					Uppercut.execute(client);
				}
			} else {
				SpacingNeuron.tick(client, target, advantage, disadvantage);
			}
			return;
		}

		// 1. spacing — wrong range, move first (§12 step 1)
		if (!SpacingNeuron.inDesiredRange(client, target, advantage, disadvantage)) {
			SpacingNeuron.tick(client, target, advantage, disadvantage);
			return;
		}

		// 5. priority engine — pick the opening flavor by HP situation (§12 step 5)
		if (disadvantage) {
			EngageNeuron.survival(client, target);
		} else if (advantage) {
			EngageNeuron.aggressive(client, target);
		} else {
			EngageNeuron.probe(client, target);
		}
	}

	public static double distance() {
		Double sq = targetStats.distanceToTargetSq;
		return sq == null ? Double.MAX_VALUE : Math.sqrt(sq);
	}
}