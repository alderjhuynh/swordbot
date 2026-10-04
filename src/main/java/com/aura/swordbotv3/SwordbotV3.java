package com.aura.swordbotv3;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwordbotV3 implements ModInitializer {
   public static final String MOD_ID = "swordbot-v3";
   public static final Logger LOGGER = LoggerFactory.getLogger("swordbot-v3");

   public void onInitialize() {
      LOGGER.info("God help us all...");
   }

   public static Identifier id(String var0) {
      return Identifier.fromNamespaceAndPath("swordbot-v3", var0);
   }
}
