package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

// kit inventory scan + behavior-set selection (v36).
public final class KitProfile {
    public enum Archetype {
        PLAIN,
        KB_DUAL,
        KB_ONLY
    }

    public Archetype type = Archetype.PLAIN;
    public int plainSlot = -1;
    public int kbSlot = -1;
    public int kbLevel;
    public boolean wieldKb;
    private boolean scanned;

    public void rescan(Minecraft client) {
        plainSlot = -1;
        kbSlot = -1;
        kbLevel = 0;
        wieldKb = false;
        scanned = true;
        if (client == null || client.player == null) {
            type = Archetype.PLAIN;
            return;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || kbLevelOf(s, client) > 0) {
                continue;
            }
            if (s.is(ItemTags.SWORDS)) {
                plainSlot = i;
                break;
            }
        }
        if (plainSlot < 0) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = inv.getItem(i);
                if (s.isEmpty() || kbLevelOf(s, client) > 0) {
                    continue;
                }
                if (s.is(ItemTags.AXES) || RecoverBrain.isWeapon(s)) {
                    plainSlot = i;
                    break;
                }
            }
        }
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            int kb = kbLevelOf(s, client);
            if (kb > 0 && (s.is(ItemTags.SWORDS) || s.is(ItemTags.AXES)
                    || RecoverBrain.isWeapon(s))) {
                kbSlot = i;
                kbLevel = kb;
                break;
            }
        }
        if (kbSlot < 0) {
            type = Archetype.PLAIN;
        } else if (plainSlot < 0) {
            type = Archetype.KB_ONLY;
        } else {
            type = Archetype.KB_DUAL;
        }
        refreshWield(client);
    }

    public void refreshWield(Minecraft client) {
        if (client == null || client.player == null || !scanned) {
            wieldKb = false;
            return;
        }
        int sel = client.player.getInventory().getSelectedSlot();
        wieldKb = kbSlot >= 0 && sel == kbSlot;
    }

    public boolean needsScan(int age) {
        return !scanned || age % 40 == 0;
    }

    public boolean hasKb() {
        return kbSlot >= 0;
    }

   public boolean kbMode() {
        return hasKb() && wieldKb;
    }

    public String kitName() {
        return switch (type) {
            case KB_DUAL -> "KB_DUAL";
            case KB_ONLY -> "KB_ONLY";
            default -> "PLAIN";
        };
    }

    public int doctrineSlot(Minecraft client, SenseState sense, BotConfig config) {
        if (!hasKb()) {
            return plainSlot;
        }
        int fallback = plainSlot >= 0 ? plainSlot : kbSlot;
        if (client == null || client.player == null || sense == null) {
            return fallback;
        }
        double myHp = sense.health + sense.absorption;
        boolean hasGap = RecoverBrain.findGapSlot(client) >= 0;
        boolean wantGap = myHp <= config.gapHpThreshold && hasGap;
        boolean critLow = myHp <= config.critHpThreshold && hasGap;
        boolean threatClose = sense.target != null && sense.targetVisible
                && sense.reachOp <= 4.5;
        if ((wantGap && threatClose) || critLow) {
            return kbSlot;
        }
        return fallback;
    }

    public boolean ensureWeaponForAttack(Minecraft client, SenseState sense,
            BotConfig config) {
        if (client == null || client.player == null) {
            return true;
        }
        int desired = doctrineSlot(client, sense, config);
        var inv = client.player.getInventory();
        if (desired < 0 || inv.getSelectedSlot() == desired) {
            return true;
        }
        inv.setSelectedSlot(desired);
        refreshWield(client);
        return false;
    }

    public void applyDoctrine(Minecraft client, SenseState sense, BotConfig config,
            RecoverBrain recover, BowBrain bow, Intents in) {
        if (client == null || client.player == null || sense == null) {
            return;
        }
        if (in.use || recover.mode() == 2 || bow.drawing()) {
            return;
        }
        int desired = doctrineSlot(client, sense, config);
        var inv = client.player.getInventory();
        if (desired >= 0 && inv.getSelectedSlot() != desired) {
            inv.setSelectedSlot(desired);
            refreshWield(client);
        }
    }

    private static int kbLevelOf(ItemStack stack, Minecraft client) {
        try {
            if (client == null || client.level == null || stack.isEmpty()) {
                return 0;
            }
            var lookup = client.level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT);
            var kb = lookup.getOrThrow(Enchantments.KNOCKBACK);
            return EnchantmentHelper.getItemEnchantmentLevel(kb, stack);
        } catch (Throwable ignored) {
            return 0;
        }
    }
}
