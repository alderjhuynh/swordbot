package com.aura.swordbotv3.client.primitives.keyactions;

import net.minecraft.client.Minecraft;

public class MoveBack {
   public static void executeDown(Minecraft var0) {
      var0.options.keyDown.setDown(true);
   }

   public static void executeUp(Minecraft var0) {
      var0.options.keyDown.setDown(false);
   }
}
