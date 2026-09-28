package com.aura.swordbotv2.client.bot;

import com.aura.swordbotv2.client.primitives.KeyPress;
import com.aura.swordbotv2.client.primitives.lookactions.LookAtRotation;
import net.minecraft.client.Minecraft;

final class Motor {
    void apply(Minecraft client, Intents in, Aim aim, BotConfig config, boolean faceTarget) {
        if (client.player == null || client.options == null) {
            return;
        }
        if (faceTarget) {
            float[] stepped = aim.step(config);
            LookAtRotation.execute(client, stepped[0], stepped[1]);
        }

        client.options.keyUp.setDown(in.f > 0);
        client.options.keyDown.setDown(in.f < 0);
        client.options.keyLeft.setDown(in.s > 0);
        client.options.keyRight.setDown(in.s < 0);
        client.options.keyJump.setDown(in.jump);
        client.options.keySprint.setDown(in.sprint && in.s == 0);

        if (in.attack) {
            KeyPress.press(client.options.keyAttack);
        } else {
            client.options.keyAttack.setDown(false);
        }
        client.options.keyUse.setDown(in.use);
    }

    void releaseAll(Minecraft client) {
        if (client.options == null) {
            return;
        }
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keySprint.setDown(false);
        client.options.keyAttack.setDown(false);
        client.options.keyUse.setDown(false);
    }
}
