package com.aura.swordbot.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class FindItem {
    public static int findSlotUnenchanted(Minecraft client, Item targetItem) {
        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);
            if (stack.is(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    public static int findSlotEnchanted(Minecraft client, ResourceKey<Enchantment> targetEnchantment, Item targetItem) {
        int bestSlot = -1;
        int bestLevel = -1;

        var ench = client.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(targetEnchantment);

        for (int i = 0; i <= 8; i++) {
            ItemStack stack = client.player.getInventory().getItem(i);

            if (!stack.is(targetItem)) continue;

            int level = stack.getEnchantments().getLevel(ench);

            if (level > bestLevel) {
                bestLevel = level;
                bestSlot = i;
            }
        }

        return bestSlot;
    }
}
