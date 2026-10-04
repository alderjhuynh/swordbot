package com.aura.swordbotv3.client;

import com.aura.swordbotv3.client.bot.BotConfig;
import com.aura.swordbotv3.client.bot.BotEngine;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;

public class SwordbotV3Client implements ClientModInitializer {
   private final BotEngine engine = new BotEngine();
   private KeyMapping toggleKey;
   private KeyMapping targetKey;
   private KeyMapping skillKey;
   private KeyMapping styleKey;
   private KeyMapping nnKey;

   public void onInitializeClient() {
      this.toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swordbot-v3.toggle", Type.KEYBOARD, 10, Category.GAMEPLAY));
      this.targetKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swordbot-v3.target", Type.KEYBOARD, 11, Category.GAMEPLAY));
      this.skillKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swordbot-v3.skill", Type.KEYBOARD, 13, Category.GAMEPLAY));
      this.styleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swordbot-v3.style", Type.KEYBOARD, 14, Category.GAMEPLAY));
      this.nnKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.swordbot-v3.nn", Type.KEYBOARD, 17, Category.GAMEPLAY));
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)var1 -> {
         if (var1.player != null) {
            if (Boolean.getBoolean("swordbot.lab")) {
               this.engine.config().cloneDump = true;
               boolean var2 = var1.gameMode != null && var1.gameMode.getPlayerMode() == GameType.SURVIVAL && var1.player.isAlive() && var1.gui.screen() == null;
               if (var2 != this.engine.enabled()) {
                  if (var2) {
                     var1.player.getInventory().setSelectedSlot(0);
                     var1.player.resetAttackStrengthTicker();
                  }

                  this.engine.setEnabled(var1, var2);
                  System.out.println("SWORDBOT_LAB_ACTIVE=" + var2);
               }
            }

            while (this.toggleKey.consumeClick()) {
               this.engine.setEnabled(var1, !this.engine.enabled());
               var1.player.sendSystemMessage(Component.literal(this.engine.statusLine()));
            }

            while (this.targetKey.consumeClick()) {
               this.engine.retarget(var1);
               var1.player.sendSystemMessage(Component.literal(this.engine.statusLine()));
            }

            while (this.skillKey.consumeClick()) {
               BotConfig.Skill[] var3 = BotConfig.Skill.values();
               this.engine.config().skill = var3[(this.engine.config().skill.ordinal() + 1) % var3.length];
               var1.player.sendSystemMessage(Component.literal(this.engine.statusLine()));
            }

            while (this.styleKey.consumeClick()) {
               BotConfig.Style[] var4 = BotConfig.Style.values();
               this.engine.config().style = var4[(this.engine.config().style.ordinal() + 1) % var4.length];
               this.engine.noteManualStyle();
               var1.player.sendSystemMessage(Component.literal(this.engine.statusLine()));
            }

            while (this.nnKey.consumeClick()) {
               this.engine.toggleNn(var1);
            }

            this.engine.tick(var1);
         }
      });
   }
}
