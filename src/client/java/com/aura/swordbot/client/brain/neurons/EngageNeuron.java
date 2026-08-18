package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.complex.Crit;
import com.aura.swordbot.client.brain.act.complex.Jump;
import com.aura.swordbot.client.brain.act.complex.SprintHit;
import com.aura.swordbot.client.brain.act.complex.Strafe;
import com.aura.swordbot.client.brain.act.complex.ThreeBlockHit;
import com.aura.swordbot.client.brain.info.targetStats;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Opening / engagement neuron (§4, §11.2, §11.3). Picks the combo starter by
 * HP situation. Aggressive: sprint hits + reactive jumps, no leading crits,
 * three-blocks against sprinting opponents. Survival: long-range crits, baits,
 * RPS crits close-in — never exchange sprint hits from behind. Probe: the crit
 * game first to gather data on unknown opponents.
 */
public final class EngageNeuron {

	private EngageNeuron() {}

	public static void aggressive(Minecraft client, LivingEntity target) {
		double dist = PriorityNeuron.distance();

		// jump whenever the opponent jumps — safe, never out-traded by crits (§11.2)
		if (TimingNeuron.targetRising()) {
			Jump.execute(client);
			return;
		}

		// opponent sprints at us from just out of reach → three-block before they reach us (§4.3)
		if (targetStats.targetSprinting
				&& dist <= FightConfig.THREE_BLOCK_RANGE + 0.5
				&& dist > FightConfig.MIN_COMBAT_RANGE) {
			if (!TimingNeuron.canAttack()) return;
			ActionLock.lock(3);
			ThreeBlockHit.execute(client);
			Brain.state = FightState.COMBATING;
			return;
		}

		if (dist <= FightConfig.COMBO_RANGE + 0.5) {
			if (!TimingNeuron.canKbHit()) return;
			ActionLock.lock(3);
			SprintHit.executeAimed(client);
			Brain.state = FightState.COMBATING;
		} else {
			SpacingNeuron.approach(client, target);
		}
	}

	public static void survival(Minecraft client, LivingEntity target) {
		double dist = PriorityNeuron.distance();

		// too close while behind — back out, never trade sprint hits from behind (§11.3)
		if (dist < FightConfig.MIN_COMBAT_RANGE) {
			SpacingNeuron.retreat(client);
			return;
		}

		if (!TimingNeuron.canAttack()) {
			SpacingNeuron.tick(client, target, false, true);
			return;
		}

		if (dist >= FightConfig.CHASE_RANGE) {
			// long-range crits — most options, most reward (§11.1, §11.3)
			ActionLock.lock(9);
			Crit.execute(client);
			Brain.state = FightState.ENGAGING;
		} else if (dist > FightConfig.BAIT_RANGE) {
			// baiting distance — strafe side-to-side to force a whiff (§4.2)
			Strafe.start(client, Strafe.pickSide());
			Brain.state = FightState.BAITING;
		} else {
			// close range behind: play the crit game, don't commit sprint hits (§11.3)
			RpsNeuron.tick(client, target);
			Brain.state = FightState.ENGAGING;
		}
	}

	public static void probe(Minecraft client, LivingEntity target) {
		// unknown opponent → the crit game feeds information, then we switch (§10.3)
		RpsNeuron.tick(client, target);
		Brain.state = FightState.ENGAGING;
	}
}