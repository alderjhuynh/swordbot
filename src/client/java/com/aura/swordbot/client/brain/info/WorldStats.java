package com.aura.swordbot.client.brain.info;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class WorldStats {

	public static ResourceKey<Level> dimension;
	public static float tps = 20f;

	private static long lastGameTime = -1;
	private static long lastNanoTime = -1;

	public static void tick(Minecraft client) {
		if (client.level == null) {
			tps = 0f;
			dimension = null;
			lastGameTime = -1;
			lastNanoTime = -1;
			return;
		}

		dimension = client.level.dimension();

		long gameTime = client.level.getLevelData().getGameTime();
		long now = System.nanoTime();

		if (lastGameTime >= 0 && now > lastNanoTime) {
			float estimate = (float) ((gameTime - lastGameTime) * 1_000_000_000L / (now - lastNanoTime));
			if (estimate > 0f && estimate < 60f) {
				tps = (tps * 19f + estimate) / 20f;
			}
		}

		lastGameTime = gameTime;
		lastNanoTime = now;
	}

	public static BlockState blockAt(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		return client.level != null ? client.level.getBlockState(pos) : null;
	}

	public static BlockState blockAt(Vec3 pos) {
		return blockAt(BlockPos.containing(pos));
	}

	public static boolean isAir(BlockPos pos) {
		BlockState state = blockAt(pos);
		return state == null || state.isAir();
	}

	public static boolean isReachable(BlockPos pos) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (player == null) return false;
		return player.isWithinBlockInteractionRange(pos, 0.0);
	}

	public static List<BlockPos> blocksInReach() {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (client.level == null || player == null) return List.of();

		int radius = (int) Math.ceil(player.blockInteractionRange());
		BlockPos center = player.blockPosition();
		List<BlockPos> result = new ArrayList<>();

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					BlockPos pos = center.offset(dx, dy, dz);
					BlockState state = client.level.getBlockState(pos);
					if (!state.isAir() && player.isWithinBlockInteractionRange(pos, 0.0)) {
						result.add(pos);
					}
				}
			}
		}
		return result;
	}

	public static List<Entity> entitiesInRange(double radius) {
		return entitiesInRange(radius, entity -> true);
	}

	public static List<Entity> entitiesInRange(double radius, Predicate<Entity> predicate) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) return List.of();

		double radiusSq = radius * radius;
		Vec3 center = client.player.position();
		List<Entity> result = new ArrayList<>();

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!entity.isAlive() || entity == client.player) continue;
			if (entity.distanceToSqr(center) > radiusSq) continue;
			if (predicate.test(entity)) result.add(entity);
		}
		return result;
	}

	public static Entity closestEntity(double radius, Predicate<Entity> predicate) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null) return null;

		double radiusSq = radius * radius;
		Vec3 center = client.player.position();
		Entity closest = null;
		double best = Double.MAX_VALUE;

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!entity.isAlive() || entity == client.player) continue;
			if (!predicate.test(entity)) continue;
			double distSq = entity.distanceToSqr(center);
			if (distSq > radiusSq || distSq >= best) continue;
			best = distSq;
			closest = entity;
		}
		return closest;
	}

	public static BlockHitResult raycast(Vec3 from, Vec3 to) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return null;

		ClipContext context = new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player);
		return client.level.clip(context);
	}

	public static BlockHitResult raycastFromEye(double distance) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (client.level == null || player == null) return null;

		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getViewVector(1.0f).scale(distance));
		return raycast(from, to);
	}

	public static Entity entityRaycast(double distance) {
		Minecraft client = Minecraft.getInstance();
		LocalPlayer player = client.player;
		if (client.level == null || player == null) return null;

		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getViewVector(1.0f).scale(distance));
		return entityRaycast(from, to, entity -> entity != player && entity.isAlive());
	}

	public static Entity entityRaycast(Vec3 from, Vec3 to, Predicate<Entity> predicate) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null) return null;

		Entity closest = null;
		double best = Double.MAX_VALUE;

		for (Entity entity : client.level.entitiesForRendering()) {
			if (!entity.isAlive() || !predicate.test(entity)) continue;
			AABB box = entity.getBoundingBox();
			if (!box.intersects(from, to)) continue;
			Vec3 hit = box.clip(from, to).orElse(null);
			if (hit == null) continue;
			double distSq = hit.distanceToSqr(from);
			if (distSq >= best) continue;
			best = distSq;
			closest = entity;
		}
		return closest;
	}

	public static HitResult crosshair() {
		return Minecraft.getInstance().hitResult;
	}
}