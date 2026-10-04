package com.aura.swordbotv3.client.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
   @Accessor("clickCount")
   int swordbotv3$getClickCount();

   @Accessor("clickCount")
   void swordbotv3$setClickCount(int var1);
}
