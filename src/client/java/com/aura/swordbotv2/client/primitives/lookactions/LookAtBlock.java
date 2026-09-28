package com.aura.swordbotv2.client.primitives.lookactions;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class LookAtBlock {
    public static void execute(Minecraft client, BlockPos pos) {
        if (client.player == null) {
            return;
        }
        client.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
    }
}
