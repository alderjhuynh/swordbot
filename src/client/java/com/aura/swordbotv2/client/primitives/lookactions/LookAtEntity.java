package com.aura.swordbotv2.client.primitives.lookactions;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.Entity;

public class LookAtEntity {
    public static void execute(Minecraft client, Entity target) {
        if (client.player == null || target == null) {
            return;
        }
        client.player.lookAt(
                EntityAnchorArgument.Anchor.EYES,
                EntityAnchorArgument.Anchor.EYES.apply(target));
    }
}
