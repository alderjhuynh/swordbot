package com.aura.swordbotv3.client.primitives;

import com.aura.swordbotv3.client.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;

public final class KeyPress {
   private KeyPress() {
   }

   public static void press(KeyMapping var0) {
      var0.setDown(true);
      KeyMappingAccessor var1 = (KeyMappingAccessor)var0;
      var1.swordbotv3$setClickCount(var1.swordbotv3$getClickCount() + 1);
   }

   public static void release(KeyMapping var0) {
      var0.setDown(false);
   }
}
