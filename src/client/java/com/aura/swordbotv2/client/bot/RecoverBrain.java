package com.aura.swordbotv2.client.bot;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class RecoverBrain {
    private int mode;
    private int modeAge;
    private int giveUpUntil;

    boolean tick(Minecraft client, SenseState sense, BotConfig config, Intents out, int age) {
        out.clear();
        if (!config.allowEat || client.player == null) {
            mode = 0;
            return false;
        }
        double myHp = sense.health + sense.absorption;
        int want = wantLevel(client, sense, config, myHp);
        double window = window(sense, age);

        if (mode == 3 && age >= giveUpUntil) {
            mode = 0;
        }
        if (mode == 0) {
            if (want == 0) {
                return false;
            }
            if (mealWindow(want, window)) {
                if (startMeal(client, sense, want, age)) {
                    return eatTick(sense, out, age);
                }
            }
            return false;
        }
        if (mode == 2) {
            int selected = client.player.getInventory().getSelectedSlot();
            if (want == 0 || !isEdibleNow(client, selected, sense.food)
                    || age - modeAge >= 60) {
                mode = 0;
                selectWeapon(client);
                return false;
            }
            return eatTick(sense, out, age);
        }
        return false;
    }

    private static boolean eatTick(SenseState sense, Intents out, int age) {
        out.f = -1;
        out.use = true;
        return true;
    }

    private static boolean mealWindow(int want, double window) {
        return (want == 1 && window >= 36) || (want >= 2 && window >= 20);
    }

    private boolean startMeal(Minecraft client, SenseState sense, int want, int age) {
        int slot;
        if (want >= 2) {
            slot = findGapSlot(client);
            if (slot < 0) {
                return false;
            }
        } else {
            slot = findRegularFoodSlot(client);
            if (slot < 0) {
                slot = findGapSlot(client);
            }
            if (slot < 0 || !isEdibleNow(client, slot, sense.food)) {
                return false;
            }
        }
        mode = 2;
        modeAge = age;
        selectSlot(client, slot);
        return true;
    }

    private static int wantLevel(Minecraft client, SenseState sense, BotConfig config, double myHp) {
        boolean hasFood = findFoodSlot(client, false) >= 0;
        boolean hasGap = findGapSlot(client) >= 0;
        if (myHp <= config.critHpThreshold && hasGap) {
            return 3;
        }
        if (myHp <= config.gapHpThreshold && hasGap) {
            return 2;
        }
        if (sense.food <= 17 && hasFood) {
            return 1;
        }
        return 0;
    }

    private static double window(SenseState sense, int age) {
        if (sense.target == null || !sense.targetVisible) {
            return 200.0;
        }
        int ready = MeleeBrain.opponentReadyIn(sense, age);
        double closing = 0.22 + Math.max(0, sense.radialVel);
        double dist = Math.max(0, sense.reachOp - 3.0) / Math.max(0.05, closing);
        return Math.max(ready, dist);
    }

    static int findFoodSlot(Minecraft client, boolean gapsOnly) {
        if (client.player == null) {
            return -1;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE)) {
                return i;
            }
            if (!gapsOnly && s.getComponents().has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }

    static int findGapSlot(Minecraft client) {
        if (client.player == null) {
            return -1;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()
                    && (s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE))) {
                return i;
            }
        }
        return -1;
    }

    static int findRegularFoodSlot(Minecraft client) {
        if (client.player == null) {
            return -1;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || isGap(s)) {
                continue;
            }
            if (s.getComponents().has(DataComponents.FOOD)) {
                return i;
            }
        }
        return -1;
    }

    static boolean isEdibleNow(Minecraft client, int slot, int foodLevel) {
        if (client.player == null || slot < 0 || slot >= 9) {
            return false;
        }
        ItemStack s = client.player.getInventory().getItem(slot);
        if (s.isEmpty()) {
            return false;
        }
        if (isGap(s)) {
            return true;
        }
        return s.getComponents().has(DataComponents.FOOD) && foodLevel < 20;
    }

    private static boolean isGap(ItemStack s) {
        return s.is(Items.GOLDEN_APPLE) || s.is(Items.ENCHANTED_GOLDEN_APPLE);
    }

    static int findBowSlot(Minecraft client) {
        if (client.player == null) {
            return -1;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (!inv.getItem(i).isEmpty() && inv.getItem(i).is(Items.BOW)) {
                return i;
            }
        }
        return -1;
    }

    static boolean hasArrows(Minecraft client) {
        if (client.player == null) {
            return false;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && (s.is(Items.ARROW) || s.is(Items.SPECTRAL_ARROW))) {
                return true;
            }
        }
        return false;
    }

    private static void selectSlot(Minecraft client, int slot) {
        client.player.getInventory().setSelectedSlot(slot);
    }

    int mode() {
        return mode;
    }

    static boolean isWeapon(ItemStack s) {
        return !s.isEmpty() && s.getComponents().has(DataComponents.WEAPON);
    }

    static int findWeaponSlot(Minecraft client) {
        if (client.player == null) {
            return -1;
        }
        var inv = client.player.getInventory();
        for (int i = 0; i < 9; i++) {
            if (isWeapon(inv.getItem(i))) {
                return i;
            }
        }
        return -1;
    }

    static void selectWeapon(Minecraft client) {
        if (client.player == null) {
            return;
        }
        var inv = client.player.getInventory();
        if (isWeapon(inv.getItem(inv.getSelectedSlot()))) {
            return;
        }
        int w = findWeaponSlot(client);
        if (w >= 0) {
            inv.setSelectedSlot(w);
        }
    }

    static boolean ensureWeaponForAttack(Minecraft client) {
        if (client.player == null) {
            return true;
        }
        var inv = client.player.getInventory();
        if (isWeapon(inv.getItem(inv.getSelectedSlot()))) {
            return true;
        }
        int w = findWeaponSlot(client);
        if (w < 0) {
            return true;
        }
        inv.setSelectedSlot(w);
        return false;
    }

    void reset() {
        mode = 0;
        modeAge = 0;
        giveUpUntil = 0;
    }
}
