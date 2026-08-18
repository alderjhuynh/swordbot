package com.aura.swordbot.client.brain.act.base.attack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class AttackOnce {
    public static void execute(Minecraft client) {
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        HitResult hit = client.hitResult;

        if (player == null || gameMode == null || hit == null) return;

        switch (hit.getType()) {
            case ENTITY -> {
                EntityHitResult entityHit = (EntityHitResult) hit;
                gameMode.attack(player, entityHit.getEntity());
                player.swing(InteractionHand.MAIN_HAND);
            }
            case BLOCK -> {
                BlockHitResult blockHit = (BlockHitResult) hit;
                gameMode.startDestroyBlock(blockHit.getBlockPos(), blockHit.getDirection());
                player.swing(InteractionHand.MAIN_HAND);
            }
            case MISS -> player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
