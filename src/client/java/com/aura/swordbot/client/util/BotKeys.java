package com.aura.swordbot.client.util;

import net.minecraft.client.KeyMapping;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks which keys the bot has deliberately held down. The bot is only ever
 * allowed to release keys it pressed itself, so it can never yank a movement
 * key out from under the player's real input.
 */
public final class BotKeys {

	private static final Set<KeyMapping> pressed = Collections.synchronizedSet(new HashSet<>());

	private BotKeys() {}

	public static void down(KeyMapping key) {
		key.setDown(true);
		pressed.add(key);
	}

	public static void up(KeyMapping key) {
		key.setDown(false);
		pressed.remove(key);
	}

	/** Releases every key the bot pressed, leaving the player's keys untouched. */
	public static void releaseAll() {
		for (KeyMapping key : pressed) {
			key.setDown(false);
		}
		pressed.clear();
	}
}