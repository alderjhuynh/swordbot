package com.aura.swordbotv3.client.primitives.mouseactions;

import com.aura.swordbotv3.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class Attack {
   public static void executeDown(Minecraft var0) {
      KeyPress.press(var0.options.keyAttack);
   }

   public static void executeUp(Minecraft var0) {
      KeyPress.release(var0.options.keyAttack);
   }
}
