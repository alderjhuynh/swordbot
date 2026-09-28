package com.aura.swordbotv2.client.primitives.mouseactions;

import com.aura.swordbotv2.client.primitives.KeyPress;
import net.minecraft.client.Minecraft;

public class Attack {
    public static void executeDown(Minecraft client) {
        KeyPress.press(client.options.keyAttack);
    }

    public static void executeUp(Minecraft client) {
        KeyPress.release(client.options.keyAttack);
    }
}
