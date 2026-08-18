package com.aura.swordbot.client.brain.neurons;

import com.aura.swordbot.client.brain.act.complex.Crit;
import com.aura.swordbot.client.brain.act.complex.DoubleCrit;
import com.aura.swordbot.client.brain.act.complex.FaceTarget;
import com.aura.swordbot.client.brain.act.complex.Hop;
import com.aura.swordbot.client.brain.act.complex.Lunge;
import com.aura.swordbot.client.brain.act.complex.STap;
import com.aura.swordbot.client.brain.act.complex.Strafe;
import com.aura.swordbot.client.brain.act.complex.Sweep;
import com.aura.swordbot.client.brain.act.complex.TickHit;
import com.aura.swordbot.client.brain.act.complex.WTap;
import com.aura.swordbot.client.util.TickScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/**
 * Combo sustain neuron (§12 step 4, §5). Keeps the chain alive with W-tap
 * cadence tuned to the opponent's landing frames; reads the back-out and
 * switches (lunge / follow-up sweep / hop-lunge alternation), S-taps
 * jump-resetters, and fires deliberate ticks in the chip window. High-ping
 * opponents are comboed with no-KB crits and double crits instead (§11.5).
 */
public final class ComboNeuron {

	private static int sHoldStreak = 0;
	private static boolean lastKeepWasHop = false;

	private ComboNeuron() {}

	public static void tick(Minecraft client, LivingEntity target) {
		if (target == null) return;

		double dist = PriorityNeuron.distance();
		if (dist < FightConfig.MIN_COMBAT_RANGE || dist > FightConfig.COMBO_RANGE) return;

		// high ping: don't combo with sprint hits — play the crit game (§11.5)
		if (Brain.opponent.highPing) {
			critChain(client);
			return;
		}

		// golden rule: never KB-hit a falling target — wait for the landing (§1.4)
		if (TimingNeuron.targetFalling()) {
			holdForLanding(client);
			return;
		}

		if (!TimingNeuron.canKbHit()) {
			// chip window — free damage / freeze their KB while waiting (§1.2)
			if (TimingNeuron.isTickWindow()) {
				FaceTarget.instant(client);
				ActionLock.lock(3);
				TickHit.execute(client);
			}
			return;
		}

		// backing out → lunge or follow-up sweep (§5.4, §7.2)
		if (CounterNeuron.isBackingOut(target)) {
			sHoldStreak++;
			if (sHoldStreak >= FightConfig.S_HOLD_STREAK) {
				// S-holder: alternate hops and lunges to stay in range (§8.3)
				ActionLock.lock(10);
				lastKeepWasHop = !lastKeepWasHop;
				if (lastKeepWasHop) {
					Hop.execute(client);
				} else {
					Lunge.execute(client);
				}
			} else {
				ActionLock.lock(8);
				if (Brain.tickCount % 3 == 0) {
					Sweep.execute(client); // follow-up sweep (§7.2)
				} else {
					Lunge.execute(client);
				}
			}
			return;
		}
		sHoldStreak = 0;

		// jump-resetters → S-tap, max stun (§3.2)
		if (TimingNeuron.targetRising()) {
			ActionLock.lock(3);
			STap.executeAimed(client);
			return;
		}

		// default: W-tap cadence, pausing when they drift back (§3.1, §5.3)
		Strafe.start(client, Strafe.pickSide());
		if (CounterNeuron.driftingBack()) {
			ActionLock.lock(FightConfig.PAUSE_W_TAP_DELAY + 3);
			TickScheduler.schedule(FightConfig.PAUSE_W_TAP_DELAY, () -> WTap.executeAimed(client));
		} else {
			ActionLock.lock(3);
			WTap.executeAimed(client);
		}
	}

	/** No-KB crits + double crits, stay close — the high-ping combo (§11.5). */
	private static void critChain(Minecraft client) {
		if (!TimingNeuron.canAttack()) return;
		ActionLock.lock(14);
		if (Brain.tickCount % 4 == 0) {
			DoubleCrit.execute(client);
		} else {
			Crit.noKb(client);
		}
	}

	/** Fallback while the target is falling — hold the ring, chip if we can (§1.4). */
	private static void holdForLanding(Minecraft client) {
		Strafe.start(client, Strafe.pickSide());
		if (TimingNeuron.isTickWindow()) {
			FaceTarget.instant(client);
			ActionLock.lock(3);
			TickHit.execute(client);
		}
	}
}