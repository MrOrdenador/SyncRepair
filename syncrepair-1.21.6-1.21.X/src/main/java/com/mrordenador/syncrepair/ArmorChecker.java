package com.mrordenador.syncrepair;

import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;

import java.util.ArrayList;
import java.util.List;

public class ArmorChecker {
    public static int totalArmorDamage = 0;
    public static int damageNeededToReachCap = 0;
    public static boolean shouldRepair = false;

    public static ArmorPieceInfo helmetInfo = new ArmorPieceInfo();
    public static ArmorPieceInfo chestplateInfo = new ArmorPieceInfo();
    public static ArmorPieceInfo leggingsInfo = new ArmorPieceInfo();
    public static ArmorPieceInfo bootsInfo = new ArmorPieceInfo();

    public static class ArmorPieceInfo {
        public int currentDurability = 0;
        public int maxDurability = 0;
        public int damage = 0;
        public boolean hasMending = false;
        public ItemStack itemStack = ItemStack.EMPTY;

        public float getDurabilityPercentage() {
            if (maxDurability == 0) return 0f;
            return (float) currentDurability / maxDurability;
        }
    }

    private static int getDamage(ItemStack stack) {
        if (!stack.isDamageable())
            return 0;
        return stack.getDamage();
    }

    private static int getMaxDamage(ItemStack stack) {
        if (!stack.isDamageable())
            return 0;
        return stack.getMaxDamage();
    }

    private static int getCurrentDurability(ItemStack stack) {
        if (!stack.isDamageable())
            return 0;
        return stack.getMaxDamage() - stack.getDamage();
    }

    private static boolean hasMending(ItemStack stack) {
        if (stack.isEmpty())
            return false;
        ItemEnchantmentsComponent enchantments = EnchantmentHelper.getEnchantments(stack);
        for (RegistryEntry<Enchantment> entry : enchantments.getEnchantments()) {
            if (entry.getKey().isPresent() && entry.getKey().get().equals(Enchantments.MENDING)) {
                return enchantments.getLevel(entry) > 0;
            }
        }
        return false;
    }

    public static boolean hasMendingStatic(ItemStack stack) {
        return hasMending(stack);
    }

    private static void updateArmorPieceInfo(ArmorPieceInfo info, ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageable()) {
            info.currentDurability = 0;
            info.maxDurability = 0;
            info.damage = 0;
            info.hasMending = false;
            info.itemStack = ItemStack.EMPTY;
        } else {
            info.currentDurability = getCurrentDurability(stack);
            info.maxDurability = getMaxDamage(stack);
            info.damage = getDamage(stack);
            info.hasMending = hasMending(stack);
            info.itemStack = stack.copy();
        }
    }

    public static void checkArmorDamage(PlayerEntity player, SyncRepairConfig config) {
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

        updateArmorPieceInfo(helmetInfo, helmet);
        updateArmorPieceInfo(chestplateInfo, chestplate);
        updateArmorPieceInfo(leggingsInfo, leggings);
        updateArmorPieceInfo(bootsInfo, boots);

        List<ItemStack> mendingArmorPieces = new ArrayList<>();
        int currentTotalDamage = 0;
        int minMaxDurability = Integer.MAX_VALUE;

        ItemStack[] allArmorPieces = {helmet, chestplate, leggings, boots};
        for (ItemStack piece : allArmorPieces) {
            if (hasMending(piece) && piece.isDamageable()) {
                mendingArmorPieces.add(piece);
                currentTotalDamage += getDamage(piece);
                minMaxDurability = Math.min(minMaxDurability, getMaxDamage(piece));
            }
        }

        totalArmorDamage = currentTotalDamage;

        int calculatedDamageNeededToReachCap = 0;
        if (!mendingArmorPieces.isEmpty() && minMaxDurability != Integer.MAX_VALUE) {
            for (ItemStack piece : mendingArmorPieces) {
                int currentDurability = getCurrentDurability(piece);
                if (currentDurability < minMaxDurability) {
                    calculatedDamageNeededToReachCap += (minMaxDurability - currentDurability);
                }
            }
        }

        damageNeededToReachCap = calculatedDamageNeededToReachCap;

        int xpBase = config.getXPCalculationBase();
        int lowThreshold = (xpBase * 800 / 896);
        int highThreshold = xpBase;

        shouldRepair = (damageNeededToReachCap >= lowThreshold && damageNeededToReachCap <= highThreshold) || damageNeededToReachCap >= highThreshold;
    }

    public static void checkArmorDamage(PlayerEntity player) {
        SyncRepairConfig defaultConfig = new SyncRepairConfig();
        checkArmorDamage(player, defaultConfig);
    }
}
