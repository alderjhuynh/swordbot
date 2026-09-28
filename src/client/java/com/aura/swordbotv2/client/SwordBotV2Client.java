package com.aura.swordbotv2.client;

import com.aura.swordbotv2.client.bot.BotConfig;
import com.aura.swordbotv2.client.bot.BotEngine;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

public class SwordBotV2Client implements ClientModInitializer {
    private final BotEngine engine = new BotEngine();
    private KeyMapping toggleKey;
    private KeyMapping targetKey;
    private KeyMapping skillKey;
    private KeyMapping styleKey;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.swordbot-v2.toggle", InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_G, KeyMapping.Category.GAMEPLAY));
        targetKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.swordbot-v2.target", InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_H, KeyMapping.Category.GAMEPLAY));
        skillKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.swordbot-v2.skill", InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_J, KeyMapping.Category.GAMEPLAY));
        styleKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.swordbot-v2.style", InputConstants.Type.KEYBOARD,
                        InputConstants.KEY_K, KeyMapping.Category.GAMEPLAY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (toggleKey.consumeClick()) {
                engine.setEnabled(client, !engine.enabled());
                client.player.sendSystemMessage(Component.literal(engine.statusLine()));
            }
            while (targetKey.consumeClick()) {
                engine.retarget(client);
                client.player.sendSystemMessage(Component.literal(engine.statusLine()));
            }
            while (skillKey.consumeClick()) {
                BotConfig.Skill[] skills = BotConfig.Skill.values();
                engine.config().skill =
                        skills[(engine.config().skill.ordinal() + 1) % skills.length];
                client.player.sendSystemMessage(Component.literal(engine.statusLine()));
            }
            while (styleKey.consumeClick()) {
                BotConfig.Style[] styles = BotConfig.Style.values();
                engine.config().style =
                        styles[(engine.config().style.ordinal() + 1) % styles.length];
                client.player.sendSystemMessage(Component.literal(engine.statusLine()));
            }
            engine.tick(client);
        });
    }
}
