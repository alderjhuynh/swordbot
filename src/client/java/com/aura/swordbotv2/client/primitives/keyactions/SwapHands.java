package com.aura.swordbotv2.client.primitives.keyactions;

import com.aura.swordbotv2.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class SwapHands {
    public static void executeDown(Minecraft client) {
        KeyPress.press(client.options.keySwapOffhand);
    }

    public static void executeUp(Minecraft client) {
        KeyPress.release(client.options.keySwapOffhand);
    }
}
