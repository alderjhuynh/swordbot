package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.complex.Crit;
import com.aura.swordbot.client.brain.act.complex.CritDeflect;
import com.aura.swordbot.client.brain.act.complex.DoubleCrit;
import com.aura.swordbot.client.brain.act.complex.SprintHit;
import com.aura.swordbot.client.brain.act.complex.StandingCrit;
import com.aura.swordbot.client.brain.act.complex.Strafe;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * The crit game (§10). Two entry points:
 * tick() — neutral openings from the decision tree (§10.3);
 * counter() — responding to an opponent who is airborne and critting (§10.2).
 * High-ping opponents get no-KB crits and double crits, staying close (§1.5).
 */
public final class RpsNeuron {

	private RpsNeuron() {}

	public static void tick(Minecraft client, LivingEntity target) {
		if (!TimingNeuron.canAttack()) {
			Strafe.start(client, Strafe.pickSide());
			return;
		}

		OpponentProfile p = Brain.opponent;

		// high ping: no-KB crits and double crits, stay as close as possible (§1.5, §11.5)
		if (p.highPing) {
			ActionLock.lock(14);
			if (Brain.tickCount % 4 == 0) {
				DoubleCrit.execute(client);
			} else {
				Crit.noKb(client);
			}
			return;
		}

		// unknown opponent → crit deflect (safest, feeds information) (§10.3)
		if (p.airCrits < FightConfig.CRIT_PROBE_COUNT) {
			ActionLock.lock(3);
			CritDeflect.execute(client, false);
			return;
		}

		// crit deflects not working → they're no-KB critting → switch to double crit
		if (p.noKbCrits > 0 && p.noKbCrits >= p.doubleCrits) {
			ActionLock.lock(14);
			DoubleCrit.execute(client);
			return;
		}

		// they adapt to double crits with no-KB crits → double crit reaction
		if (p.doubleCrits > 0 && p.noKbCrits > 0) {
			ActionLock.lock(3);
			SprintHit.executeAimed(client);
			return;
		}

		// they go for punish crits → standing crit (§10.5)
		if (p.punishCrits > 0) {
			ActionLock.lock(14);
			StandingCrit.execute(client);
			return;
		}

		ActionLock.lock(3);
		CritDeflect.execute(client, p.noKbCrits > 0);
	}

	/** Counter an airborne critter (guide §10.2 counter chart). */
	public static boolean counter(Minecraft client, LivingEntity target) {
		OpponentProfile p = Brain.opponent;
		if (!TimingNeuron.canAttack()) return false;

		// no-KB critters → double crit
		if (p.noKbCrits > 0) {
			ActionLock.lock(14);
			DoubleCrit.execute(client);
			return true;
		}

		// punish critters / double-critters → sprint hit before their second crit
		if (p.punishCrits > 0 || p.doubleCrits > 0) {
			ActionLock.lock(3);
			SprintHit.executeAimed(client);
			return true;
		}

		// default vs an airborne critter — crit deflect, with an S-tap when we
		// suspect no-KB crits (that combo beats even no-KB crits, §10.6)
		ActionLock.lock(3);
		CritDeflect.execute(client, p.noKbCrits > 0);
		return true;
	}
}