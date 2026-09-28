package com.aura.swordbotv2.client.primitives.mouseactions;

import com.aura.swordbotv2.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class UseItem {
    public static void executeDown(Minecraft client) {
        KeyPress.press(client.options.keyUse);
    }

    public static void executeUp(Minecraft client) {
        KeyPress.release(client.options.keyUse);
    }
}
