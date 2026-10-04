package com.aura.swordbotv3.client.bot;

import com.aura.swordbotv3.client.mixin.MinecraftInputInvoker;
import com.aura.swordbotv3.client.primitives.lookactions.LookAtRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

final class Motor {
   boolean apply(Minecraft var1, Intents var2, Aim var3, BotConfig var4, boolean var5, LivingEntity var6) {
      boolean var7 = false;
      if (var1.player != null && var1.options != null) {
         if (var5) {
            float[] var8 = var3.step(var4);
            LookAtRotation.execute(var1, var8[0], var8[1]);
         }

         var1.options.keyUp.setDown(var2.f > 0);
         var1.options.keyDown.setDown(var2.f < 0);
         var1.options.keyLeft.setDown(var2.s > 0);
         var1.options.keyRight.setDown(var2.s < 0);
         var1.options.keyJump.setDown(var2.jump);
         var1.options.keySprint.setDown(var2.sprint && var2.f > 0);
         if (var2.attack && !var1.player.isUsingItem()) {
            MinecraftInputInvoker var10 = (MinecraftInputInvoker)var1;
            var10.swordbotv3$pick(1.0F);
            if (var6 != null && var1.crosshairPickEntity == var6) {
               float var9 = var1.player.getAttackStrengthScale(0.5F);
               var10.swordbotv3$startAttack();
               var7 = var1.player.getAttackStrengthScale(0.5F) < var9;
            }

            var1.options.keyAttack.setDown(false);
         } else {
            var1.options.keyAttack.setDown(false);
         }

         var1.options.keyUse.setDown(var2.use);
      }

      return var7;
   }

   void releaseAll(Minecraft var1) {
      if (var1.options != null) {
         var1.options.keyUp.setDown(false);
         var1.options.keyDown.setDown(false);
         var1.options.keyLeft.setDown(false);
         var1.options.keyRight.setDown(false);
         var1.options.keyJump.setDown(false);
         var1.options.keySprint.setDown(false);
         var1.options.keyAttack.setDown(false);
         var1.options.keyUse.setDown(false);
      }
   }
}
