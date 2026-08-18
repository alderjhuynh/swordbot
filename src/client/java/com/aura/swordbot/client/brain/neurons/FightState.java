package com.aura.swordbot.client.brain.neurons;

/** High-level fight posture, decided by the priority engine each tick. */
public enum FightState {
	NEUTRAL,
	APPROACHING,
	ENGAGING,
	COMBATING,
	BAITING,
	ESCAPING
}