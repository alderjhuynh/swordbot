package com.aura.swordbot.client.brain.neurons;

/** Central tuning constants for the fight brain (guide §1–§12). */
public final class FightConfig {

	// combat ring: the bot backs up if closer than MIN and closes in if farther than COMBO
	public static final double COMBO_RANGE = 3.0;
	public static final double MIN_COMBAT_RANGE = 2.5;
	public static final double THREE_BLOCK_RANGE = 3.2;
	public static final double BAIT_RANGE = 4.5;
	public static final double CRIT_RANGE = 8.0;
	public static final double CHASE_RANGE = 6.0;

	// timing gates (§1.2): full-KB sprint hits at >= ~85% charge; ticks below ~90%
	public static final float SPRINT_HIT_COOLDOWN = 0.85f;
	public static final float TICK_COOLDOWN = 0.9f;
	public static final float TICK_COOLDOWN_MIN = 0.1f;
	public static final float FALL_TOLERANCE = 0.05f;
	public static final float RISE_TOLERANCE = 0.05f;
	public static final float HAIL_MARY_HP = 2.0f;

	// combo cadence (§3.1, §5.3)
	public static final int PAUSE_W_TAP_DELAY = 5;
	public static final int ESCAPE_HIT_WINDOW = 4;

	// ping read (§1.5)
	public static final int HIGH_PING_THRESHOLD = 3;
	public static final int PING_SAMPLE_TICKS = 40;

	// crit-game detection (§10)
	public static final int CRIT_PROBE_COUNT = 3;
	public static final int DOUBLE_CRIT_WINDOW = 12;
	public static final int PUNISH_CRIT_WINDOW = 6;

	// S-holder detection (§5.4, §8.3)
	public static final int S_HOLD_STREAK = 3;

	public static final int COMBO_TIMEOUT_TICKS = 60;
	public static final int ESCAPE_TIMEOUT_TICKS = 40;

	private FightConfig() {}
}