package com.aura.swordbot.client.brain.info;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class selfStats {

	public static Vec3 pos;
	public static Vec3 velocity;
	public static float health;
	public static float maxHealth;
	public static int armor;
	public static int food;
	public static float saturation;
	public static ItemStack mainHandItem;
	public static ItemStack offhandItem;
	public static boolean sprinting;
	public static boolean sneaking;
	public static boolean onGround;
	public static boolean jumping;
	public static boolean swimming;
	public static boolean flying;
	public static boolean alive;
	public static float yaw;
	public static float pitch;
	public static float attackCooldown;
	public static double movementSpeed;
	public static double stepHeight;
	public static double attackDamage;
	public static double attackSpeed;
	public static double entityInteractionRange;
	public static double blockInteractionRange;

	public static void tick(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null) return;

		pos = player.position();
		velocity = player.getDeltaMovement();
		health = player.getHealth();
		maxHealth = player.getMaxHealth();
		armor = player.getArmorValue();
		food = player.getFoodData().getFoodLevel();
		saturation = player.getFoodData().getSaturationLevel();
		mainHandItem = player.getMainHandItem();
		offhandItem = player.getOffhandItem();
		sprinting = player.isSprinting();
		sneaking = player.isShiftKeyDown();
		onGround = player.onGround();
		jumping = player.isJumping();
		swimming = player.isSwimming();
		flying = player.getAbilities().flying;
		alive = player.isAlive();
		yaw = player.getYRot();
		pitch = player.getXRot();
		attackCooldown = player.getAttackStrengthScale(0.0f);
		movementSpeed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
		stepHeight = player.maxUpStep();
		attackDamage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
		attackSpeed = player.getAttributeValue(Attributes.ATTACK_SPEED);
		entityInteractionRange = player.entityInteractionRange();
		blockInteractionRange = player.blockInteractionRange();
	}
}