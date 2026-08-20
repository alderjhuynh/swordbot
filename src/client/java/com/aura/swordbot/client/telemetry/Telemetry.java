package com.aura.swordbot.client.telemetry;

import com.aura.swordbot.client.brain.info.selfStats;
import com.aura.swordbot.client.brain.neurons.Brain;
import com.aura.swordbot.client.brain.neurons.FightState;
import com.aura.swordbot.client.brain.neurons.TimingNeuron;
import com.aura.swordbot.client.util.TargetManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class Telemetry {

	private static final Gson GSON = new Gson();
	private static final Path DIR = FabricLoader.getInstance().getGameDir().resolve("swordbot");
	private static final Path FILE = DIR.resolve("telemetry.jsonl");
	private static final int FLUSH_INTERVAL_TICKS = 100;

	private static final List<String> pending = new ArrayList<>();
	private static boolean dirReady;
	private static int lastFlushTick;

	private static boolean active;
	private static long sessionId;
	private static int startTick;
	private static long startMillis;
	private static float startSelfHealth = Float.NaN;
	private static float startTargetHealth = Float.NaN;
	private static FightState lastState;

	private static float damageDealt;
	private static float damageTaken;
	private static int hitsLanded;
	private static int hitsTaken;
	private static int whiffs;
	private static int missWhiffs;
	private static int blockWhiffs;
	private static int wrongTargetWhiffs;
	private static int escapes;
	private static int refusals;
	private static int combo;
	private static int maxCombo;
	private static int fullSwordIdleTicks;

	private Telemetry() {}

	public static boolean active() {
		return active;
	}

	public static void init() {
		try {
			Files.createDirectories(DIR);
			dirReady = true;
		} catch (IOException ignored) {
		}
	}

	public static void fightStart(LivingEntity target) {
		if (active) fightEnd("RESTART");
		active = true;
		sessionId++;
		startTick = Brain.tickCount;
		startMillis = System.currentTimeMillis();
		startSelfHealth = selfStats.health;
		startTargetHealth = target != null ? target.getHealth() : Float.NaN;
		lastState = Brain.state;

		damageDealt = 0;
		damageTaken = 0;
		hitsLanded = 0;
		hitsTaken = 0;
		whiffs = 0;
		missWhiffs = 0;
		blockWhiffs = 0;
		wrongTargetWhiffs = 0;
		escapes = 0;
		refusals = 0;
		combo = 0;
		maxCombo = 0;
		fullSwordIdleTicks = 0;

		JsonObject e = base("fight/start");
		e.addProperty("selfHp", round(startSelfHealth));
		if (target != null) {
			e.addProperty("targetHp", round(target.getHealth()));
			e.addProperty("dist", round(distTo(target)));
		}
		log(e);
	}

	public static void fightEnd(String reason) {
		if (!active) return;
		active = false;

		JsonObject e = base("fight/end");
		e.addProperty("reason", reason);
		e.addProperty("durationTicks", Brain.tickCount - startTick);
		e.addProperty("durationMs", System.currentTimeMillis() - startMillis);

		JsonObject s = new JsonObject();
		s.addProperty("startSelfHp", round(startSelfHealth));
		s.addProperty("endSelfHp", round(selfStats.health));
		s.addProperty("startTargetHp", round(startTargetHealth));
		s.addProperty("endTargetHp", round(targetHealth()));
		s.addProperty("damageDealt", round(damageDealt));
		s.addProperty("damageTaken", round(damageTaken));
		s.addProperty("hitsLanded", hitsLanded);
		s.addProperty("hitsTaken", hitsTaken);
		s.addProperty("whiffs", whiffs);
		s.addProperty("whiffMiss", missWhiffs);
		s.addProperty("whiffBlock", blockWhiffs);
		s.addProperty("whiffWrongTarget", wrongTargetWhiffs);
		s.addProperty("escapes", escapes);
		s.addProperty("attackRefused", refusals);
		s.addProperty("maxCombo", maxCombo);
		s.addProperty("fullSwordIdleTicks", fullSwordIdleTicks);
		e.add("summary", s);
		log(e);
		flush();
	}

	public static void hitLanded(float dmg) {
		if (!active) return;
		damageDealt += dmg;
		hitsLanded++;
		combo++;
		if (combo > maxCombo) maxCombo = combo;
		JsonObject e = base("hit/landed");
		e.addProperty("dmg", round(dmg));
		e.addProperty("combo", combo);
		e.addProperty("dist", round(dist()));
		log(e);
	}

	public static void hitTaken(float dmg) {
		if (!active) return;
		damageTaken += dmg;
		hitsTaken++;
		combo = 0;
		JsonObject e = base("hit/taken");
		e.addProperty("dmg", round(dmg));
		e.addProperty("dist", round(dist()));
		log(e);
	}

	public static void whiff(String kind) {
		if (!active) return;
		whiffs++;
		switch (kind) {
			case "MISS" -> missWhiffs++;
			case "BLOCK" -> blockWhiffs++;
			default -> wrongTargetWhiffs++;
		}
		JsonObject e = base("whiff");
		e.addProperty("kind", kind);
		e.addProperty("dist", round(dist()));
		log(e);
	}

	public static void refused(float cooldown) {
		if (!active) return;
		refusals++;
		JsonObject e = base("attack/refused");
		e.addProperty("cooldown", round(cooldown));
		e.addProperty("dist", round(dist()));
		log(e);
	}

	public static void stateChanged(FightState state) {
		if (!active || state == lastState) return;
		if (state == FightState.ESCAPING) escapes++;
		JsonObject e = base("state");
		e.addProperty("from", lastState != null ? lastState.name() : "?");
		e.addProperty("to", state.name());
		log(e);
		lastState = state;
	}

	public static void tick() {
		if (!active) return;
		if (TimingNeuron.canAttack() && !Brain.justLandedHit) {
			fullSwordIdleTicks++;
		}
		if (Brain.tickCount - lastFlushTick >= FLUSH_INTERVAL_TICKS) {
			lastFlushTick = Brain.tickCount;
			flush();
		}
	}

	private static JsonObject base(String event) {
		JsonObject e = new JsonObject();
		e.addProperty("ts", System.currentTimeMillis());
		e.addProperty("tick", Brain.tickCount);
		e.addProperty("session", sessionId);
		e.addProperty("event", event);
		return e;
	}

	private static double dist() {
		LivingEntity target = TargetManager.getTarget();
		return target != null ? distTo(target) : -1;
	}

	private static double distTo(LivingEntity target) {
		Minecraft mc = Minecraft.getInstance();
		return mc.player != null ? mc.player.distanceTo(target) : -1;
	}

	private static float targetHealth() {
		LivingEntity target = TargetManager.getTarget();
		return target != null ? target.getHealth() : Float.NaN;
	}

	private static float round(float v) {
		if (Float.isNaN(v)) return -1;
		return (float) (Math.round(v * 100.0) / 100.0);
	}

	private static double round(double v) {
		if (Double.isNaN(v)) return -1;
		return Math.round(v * 100.0) / 100.0;
	}

	private static void log(JsonObject e) {
		pending.add(GSON.toJson(e));
	}

	private static void flush() {
		if (pending.isEmpty()) return;
		if (!dirReady) init();
		try {
			try (BufferedWriter w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
				for (String line : pending) {
					w.write(line);
					w.newLine();
				}
			}
			pending.clear();
		} catch (IOException ignored) {}
	}
}