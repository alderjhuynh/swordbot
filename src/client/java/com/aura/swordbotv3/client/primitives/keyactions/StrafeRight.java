package com.aura.swordbotv3.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class StrafeRight {
   public static void executeDown(Minecraft var0) {
      var0.options.keyRight.setDown(true);
   }

   public static void executeUp(Minecraft var0) {
      var0.options.keyRight.setDown(false);
   }
}
