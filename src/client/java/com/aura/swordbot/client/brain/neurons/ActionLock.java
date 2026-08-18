package com.aura.swordbot.client.brain.neurons;

/**
 * Blocks new attack sequences while a multi-tick action (crit, double crit,
 * hop, lunge, ...) is still unfolding, so two scheduled attack sequences can't
 * overlap. Movement and spacing are unaffected — only the attack-committing
 * dispatch is gated (checked in {@link PriorityNeuron}).
 */
public final class ActionLock {

	private static int lockUntil = Integer.MIN_VALUE;

	private ActionLock() {}

	public static boolean isLocked() {
		return Brain.tickCount < lockUntil;
	}

	/** Lock the attack dispatch for the given number of ticks. */
	public static void lock(int ticks) {
		lockUntil = Brain.tickCount + ticks;
	}
}