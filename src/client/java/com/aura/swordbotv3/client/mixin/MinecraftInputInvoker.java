package com.aura.swordbotv3.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftInputInvoker {
   @Invoker("pick")
   void swordbotv3$pick(float var1);

   @Invoker("startAttack")
   boolean swordbotv3$startAttack();
}
