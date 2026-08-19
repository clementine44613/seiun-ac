/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.minecraft.class_155
 *  net.minecraft.class_2561
 *  net.minecraft.class_310
 *  net.minecraft.class_8710
 */
package net.kanieoutis.seiunac.client;

import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.client.ClientVerificationState;
import net.kanieoutis.seiunac.client.ModChecker;
import net.kanieoutis.seiunac.network.AntiCheatPackets;
import net.minecraft.class_155;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_8710;

public class NetworkHandler {
    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(AntiCheatPackets.RequestModListPayload.ID, (payload, context) -> {
            SeiunAC.LOGGER.info("\u2190 Server requested mod list");
            context.client().execute(() -> NetworkHandler.sendModListToServer());
        });
        ClientPlayNetworking.registerGlobalReceiver(AntiCheatPackets.ServerResponsePayload.ID, (payload, context) -> {
            SeiunAC.LOGGER.info("\u2190 Server response received: Allowed={}", (Object)payload.allowed());
            context.client().execute(() -> NetworkHandler.handleServerResponse(payload, context.client()));
        });
        ClientPlayNetworking.registerGlobalReceiver(AntiCheatPackets.VerificationSettingsPayload.ID, (payload, context) -> {
            SeiunAC.LOGGER.info("\u2190 Server verification settings received: block-pack-change={}", (Object)payload.blockPackChange());
            context.client().execute(() -> ClientVerificationState.applyServerBlockPackChange(payload.blockPackChange()));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientVerificationState.reset());
        SeiunAC.LOGGER.info("Anti-Cheat Client Network Handler registered");
    }

    public static void sendModListToServer() {
        List<String> modsWithHashes = ModChecker.getInstalledModsWithHashes();
        List<String> resourcePacks = ModChecker.getActiveResourcePacks();
        String minecraftVersion = class_155.method_16673().comp_4025();
        SeiunAC.LOGGER.info("\u2192 Sending mod hashes to server ({} mods, {} packs, MC client version: {}, SeiunAC version: {})", new Object[]{modsWithHashes.size(), resourcePacks.size(), minecraftVersion, "1.0.5"});
        AntiCheatPackets.ClientModListPayload payload = new AntiCheatPackets.ClientModListPayload("1.0.5", minecraftVersion, modsWithHashes, resourcePacks);
        if (ClientPlayNetworking.canSend(AntiCheatPackets.ClientModListPayload.ID)) {
            ClientPlayNetworking.send((class_8710)payload);
            SeiunAC.LOGGER.info("\u2713 Hashes sent");
        } else {
            SeiunAC.LOGGER.error("\u2717 Server does not support Anti-Cheat");
        }
    }

    private static void handleServerResponse(AntiCheatPackets.ServerResponsePayload payload, class_310 client) {
        if (payload.allowed()) {
            SeiunAC.LOGGER.info("\u2713 Anti-Cheat check passed");
            if (client.field_1724 != null) {
                String message = payload.reason();
                if (message == null || message.isBlank()) {
                    message = "\u00a7a\u2713 Anti-Cheat verification successful";
                }
                client.field_1724.method_7353((class_2561)class_2561.method_43470((String)message), false);
            }
        } else {
            SeiunAC.LOGGER.error("\u2717 Connection denied: {}", (Object)payload.reason().replaceAll("\u00a7[0-9a-fk-or]", ""));
            if (client.method_1562() != null) {
                client.method_1562().method_48296().method_10747((class_2561)class_2561.method_43470((String)payload.reason()));
            }
        }
    }

    public static void showModReport() {
        class_310 client = class_310.method_1551();
        if (client.field_1724 != null) {
            String report = ModChecker.generateModReport();
            for (String line : report.split("\n")) {
                client.field_1724.method_7353((class_2561)class_2561.method_43470((String)line), false);
            }
        }
    }
}
