package com.aura.swordbot.client.brain.info;

import com.aura.swordbot.client.util.TargetManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class targetStats {

	public static Vec3 targetPos;
	public static Float targetHealth;
	public static int targetArmor;
	public static Double distanceToTargetSq;
	public static Double targetJumping;
	public static Vec3 targetVelocity;
	public static ItemStack targetHeldItem;
	public static boolean targetSprinting;
	public static boolean targetSneaking;
	public static boolean targetOnGround;
	public static float targetYaw;
	public static float targetPitch;

	public static void tick(Minecraft client) {
		if (client.player == null || TargetManager.getTarget() == null) return;

		LivingEntity target = TargetManager.getTarget();

		targetPos = target.position();
		targetHealth = target.getHealth();
		targetArmor = target.getArmorValue();
		distanceToTargetSq = client.player.distanceToSqr(target);
		targetJumping = (double) (target.isJumping() ? 1 : 0);
		targetVelocity = target.getDeltaMovement();
		targetHeldItem = target.getMainHandItem();
		targetSprinting = target.isSprinting();
		targetSneaking = target.isShiftKeyDown();
		targetOnGround = target.onGround();
		targetYaw = target.getYRot();
		targetPitch = target.getXRot();
	}
}