package com.aura.swordbot.client.brain.act.complex;

import com.aura.swordbot.client.brain.act.base.look.SmoothLook;
import com.aura.swordbot.client.util.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

public final class FaceTarget {

	private FaceTarget() {}

	/** Fast smooth flick onto the target's eye level — no crosshair teleport. */
	public static void instant(Minecraft client) {
		LivingEntity target = TargetManager.getTarget();
		if (target == null) return;
		SmoothLook.lookAt(client, target, 0);
	}

	/** Smoothly aim at the head — extends reach, required for three-block hits. */
	public static void atHead(Minecraft client) {
		LivingEntity target = TargetManager.getTarget();
		if (target == null) return;
		SmoothLook.lookAt(client, target.getEyePosition(), 0);
	}

	/** Smoothly aim at the body — shortens reach to ~2.7 blocks but is steadier at close range. */
	public static void atBody(Minecraft client) {
		LivingEntity target = TargetManager.getTarget();
		if (target == null) return;
		SmoothLook.lookAt(client, target.position().add(0, 0.8, 0), 0);
	}

	/** Smoothly rotate toward the target over the given duration. */
	public static void smooth(Minecraft client, long durationMillis) {
		LivingEntity target = TargetManager.getTarget();
		if (target == null) return;
		SmoothLook.lookAt(client, target, durationMillis);
	}
}