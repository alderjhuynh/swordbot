package com.aura.swordbotv2.client.primitives.keyactions;

import com.aura.swordbotv2.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class HotbarSlot {
    public static void executeDown(Minecraft client, int slot) {
        if (slot < 0 || slot >= client.options.keyHotbarSlots.length) {
            throw new IllegalArgumentException("Hotbar slot out of range: " + slot);
        }
        KeyPress.press(client.options.keyHotbarSlots[slot]);
    }

    public static void executeUp(Minecraft client, int slot) {
        if (slot < 0 || slot >= client.options.keyHotbarSlots.length) {
            throw new IllegalArgumentException("Hotbar slot out of range: " + slot);
        }
        KeyPress.release(client.options.keyHotbarSlots[slot]);
    }
}
