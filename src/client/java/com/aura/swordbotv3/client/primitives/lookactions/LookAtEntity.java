package com.aura.swordbotv3.client.primitives.lookactions;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.world.entity.Entity;

public class LookAtEntity {
   public static void execute(Minecraft var0, Entity var1) {
      if (var0.player != null && var1 != null) {
         var0.player.lookAt(Anchor.EYES, Anchor.EYES.apply(var1));
      }
   }
}
