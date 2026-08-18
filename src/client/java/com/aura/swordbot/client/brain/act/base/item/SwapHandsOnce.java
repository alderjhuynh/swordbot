package com.aura.swordbot.client.brain.act.base.item;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;

public final class SwapHandsOnce {
	private SwapHandsOnce() {}

	public static void execute(Minecraft client) {
		if (client.player == null || client.getConnection() == null) return;

		client.getConnection().send(new ServerboundPlayerActionPacket(
				ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND,
				BlockPos.ZERO,
				Direction.DOWN
		));
	}
}