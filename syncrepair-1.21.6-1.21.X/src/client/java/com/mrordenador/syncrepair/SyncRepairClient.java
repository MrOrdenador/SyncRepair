package com.mrordenador.syncrepair;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.joml.Matrix3x2fStack;

public class SyncRepairClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AutoConfig.register(SyncRepairConfig.class, GsonConfigSerializer::new);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            var player = MinecraftClient.getInstance().player;
            if (player != null) {
                SyncRepairConfig config = AutoConfig.getConfigHolder(SyncRepairConfig.class).getConfig();
                ArmorChecker.checkArmorDamage(player, config);
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.textRenderer != null) {
                SyncRepairConfig config = AutoConfig.getConfigHolder(SyncRepairConfig.class).getConfig();
                if (config.enableDamageStatusTexts) {
                    renderTopTexts(drawContext, client, config);
                }
                if (config.enableArmorDurabilityHUD) {
                    renderArmorDurabilityHUD(drawContext, client, config);
                }
            }
        });
    }

    private void renderTopTexts(DrawContext drawContext, MinecraftClient client, SyncRepairConfig config) {
        int damageNeeded = ArmorChecker.damageNeededToReachCap;
        int currentTotalDamage = ArmorChecker.totalArmorDamage;
        int xpBase = config.getXPCalculationBase();

        if (damageNeeded >= 500 || currentTotalDamage > 0) {
            String repairText;
            int repairColor;

            if (damageNeeded >= xpBase) {
                repairText = "Best time to repair";
                repairColor = 0xFFFF00FF;
            } else if (damageNeeded >= (xpBase * 800 / 896)) {
                repairText = "Good time to repair";
                repairColor = 0xFFFF0000;
            } else if (damageNeeded >= (xpBase * 700 / 896)) {
                repairText = "Near optimal repair";
                repairColor = 0xFFFF8000;
            } else if (damageNeeded >= (xpBase * 500 / 896)) {
                repairText = "Moderated damage";
                repairColor = 0xFFFFFF00;
            } else if (damageNeeded >= (xpBase * 250 / 896)) {
                repairText = "Noticeable damage";
                repairColor = 0xFF00AA00;
            } else if (damageNeeded > 0) {
                repairText = "Minor damage";
                repairColor = 0xFF00FF00;
            } else {
                repairText = "Armor is fully repaired";
                repairColor = 0xFF888888;
            }

            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            int y = config.getTopTextsPositionY() < 0 ? screenHeight + config.getTopTextsPositionY() : config.getTopTextsPositionY();

            int repairX;
            if (config.getTopTextsPositionX() == 0) {
                int repairTextWidth = client.textRenderer.getWidth(repairText);
                repairX = (screenWidth - repairTextWidth) / 2;
            } else {
                int posX = config.getTopTextsPositionX() < 0 ? screenWidth + config.getTopTextsPositionX() : config.getTopTextsPositionX();
                repairX = posX;
            }

            drawContext.drawText(client.textRenderer, repairText, repairX, y, repairColor, true);

            if (config.showAdvancedInformation) {
                String armorDamageText = "Armor Damage: " + currentTotalDamage;
                String xpBottlesText = "XP Bottles needed: " + calculateTotalXPBottlesNeeded(client.player, config);
                String modeText = "Mode: " + getModeDisplayText(config);

                int armorDamageX, xpBottlesX, modeX;
                if (config.getTopTextsPositionX() == 0) {
                    int armorDamageWidth = client.textRenderer.getWidth(armorDamageText);
                    int xpBottlesWidth = client.textRenderer.getWidth(xpBottlesText);
                    int modeWidth = client.textRenderer.getWidth(modeText);
                    armorDamageX = (screenWidth - armorDamageWidth) / 2;
                    xpBottlesX = (screenWidth - xpBottlesWidth) / 2;
                    modeX = (screenWidth - modeWidth) / 2;
                } else {
                    int posX = config.getTopTextsPositionX() < 0 ? screenWidth + config.getTopTextsPositionX() : config.getTopTextsPositionX();
                    armorDamageX = posX;
                    xpBottlesX = posX;
                    modeX = posX;
                }

                Matrix3x2fStack matrices = drawContext.getMatrices();
                matrices.pushMatrix();
                matrices.scale(0.9f, 0.9f);

                int scaledArmorDamageX = (int) (armorDamageX / 0.9f);
                int scaledXpBottlesX = (int) (xpBottlesX / 0.9f);
                int scaledModeX = (int) (modeX / 0.9f);
                int scaledY1 = (int) ((y + 14) / 0.9f);
                int scaledY2 = (int) ((y + 26) / 0.9f);
                int scaledY3 = (int) ((y + 38) / 0.9f);

                drawContext.drawText(client.textRenderer, armorDamageText, scaledArmorDamageX, scaledY1, 0xFF888888, true);
                drawContext.drawText(client.textRenderer, xpBottlesText, scaledXpBottlesX, scaledY2, 0xFF888888, true);
                drawContext.drawText(client.textRenderer, modeText, scaledModeX, scaledY3, 0xFF00AAAA, true);

                matrices.popMatrix();
            }
        }
    }

    private String getModeDisplayText(SyncRepairConfig config) {
        switch (config.xpBottleMode) {
            case BOTTLES_32:
                return "32 XP Bottles (448 XP)";
            case CUSTOM:
                int totalXP = config.customXPBottles * 14;
                return config.customXPBottles + " XP Bottles (" + totalXP + " XP)";
            case BOTTLES_64:
            default:
                return "64 XP Bottles (896 XP)";
        }
    }

    private int calculateTotalXPBottlesNeeded(net.minecraft.entity.player.PlayerEntity player, SyncRepairConfig config) {
        if (player == null) return 0;

        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

        int capDurability = calculateCapDurability(helmet, chestplate, leggings, boots);
        int totalBottles = 0;

        totalBottles += calculateXPBottlesNeededForCap(helmet, capDurability, config);
        totalBottles += calculateXPBottlesNeededForCap(chestplate, capDurability, config);
        totalBottles += calculateXPBottlesNeededForCap(leggings, capDurability, config);
        totalBottles += calculateXPBottlesNeededForCap(boots, capDurability, config);

        return totalBottles;
    }

    private void renderArmorDurabilityHUD(DrawContext drawContext, MinecraftClient client, SyncRepairConfig config) {
        var player = client.player;
        if (player == null) return;

        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int startX = config.getHudPositionX() < 0 ? screenWidth + config.getHudPositionX() : config.getHudPositionX();
        int startY = config.getHudPositionY() < 0 ? screenHeight + config.getHudPositionY() : config.getHudPositionY();

        int currentY = startY;
        int capDurability = calculateCapDurability(helmet, chestplate, leggings, boots);
        boolean shouldShowXP = shouldShowXPInfo(config.hudDisplayMode, helmet, chestplate, leggings, boots);

        currentY = renderArmorSlot(drawContext, client, helmet, startX, currentY, capDurability, shouldShowXP, config);
        currentY = renderArmorSlot(drawContext, client, chestplate, startX, currentY, capDurability, shouldShowXP, config);
        currentY = renderArmorSlot(drawContext, client, leggings, startX, currentY, capDurability, shouldShowXP, config);
        currentY = renderArmorSlot(drawContext, client, boots, startX, currentY, capDurability, shouldShowXP, config);
    }

    private boolean shouldShowXPInfo(SyncRepairConfig.HUDDisplayMode mode, ItemStack... armorPieces) {
        if (mode == SyncRepairConfig.HUDDisplayMode.DURABILITY_ONLY) {
            return false;
        }
        return true;
    }

    private int renderArmorSlot(DrawContext drawContext, MinecraftClient client, ItemStack armorPiece, int x, int y, int capDurability, boolean shouldShowXP, SyncRepairConfig config) {
        int lineHeight = 18;
        if (!armorPiece.isEmpty()) {
            if (armorPiece.isDamageable()) {
                renderArmorPieceWithMode(drawContext, client, armorPiece, x, y, capDurability, shouldShowXP, config);
            } else {
                renderNonDamageableItem(drawContext, client, armorPiece, x, y);
            }
        }
        return y + lineHeight;
    }

    private void renderNonDamageableItem(DrawContext drawContext, MinecraftClient client, ItemStack item, int x, int y) {
        drawContext.drawItem(item, x, y - 1);
    }

    private void renderArmorPieceWithMode(DrawContext drawContext, MinecraftClient client, ItemStack armorPiece, int x, int y, int capDurability, boolean shouldShowXP, SyncRepairConfig config) {
        if (armorPiece.isEmpty() || !armorPiece.isDamageable()) return;

        int currentDurability = armorPiece.getMaxDamage() - armorPiece.getDamage();
        int maxDurability = armorPiece.getMaxDamage();
        float durabilityPercentage = (float) currentDurability / maxDurability;
        int durabilityColor = getDurabilityColor(durabilityPercentage);
        String durabilityText = getDurabilityText(currentDurability, maxDurability, durabilityPercentage, config);

        switch (config.hudDisplayMode) {
            case DURABILITY_ONLY:
                renderDurabilityOnly(drawContext, client, armorPiece, durabilityText, durabilityColor, x, y);
                break;
            case DURABILITY_XP_BOTTLES:
                renderDurabilityWithXP(drawContext, client, armorPiece, durabilityText, durabilityColor, x, y, capDurability, shouldShowXP, true, config);
                break;
            case DURABILITY_XP_POINTS:
                renderDurabilityWithXP(drawContext, client, armorPiece, durabilityText, durabilityColor, x, y, capDurability, shouldShowXP, false, config);
                break;
            case XP_BOTTLES_ONLY:
                renderXPOnly(drawContext, client, armorPiece, x, y, capDurability, shouldShowXP, true, config);
                break;
            case XP_POINTS_ONLY:
                renderXPOnly(drawContext, client, armorPiece, x, y, capDurability, shouldShowXP, false, config);
                break;
        }
    }

    private String getDurabilityText(int currentDurability, int maxDurability, float durabilityPercentage, SyncRepairConfig config) {
        switch (config.durabilityDisplayMode) {
            case PERCENTAGE:
                int percentage = Math.round(durabilityPercentage * 100);
                return percentage + "%";
            case ABSOLUTE:
            default:
                return currentDurability + "/" + maxDurability;
        }
    }

    private void renderDurabilityOnly(DrawContext drawContext, MinecraftClient client, ItemStack armorPiece, String durabilityText, int durabilityColor, int x, int y) {
        int textWidth = client.textRenderer.getWidth(durabilityText);
        int textX = x - textWidth - 5;
        int textY = y + 4;
        drawContext.drawText(client.textRenderer, durabilityText, textX, textY, durabilityColor, true);
        drawContext.drawItem(armorPiece, x, y - 1);
    }

    private void renderDurabilityWithXP(DrawContext drawContext, MinecraftClient client, ItemStack armorPiece, String durabilityText, int durabilityColor, int x, int y, int capDurability, boolean shouldShowXP, boolean useBottles, SyncRepairConfig config) {
        int durabilityTextWidth = client.textRenderer.getWidth(durabilityText);
        int durabilityX = x - durabilityTextWidth - 5;
        int textY = y + 4;
        drawContext.drawText(client.textRenderer, durabilityText, durabilityX, textY, durabilityColor, true);
        drawContext.drawItem(armorPiece, x, y - 1);

        if (shouldShowXP && ArmorChecker.hasMendingStatic(armorPiece)) {
            String xpText = "";
            if (useBottles) {
                int xpBottlesNeeded = calculateXPBottlesNeededForCap(armorPiece, capDurability, config);
                xpText = " | " + xpBottlesNeeded + " bottles";
            } else {
                int xpPointsNeeded = calculateXPPointsNeededForCap(armorPiece, capDurability);
                xpText = " | " + xpPointsNeeded + " XP";
            }

            if (!xpText.isEmpty()) {
                int xpX = x + 20;
                drawContext.drawText(client.textRenderer, xpText, xpX, textY, 0xFF00FFFF, true);
            }
        }
    }

    private void renderXPOnly(DrawContext drawContext, MinecraftClient client, ItemStack armorPiece, int x, int y, int capDurability, boolean shouldShowXP, boolean useBottles, SyncRepairConfig config) {
        if (!shouldShowXP || !ArmorChecker.hasMendingStatic(armorPiece)) return;

        String xpText = "";
        if (useBottles) {
            int xpBottlesNeeded = calculateXPBottlesNeededForCap(armorPiece, capDurability, config);
            xpText = xpBottlesNeeded + " bottles";
        } else {
            int xpPointsNeeded = calculateXPPointsNeededForCap(armorPiece, capDurability);
            xpText = xpPointsNeeded + " XP";
        }

        if (!xpText.isEmpty()) {
            int textWidth = client.textRenderer.getWidth(xpText);
            int textX = x - textWidth - 5;
            int textY = y + 4;
            drawContext.drawText(client.textRenderer, xpText, textX, textY, 0xFF00FFFF, true);
            drawContext.drawItem(armorPiece, x, y - 1);
        }
    }

    private boolean allArmorPiecesAbove300(ItemStack... armorPieces) {
        for (ItemStack piece : armorPieces) {
            if (!piece.isEmpty() && piece.isDamageable() && ArmorChecker.hasMendingStatic(piece)) {
                int currentDurability = piece.getMaxDamage() - piece.getDamage();
                if (currentDurability <= 300) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean hasArmorPiecesNeedingMoreThan2Bottles(ItemStack... armorPieces) {
        SyncRepairConfig config = AutoConfig.getConfigHolder(SyncRepairConfig.class).getConfig();
        int capDurability = calculateCapDurability(armorPieces);

        for (ItemStack piece : armorPieces) {
            if (!piece.isEmpty() && piece.isDamageable() && ArmorChecker.hasMendingStatic(piece)) {
                int xpBottlesNeeded = calculateXPBottlesNeededForCap(piece, capDurability, config);
                if (xpBottlesNeeded > 2) {
                    return true;
                }
            }
        }
        return false;
    }

    private int calculateCapDurability(ItemStack... armorPieces) {
        int minMaxDurability = Integer.MAX_VALUE;
        boolean foundMendingPiece = false;

        for (ItemStack piece : armorPieces) {
            if (!piece.isEmpty() && piece.isDamageable() && ArmorChecker.hasMendingStatic(piece)) {
                minMaxDurability = Math.min(minMaxDurability, piece.getMaxDamage());
                foundMendingPiece = true;
            }
        }

        return foundMendingPiece ? minMaxDurability : 0;
    }

    private int calculateXPBottlesNeededForCap(ItemStack armorPiece, int capDurability, SyncRepairConfig config) {
        if (armorPiece.isEmpty() || !armorPiece.isDamageable()) return 0;

        int currentDurability = armorPiece.getMaxDamage() - armorPiece.getDamage();
        if (currentDurability >= capDurability) return 0;

        int durabilityNeeded = capDurability - currentDurability;
        int xpPointsNeeded = (durabilityNeeded + 1) / 2;
        int bottlesNeeded = (xpPointsNeeded + 13) / 14;

        return Math.max(1, bottlesNeeded * 2);
    }

    private int calculateXPPointsNeededForCap(ItemStack armorPiece, int capDurability) {
        if (armorPiece.isEmpty() || !armorPiece.isDamageable()) return 0;

        int currentDurability = armorPiece.getMaxDamage() - armorPiece.getDamage();
        if (currentDurability >= capDurability) return 0;

        int durabilityNeeded = capDurability - currentDurability;
        return (durabilityNeeded + 1) / 2;
    }

    private int getDurabilityColor(float durabilityPercentage) {
        if (durabilityPercentage >= 0.9f) {
            return 0xFF00FF00;
        } else if (durabilityPercentage >= 0.75f) {
            return 0xFF00AA00;
        } else if (durabilityPercentage >= 0.5f) {
            return 0xFFFFFF00;
        } else if (durabilityPercentage >= 0.25f) {
            return 0xFFFF8000;
        } else if (durabilityPercentage > 0.1f) {
            return 0xFFFF0000;
        } else {
            return 0xFFFF00FF;
        }
    }

    private int getProgressiveColor(int damageNeeded, int currentTotalDamage, SyncRepairConfig config) {
        if (currentTotalDamage == 0) {
            return 0xFF888888;
        }

        int xpBase = config.getXPCalculationBase();
        if (damageNeeded >= xpBase) {
            return 0xFFFF00FF;
        } else if (damageNeeded >= (xpBase * 800 / 896)) {
            return 0xFFFF0000;
        } else if (damageNeeded >= (xpBase * 700 / 896)) {
            return 0xFFFF8000;
        } else if (damageNeeded >= (xpBase * 500 / 896)) {
            return 0xFFFFFF00;
        } else if (damageNeeded >= (xpBase * 250 / 896)) {
            return 0xFF00AA00;
        } else {
            return 0xFF00FF00;
        }
    }
}
