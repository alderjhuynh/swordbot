package com.aura.swordbot.client.brain;

import com.aura.swordbot.client.brain.act.base.look.SmoothLook;
import com.aura.swordbot.client.brain.act.base.move.MoveDirection;
import com.aura.swordbot.client.brain.act.base.move.MoveKeyUp;
import com.aura.swordbot.client.brain.act.base.sneak.SneakKeyUp;
import com.aura.swordbot.client.brain.act.base.sprint.EndSprint;
import com.aura.swordbot.client.brain.neurons.Brain;
import com.aura.swordbot.client.brain.neurons.FightState;
import com.aura.swordbot.client.util.TargetManager;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

/**
 * Sword fight controller. While enabled, it locks onto the single closest
 * living entity (never the local player) and re-asserts that lock every tick so
 * exactly one target is tracked. The fight toggle is driven by the fight
 * keybind (K).
 */
public final class FightController {

	public static boolean enabled = false;

	private static KeyMapping fightKey;
	private static LivingEntity lockedTarget;

	private FightController() {}

	public static void init() {
		fightKey = new KeyMapping("key.swordbot.fight", GLFW.GLFW_KEY_K, KeyMapping.Category.MISC);
		KeyMappingHelper.registerKeyMapping(fightKey);
	}

	public static void tick(Minecraft client) {
		if (fightKey != null && fightKey.consumeClick()) {
			enabled = !enabled;
			if (!enabled) disable(client);
		}
		if (!enabled || client.player == null) return;

		LivingEntity target = TargetManager.getTarget();
		if (target == null || !target.isAlive() || target == client.player) {
			target = acquireNearest(client);
		}

		// enforce a single target: overwrite any other selection every tick
		lockedTarget = target;
		TargetManager.setTarget(target);
	}

	public static LivingEntity getTarget() {
		return lockedTarget;
	}

	public static void disable(Minecraft client) {
		enabled = false;
		lockedTarget = null;
		TargetManager.clear();
		Brain.state = FightState.NEUTRAL;
		SmoothLook.stop();
		releaseKeys(client);
	}

	private static LivingEntity acquireNearest(Minecraft client) {
		if (client.level == null || client.player == null) return null;
		double best = Double.MAX_VALUE;
		LivingEntity closest = null;
		for (Entity entity : client.level.entitiesForRendering()) {
			if (!entity.isAlive() || entity == client.player) continue;
			if (!(entity instanceof LivingEntity living)) continue;
			double dist = client.player.distanceToSqr(entity);
			if (dist < best) {
				best = dist;
				closest = living;
			}
		}
		return closest;
	}

	private static void releaseKeys(Minecraft client) {
		for (MoveDirection direction : MoveDirection.values()) {
			MoveKeyUp.execute(client, direction);
		}
		SneakKeyUp.execute(client);
		EndSprint.execute(client);
	}
}