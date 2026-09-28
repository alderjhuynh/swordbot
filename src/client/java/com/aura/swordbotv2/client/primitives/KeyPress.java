package com.aura.swordbotv2.client.primitives;

import com.aura.swordbotv2.client.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;

public final class KeyPress {
    private KeyPress() {
    }

    public static void press(KeyMapping mapping) {
        mapping.setDown(true);
        KeyMappingAccessor accessor = (KeyMappingAccessor) (Object) mapping;
        accessor.swordbotv2$setClickCount(accessor.swordbotv2$getClickCount() + 1);
    }

    public static void release(KeyMapping mapping) {
        mapping.setDown(false);
    }
}
