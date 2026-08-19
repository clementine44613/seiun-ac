/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.fabricmc.api.ModInitializer
 *  net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
 *  net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
 *  net.minecraft.class_2168
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package net.kanieoutis.seiunac;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.kanieoutis.seiunac.command.AntiCheatCommand;
import net.kanieoutis.seiunac.network.AntiCheatPackets;
import net.kanieoutis.seiunac.server.ServerNetworkHandler;
import net.minecraft.class_2168;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeiunAC
implements ModInitializer {
    public static final String MOD_ID = "SeiunAC";
    public static final String MOD_VERSION = "1.0.5";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"SeiunAC");

    public void onInitialize() {
        LOGGER.info("=== Seiun AC initializing ===");
        this.registerPayloads();
        ServerNetworkHandler.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> AntiCheatCommand.register((CommandDispatcher<class_2168>)dispatcher));
        LOGGER.info("=== Seiun AC successfully loaded ===");
    }

    private void registerPayloads() {
        PayloadTypeRegistry.playS2C().register(AntiCheatPackets.RequestModListPayload.ID, AntiCheatPackets.RequestModListPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AntiCheatPackets.ClientModListPayload.ID, AntiCheatPackets.ClientModListPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(AntiCheatPackets.ResourcePackChangePayload.ID, AntiCheatPackets.ResourcePackChangePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AntiCheatPackets.VerificationSettingsPayload.ID, AntiCheatPackets.VerificationSettingsPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AntiCheatPackets.ServerResponsePayload.ID, AntiCheatPackets.ServerResponsePayload.CODEC);
        LOGGER.info("Custom Payloads Registered!");
    }
}
