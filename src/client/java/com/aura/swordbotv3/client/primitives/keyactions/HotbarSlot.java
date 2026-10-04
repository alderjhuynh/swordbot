package com.aura.swordbotv3.client.primitives.keyactions;

import com.aura.swordbotv3.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class HotbarSlot {
   public static void executeDown(Minecraft var0, int var1) {
      if (var1 >= 0 && var1 < var0.options.keyHotbarSlots.length) {
         KeyPress.press(var0.options.keyHotbarSlots[var1]);
      } else {
         throw new IllegalArgumentException("Hotbar slot out of range: " + var1);
      }
   }

   public static void executeUp(Minecraft var0, int var1) {
      if (var1 >= 0 && var1 < var0.options.keyHotbarSlots.length) {
         KeyPress.release(var0.options.keyHotbarSlots[var1]);
      } else {
         throw new IllegalArgumentException("Hotbar slot out of range: " + var1);
      }
   }
}
