package com.aura.swordbot.client.brain.act.base.look;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Per-frame smooth look. Instead of discrete scheduled steps, a rotation is
 * armed here and advanced once per rendered frame from {@link #renderFrame}.
 * Larger corrections travel at a higher angular speed (longer distances are
 * faster), the turn eases out like a human flick, and a small decaying error
 * keeps the aim from being a laser-perfect line.
 */
public final class SmoothLook {

	private static final float MIN_PITCH = -90.0f;
	private static final float MAX_PITCH = 90.0f;
	private static final long MIN_DURATION_MILLIS = 60;
	private static final long MAX_DURATION_MILLIS = 700;

	private static final float MAX_ERROR_YAW = 1.0f;
	private static final float MAX_ERROR_PITCH = 0.7f;

	private static boolean active;
	private static long startNano;
	private static long durationNanos;
	private static float startYaw;
	private static float startPitch;
	private static float goalYaw;
	private static float goalPitch;
	private static float errorYaw;
	private static float errorPitch;

	private SmoothLook() {}

	public static void lookAt(Minecraft client, Vec3 pos, long durationMillis) {
		if (client.player == null) return;

		Vec3 eye = client.player.getEyePosition();
		double dx = pos.x - eye.x;
		double dy = pos.y - eye.y;
		double dz = pos.z - eye.z;

		float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
		float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));

		look(client, yaw, pitch, durationMillis);
	}

	public static void lookAt(Minecraft client, Entity entity, long durationMillis) {
		lookAt(client, entity.getEyePosition(), durationMillis);
	}

	/**
	 * Arms a smooth rotation from the current view toward the given yaw/pitch.
	 * If durationMillis is not positive, it is derived from the angular distance
	 * so that longer turns are covered at a higher angular speed.
	 */
	public static void look(Minecraft client, float targetYaw, float targetPitch, long durationMillis) {
		LocalPlayer player = client.player;
		if (player == null) return;

		stop();

		startYaw = player.getYRot();
		startPitch = player.getXRot();
		goalYaw = targetYaw;
		goalPitch = clampPitch(targetPitch);

		// human-like: begin slightly off target and fine-tune while closing in
		errorYaw = (float) ((Math.random() * 2.0 - 1.0) * MAX_ERROR_YAW);
		errorPitch = (float) ((Math.random() * 2.0 - 1.0) * MAX_ERROR_PITCH);

		long duration = durationMillis;
		if (duration <= 0) {
			float angular = Math.abs(wrapDegrees(goalYaw - startYaw)) + Math.abs(goalPitch - startPitch);
			duration = Math.round(60 + angular * 8);
		}
		duration = Math.max(MIN_DURATION_MILLIS, Math.min(MAX_DURATION_MILLIS, duration));
		durationNanos = duration * 1_000_000L;
		startNano = System.nanoTime();
		active = true;
	}

	/** Advances the armed rotation once per rendered frame. */
	public static void renderFrame(Minecraft client) {
		if (!active) return;
		LocalPlayer player = client.player;
		if (player == null) {
			active = false;
			return;
		}

		long now = System.nanoTime();
		float t = (float) ((now - startNano) / (double) durationNanos);
		if (t >= 1.0f) {
			applyRotation(player, goalYaw, goalPitch);
			active = false;
			return;
		}

		float eased = easeOutCubic(t);
		float yaw = startYaw + wrapDegrees(goalYaw - startYaw) * eased;
		float pitch = startPitch + (goalPitch - startPitch) * eased;

		// error decays as the aim settles, with a slow organic wobble
		float fade = 1.0f - eased;
		float wobble = 0.7f + 0.3f * (float) Math.sin(now * Math.PI * 2.0 / 2_000_000_000.0);
		yaw += errorYaw * fade * wobble;
		pitch += errorPitch * fade * wobble;

		applyRotation(player, yaw, pitch);
	}

	public static void stop() {
		active = false;
	}

	private static void applyRotation(LocalPlayer player, float yaw, float pitch) {
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(clampPitch(pitch));
	}

	private static float easeOutCubic(float t) {
		float u = 1.0f - t;
		return 1.0f - u * u * u;
	}

	private static float wrapDegrees(float degrees) {
		return Math.floorMod((long) (degrees + 180f), 360L) - 180f;
	}

	private static float clampPitch(float pitch) {
		return Math.max(MIN_PITCH, Math.min(MAX_PITCH, pitch));
	}
}