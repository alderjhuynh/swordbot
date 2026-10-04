package com.aura.swordbotv3.client.primitives.lookactions;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

public class LookAtRotation {
   public static void execute(Minecraft var0, float var1, float var2) {
      if (var0.player != null) {
         var0.player.setYRot(Mth.wrapDegrees(var1));
         var0.player.setXRot(Mth.clamp(Mth.wrapDegrees(var2), -90.0F, 90.0F));
      }
   }
}
