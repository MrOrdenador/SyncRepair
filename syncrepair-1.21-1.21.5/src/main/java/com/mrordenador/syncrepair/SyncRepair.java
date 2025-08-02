package com.mrordenador.syncrepair;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

public class SyncRepair implements ModInitializer {
    public static final String MOD_ID = "syncrepair";

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ArmorChecker.checkArmorDamage(player);
            }
        });
    }
}
