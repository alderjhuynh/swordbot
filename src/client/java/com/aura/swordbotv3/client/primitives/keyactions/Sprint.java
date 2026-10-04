package com.aura.swordbotv3.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class Sprint {
   public static void executeDown(Minecraft var0) {
      var0.options.keySprint.setDown(true);
   }

   public static void executeUp(Minecraft var0) {
      var0.options.keySprint.setDown(false);
   }
}
