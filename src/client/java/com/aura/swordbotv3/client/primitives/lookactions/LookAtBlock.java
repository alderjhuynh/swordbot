package com.aura.swordbotv3.client.primitives.lookactions;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class LookAtBlock {
   public static void execute(Minecraft var0, BlockPos var1) {
      if (var0.player != null) {
         var0.player.lookAt(Anchor.EYES, Vec3.atCenterOf(var1));
      }
   }
}
