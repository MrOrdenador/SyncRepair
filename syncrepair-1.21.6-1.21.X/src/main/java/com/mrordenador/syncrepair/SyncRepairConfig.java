package com.mrordenador.syncrepair;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "syncrepair")
public class SyncRepairConfig implements ConfigData {
    public enum HUDDisplayMode {
        DURABILITY_ONLY,
        DURABILITY_XP_BOTTLES,
        DURABILITY_XP_POINTS,
        XP_BOTTLES_ONLY,
        XP_POINTS_ONLY
    }

    public enum XPBottleMode {
        BOTTLES_64("64 XP Bottles"),
        BOTTLES_32("32 XP Bottles"),
        CUSTOM("Custom");

        private final String displayName;

        XPBottleMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public enum DurabilityDisplayMode {
        ABSOLUTE("Absolute Values"),
        PERCENTAGE("Percentage");

        private final String displayName;

        DurabilityDisplayMode(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigEntry.Category("general")
    public boolean enableArmorDurabilityHUD = true;

    @ConfigEntry.Category("general")
    public boolean enableDamageStatusTexts = true;

    @ConfigEntry.Category("general")
    public boolean showAdvancedInformation = false;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public HUDDisplayMode hudDisplayMode = HUDDisplayMode.DURABILITY_XP_BOTTLES;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public XPBottleMode xpBottleMode = XPBottleMode.BOTTLES_64;

    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
    public DurabilityDisplayMode durabilityDisplayMode = DurabilityDisplayMode.ABSOLUTE;

    @ConfigEntry.Category("general")
    public int customXPBottles = 64;

    @ConfigEntry.Category("position")
    public String hudPositionX = "-100";

    @ConfigEntry.Category("position")
    public String hudPositionY = "-90";

    @ConfigEntry.Category("position")
    public String topTextsPositionX = "15";

    @ConfigEntry.Category("position")
    public String topTextsPositionY = "10";

    public int getHudPositionX() {
        try {
            int value = Integer.parseInt(hudPositionX);
            return Math.max(-800, Math.min(800, value));
        } catch (NumberFormatException e) {
            return -100;
        }
    }

    public int getHudPositionY() {
        try {
            int value = Integer.parseInt(hudPositionY);
            return Math.max(-600, Math.min(600, value));
        } catch (NumberFormatException e) {
            return -90;
        }
    }

    public int getTopTextsPositionX() {
        try {
            int value = Integer.parseInt(topTextsPositionX);
            return Math.max(-800, Math.min(800, value));
        } catch (NumberFormatException e) {
            return 15;
        }
    }

    public int getTopTextsPositionY() {
        try {
            int value = Integer.parseInt(topTextsPositionY);
            return Math.max(-600, Math.min(600, value));
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    public int getXPCalculationBase() {
        switch (xpBottleMode) {
            case BOTTLES_32:
                return 448;
            case CUSTOM:
                int bottles = Math.max(1, Math.min(64, customXPBottles));
                return bottles * 14;
            case BOTTLES_64:
            default:
                return 896;
        }
    }

    public int getXPPerBottle() {
        return 14;
    }

    public int getTotalBottlesForMode() {
        switch (xpBottleMode) {
            case BOTTLES_32:
                return 32;
            case CUSTOM:
                return Math.max(1, Math.min(64, customXPBottles));
            case BOTTLES_64:
            default:
                return 64;
        }
    }

    public boolean shouldShowCustomInput() {
        return xpBottleMode == XPBottleMode.CUSTOM;
    }
}