package com.aura.swordbotv2.client.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int swordbotv2$getClickCount();

    @Accessor("clickCount")
    void swordbotv2$setClickCount(int clickCount);
}
