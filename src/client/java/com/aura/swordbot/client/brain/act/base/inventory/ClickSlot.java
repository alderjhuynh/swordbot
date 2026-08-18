package com.aura.swordbot.client.brain.act.base.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;

public final class ClickSlot {

	private ClickSlot() {}

	public static void click(Minecraft client, int slotId, int button, ContainerInput input) {
		Player player = client.player;
		MultiPlayerGameMode gameMode = client.gameMode;
		if (player == null || gameMode == null) return;

		AbstractContainerMenu menu = player.containerMenu;
		gameMode.handleContainerInput(menu.containerId, slotId, button, input, player);
	}

	public static void pickup(Minecraft client, int slotId) {
		click(client, slotId, 0, ContainerInput.PICKUP);
	}

	public static void pickupAll(Minecraft client, int slotId) {
		click(client, slotId, 0, ContainerInput.PICKUP_ALL);
	}

	public static void quickMove(Minecraft client, int slotId) {
		click(client, slotId, 0, ContainerInput.QUICK_MOVE);
	}

	public static void dropOne(Minecraft client, int slotId) {
		click(client, slotId, 0, ContainerInput.THROW);
	}

	public static void dropStack(Minecraft client, int slotId) {
		click(client, slotId, 1, ContainerInput.THROW);
	}

	public static void swapWithHotbar(Minecraft client, int slotId, int hotbarSlot) {
		click(client, slotId, hotbarSlot, ContainerInput.SWAP);
	}

	public static void selectHotbar(Minecraft client, int hotbarSlot) {
		if (client.player == null) return;
		client.player.getInventory().setSelectedSlot(hotbarSlot);
	}

	public static AbstractContainerMenu menu(Minecraft client) {
		return client.player != null ? client.player.containerMenu : null;
	}

	public static Slot slot(Minecraft client, int slotId) {
		AbstractContainerMenu menu = menu(client);
		return menu != null ? menu.getSlot(slotId) : null;
	}
}