package com.aura.swordbotv2.client.primitives.keyactions;

import com.aura.swordbotv2.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class OpenInventory {
    public static void executeDown(Minecraft client) {
        KeyPress.press(client.options.keyInventory);
    }

    public static void executeUp(Minecraft client) {
        KeyPress.release(client.options.keyInventory);
    }
}
