/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
 *  net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
 *  net.fabricmc.loader.api.FabricLoader
 *  net.fabricmc.loader.api.ModContainer
 *  net.minecraft.class_12086
 *  net.minecraft.class_155
 *  net.minecraft.class_2561
 *  net.minecraft.class_2596
 *  net.minecraft.class_3222
 *  net.minecraft.class_5904
 *  net.minecraft.class_5905
 *  net.minecraft.class_8710
 *  net.minecraft.server.MinecraftServer
 *  org.geysermc.floodgate.api.FloodgateApi
 *  org.geysermc.geyser.api.GeyserApi
 */
package net.kanieoutis.seiunac.server;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.config.HashWhitelist;
import net.kanieoutis.seiunac.discord.DiscordConfig;
import net.kanieoutis.seiunac.discord.DiscordWebhook;
import net.kanieoutis.seiunac.discord.PlayerStatistics;
import net.kanieoutis.seiunac.discord.StatisticsConfig;
import net.kanieoutis.seiunac.network.AntiCheatPackets;
import net.kanieoutis.seiunac.util.AntiCheatErrorCode;
import net.kanieoutis.seiunac.util.ModHasher;
import net.minecraft.class_12086;
import net.minecraft.class_155;
import net.minecraft.class_2561;
import net.minecraft.class_2596;
import net.minecraft.class_3222;
import net.minecraft.class_5904;
import net.minecraft.class_5905;
import net.minecraft.class_8710;
import net.minecraft.server.MinecraftServer;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.geyser.api.GeyserApi;

public class ServerNetworkHandler {
    private static HashWhitelist hashWhitelist;
    private static DiscordWebhook discordWebhook;
    private static StatisticsConfig statistics;
    private static final Map<UUID, Long> pendingPlayers;
    private static final Map<UUID, Set<String>> approvedPackBypassIds;
    private static final Set<UUID> verifiedPlayers;
    private static volatile boolean isReloading;
    private static volatile MinecraftServer currentServer;

    public static void register() {
        hashWhitelist = new HashWhitelist();
        hashWhitelist.load();
        DiscordConfig discordConfig = DiscordConfig.load();
        discordWebhook = new DiscordWebhook(discordConfig);
        statistics = StatisticsConfig.load();
        statistics.incrementServerStart();
        PlayerStatistics.cleanupOldStatistics();
        if (discordWebhook != null) {
            discordWebhook.sendServerStart();
        }
        ServerPlayNetworking.registerGlobalReceiver(AntiCheatPackets.ClientModListPayload.ID, (payload, context) -> {
            SeiunAC.LOGGER.info("\u2190 Mod and Pack lists received from client of {} ({} mods, {} packs)", new Object[]{context.player().method_5477().getString(), payload.modsWithHashes().size(), payload.resourcePacks().size()});
            context.server().execute(() -> ServerNetworkHandler.handleClientModList(payload, context.player()));
        });
        ServerPlayNetworking.registerGlobalReceiver(AntiCheatPackets.ResourcePackChangePayload.ID, (payload, context) -> {
            SeiunAC.LOGGER.info("\u2190 Resource pack change received from client of {} (added: {}, removed: {})", new Object[]{context.player().method_5477().getString(), payload.addedPacks().size(), payload.removedPacks().size()});
            context.server().execute(() -> ServerNetworkHandler.handleResourcePackChange(payload, context.player()));
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            class_3222 player = handler.method_32311();
            UUID playerId = player.method_5667();
            currentServer = server;
            ServerNetworkHandler.sendVerificationSettings(player);
            if (isReloading) {
                SeiunAC.LOGGER.info("\u2717 Player {} attempted to join while a SeiunAC reload is in progress", (Object)player.method_5477().getString());
                player.field_13987.method_52396((class_2561)class_2561.method_43470((String)AntiCheatErrorCode.AC401.formatKickMessage("The server is currently updating verification lists.", "", "Please try again in a few seconds.")));
                return;
            }
            SeiunAC.LOGGER.info("\u2192 Player {} is connecting - starting verification", (Object)player.method_5477().getString());
            pendingPlayers.put(playerId, System.currentTimeMillis());
            Thread requestThread = new Thread(() -> {
                try {
                    Thread.sleep(1000L);
                    server.execute(() -> {
                        try {
                            if (player.field_13987 == null || !player.field_13987.method_48106()) {
                                SeiunAC.LOGGER.warn("Player {} has disconnected", (Object)player.method_5477().getString());
                                pendingPlayers.remove(playerId);
                                return;
                            }
                            ServerNetworkHandler.requestModListFromClient(player);
                        }
                        catch (Exception e) {
                            SeiunAC.LOGGER.error("Failed to request mod hashes for player {}", (Object)player.method_5477().getString(), (Object)e);
                            pendingPlayers.remove(playerId);
                        }
                    });
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    SeiunAC.LOGGER.debug("Request thread interrupted for player {}", (Object)player.method_5477().getString());
                }
            }, "SeiunAC-Request-" + player.method_5477().getString());
            requestThread.setDaemon(true);
            requestThread.start();
            ServerNetworkHandler.scheduleTimeoutCheck(server, player, playerId);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID playerId = handler.method_32311().method_5667();
            String playerName = handler.method_32311().method_5477().getString();
            if (verifiedPlayers.contains(playerId)) {
                PlayerStatistics stats = PlayerStatistics.load(playerId, playerName);
                stats.endSession();
            }
            pendingPlayers.remove(playerId);
            approvedPackBypassIds.remove(playerId);
            verifiedPlayers.remove(playerId);
        });
        SeiunAC.LOGGER.info("Verification lists loaded: {} whitelisted mods, {} gray mods, {} blacklisted mods", new Object[]{hashWhitelist.getModWhitelistIds().size(), hashWhitelist.getGrayModHashes().size(), hashWhitelist.getModBlacklistIds().size()});
    }

    private static void requestModListFromClient(class_3222 player) {
        block20: {
            try {
                FabricLoader loader;
                if (player == null || player.field_13987 == null) {
                    SeiunAC.LOGGER.error("Player or network handler is null!");
                    return;
                }
                boolean canSend = ServerPlayNetworking.canSend((class_3222)player, AntiCheatPackets.RequestModListPayload.ID);
                SeiunAC.LOGGER.debug("canSend for {} = {}", (Object)player.method_5477().getString(), (Object)canSend);
                if (canSend) {
                    ServerPlayNetworking.send((class_3222)player, (class_8710)new AntiCheatPackets.RequestModListPayload());
                    SeiunAC.LOGGER.info("\u2192 Requested mod list from player {}", (Object)player.method_5477().getString());
                    break block20;
                }
                UUID playerId = player.method_5667();
                String playerName = player.method_5477().getString();
                boolean isGeyserBedrockPlayer = false;
                boolean isFloodgateBedrockPlayer = false;
                try {
                    loader = FabricLoader.getInstance();
                    if (loader.isModLoaded("geyser") || loader.isModLoaded("geyser-fabric")) {
                        try {
                            isGeyserBedrockPlayer = GeyserApi.api().isBedrockPlayer(playerId);
                        }
                        catch (Throwable t) {
                            SeiunAC.LOGGER.warn("Failed to query Geyser API for player {}: {}", (Object)playerName, (Object)t.getMessage());
                            SeiunAC.LOGGER.debug("Geyser API check failure", t);
                        }
                    } else {
                        SeiunAC.LOGGER.debug("Geyser not present - skipping Bedrock player check for {}", (Object)playerName);
                    }
                }
                catch (Throwable t) {
                    SeiunAC.LOGGER.warn("Unexpected error while checking for Geyser mod: {}", (Object)t.getMessage());
                    SeiunAC.LOGGER.debug("Geyser presence check error", t);
                }
                try {
                    loader = FabricLoader.getInstance();
                    if (loader.isModLoaded("floodgate") || loader.isModLoaded("floodgate-fabric")) {
                        try {
                            isFloodgateBedrockPlayer = FloodgateApi.getInstance().isFloodgatePlayer(playerId);
                        }
                        catch (Throwable t) {
                            SeiunAC.LOGGER.warn("Failed to query Floodgate API for player {}: {}", (Object)playerName, (Object)t.getMessage());
                            SeiunAC.LOGGER.debug("Floodgate API check failure", t);
                        }
                    } else {
                        SeiunAC.LOGGER.debug("Floodgate not present - skipping Floodgate Bedrock player check for {}", (Object)playerName);
                    }
                }
                catch (Throwable t) {
                    SeiunAC.LOGGER.warn("Unexpected error while checking for Floodgate mod: {}", (Object)t.getMessage());
                    SeiunAC.LOGGER.debug("Floodgate presence check error", t);
                }
                if (isGeyserBedrockPlayer) {
                    verifiedPlayers.add(playerId);
                    pendingPlayers.remove(playerId);
                    statistics.incrementPlayerJoinSuccess();
                    if (isFloodgateBedrockPlayer) {
                        SeiunAC.LOGGER.info("\u2713 Player {} is a Bedrock player (via Geyser Floodgate) - verification skipped", (Object)playerName);
                        ServerNetworkHandler.sendResponse(player, true, "\u00a7a\u2713 You are a Bedrock player (via Geyser Floodgate) - verification skipped");
                    } else {
                        SeiunAC.LOGGER.info("\u2713 Player {} is a Bedrock player (via Geyser) - verification skipped", (Object)playerName);
                        ServerNetworkHandler.sendResponse(player, true, "\u00a7a\u2713 You are a Bedrock player (via Geyser) - verification skipped");
                    }
                } else {
                    pendingPlayers.remove(playerId);
                    SeiunAC.LOGGER.warn("\u2717 Java player {} cannot receive verification packages, proceeding to kick", (Object)playerName);
                    ServerNetworkHandler.kickPlayer(player, AntiCheatErrorCode.AC002.formatKickMessage("The server could not communicate with your verification client.", "", "Please install the SeiunAC mod (you can find it on prerequisites)."), null);
                }
            }
            catch (Exception e) {
                SeiunAC.LOGGER.error("Failed to send mod list request to {}: {}", (Object)(player != null ? player.method_5477().getString() : "unknown player"), (Object)e.getMessage());
                if (player == null) break block20;
                pendingPlayers.remove(player.method_5667());
            }
        }
    }

    private static void scheduleTimeoutCheck(MinecraftServer server, class_3222 player, UUID playerId) {
        String playerName = player.method_5477().getString();
        Thread timeoutThread = new Thread(() -> {
            try {
                Thread.sleep(31000L);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SeiunAC.LOGGER.debug("Timeout thread for {} was interrupted", (Object)playerName);
                return;
            }
            server.execute(() -> {
                try {
                    if (player.field_13987 == null || !player.field_13987.method_48106()) {
                        SeiunAC.LOGGER.debug("Player {} has disconnected - timeout check skipped", (Object)playerName);
                        pendingPlayers.remove(playerId);
                        verifiedPlayers.remove(playerId);
                        return;
                    }
                    boolean isPending = pendingPlayers.containsKey(playerId);
                    boolean isVerified = verifiedPlayers.contains(playerId);
                    SeiunAC.LOGGER.info("\ud83d\udec8 Timeout check for {}: isPending={}, isVerified={}", new Object[]{playerName, isPending, isVerified});
                    if (isPending && !isVerified) {
                        SeiunAC.LOGGER.warn("\u2717 Timeout: Player {} did not send mod list", (Object)playerName);
                        ServerNetworkHandler.kickPlayer(player, AntiCheatErrorCode.AC101.formatKickMessage("The server did not receive a response within 30 seconds.", "", "Your verification client is not responding.", "Please check your installation or your internet speed connection."), null);
                        pendingPlayers.remove(playerId);
                    } else if (isVerified) {
                        SeiunAC.LOGGER.info("\u2713 Player {} has been verified - timeout check OK", (Object)playerName);
                    } else {
                        SeiunAC.LOGGER.info("Player {} is not pending - already processed", (Object)playerName);
                    }
                }
                catch (Exception e) {
                    SeiunAC.LOGGER.error("Error in timeout check for player {}", (Object)playerName, (Object)e);
                    pendingPlayers.remove(playerId);
                }
            });
        }, "SeiunAC-Timeout-" + playerName);
        timeoutThread.setDaemon(true);
        timeoutThread.start();
    }

    private static void handleClientModList(AntiCheatPackets.ClientModListPayload payload, class_3222 player) {
        boolean hasGrayViolations;
        String clientHash;
        String playerName = player.method_5477().getString();
        UUID playerId = player.method_5667();
        if (player.field_13987 == null || !player.field_13987.method_48106()) {
            SeiunAC.LOGGER.warn("Player {} is already disconnected - verification aborted", (Object)playerName);
            pendingPlayers.remove(playerId);
            return;
        }
        SeiunAC.LOGGER.info("\ud83d\udec8 Checking mod hashes for player: {}", (Object)playerName);
        PlayerStatistics playerStats = PlayerStatistics.load(playerId, playerName);
        playerStats.incrementConnection("tracked");
        String clientVersion = payload.antiCheatVersion();
        String clientMinecraftVersion = payload.minecraftVersion();
        String serverVersion = "1.0.5";
        if (!clientVersion.equals(serverVersion)) {
            SeiunAC.LOGGER.warn("\u2717 Player {} is using an incorrect verification client version! Client: {}, Server: {}", new Object[]{playerName, clientVersion, serverVersion});
            statistics.incrementKick("Incorrect anti-cheat version");
            playerStats.incrementKick("Incorrect anti-cheat version", "Client: " + clientVersion + ", Server: " + serverVersion);
            if (discordWebhook != null) {
                discordWebhook.sendPlayerKick(playerName, "Incorrect anti-cheat version (Client: " + clientVersion + ", Server: " + serverVersion + ")", "AC-001", playerStats);
            }
            pendingPlayers.remove(playerId);
            ServerNetworkHandler.kickPlayer(player, AntiCheatErrorCode.AC001.formatKickMessage("Client Version: " + clientVersion, "Server Version: " + serverVersion, "", "Please install SeiunAC version " + serverVersion), playerStats, null);
            return;
        }
        SeiunAC.LOGGER.info("\u2713 Player {} is using server's SeiunAC version, which is: {}", (Object)playerName, (Object)serverVersion);
        statistics.incrementPlayerChecked();
        boolean isOperator = player.method_75004().equals((Object)class_12086.field_63185);
        boolean isModerator = player.method_75004().equals((Object)class_12086.field_63182);
        if (isOperator || isModerator) {
            verifiedPlayers.add(playerId);
            pendingPlayers.remove(playerId);
            statistics.incrementModeratorBypass();
            statistics.incrementPlayerJoinSuccess();
            playerStats.incrementOpBypass();
            playerStats.incrementSuccessfulJoin();
            if (discordWebhook != null) {
                discordWebhook.sendOpJoin(playerName);
            }
            if (isOperator) {
                SeiunAC.LOGGER.info("\u2713 Player {} has Owner permissions (Operator) - verification automatically skipped", (Object)playerName);
                ServerNetworkHandler.sendResponse(player, true, "\u00a7a\u2713 You are an \u00a7nOperator\u00a7r\u00a7a - verification automatically skipped");
            } else if (isModerator) {
                SeiunAC.LOGGER.info("\u2713 Player {} has Moderator permissions - verification automatically skipped", (Object)playerName);
                ServerNetworkHandler.sendResponse(player, true, "\u00a7a\u2713 You are a \u00a7nModerator\u00a7r\u00a7a - verification automatically skipped");
            } else {
                SeiunAC.LOGGER.info("\u2713 Player {} has the bypass.verification.onjoin permission - verification skipped", (Object)playerName);
                ServerNetworkHandler.sendResponse(player, true, "\u00a7a\u2713 You have the anti-cheat \u00a7nskip verification on join\u00a7r\u00a7a permission - verification skipped");
            }
            return;
        }
        ArrayList<String> modNames = new ArrayList<String>();
        ArrayList<String> blacklistedMods = new ArrayList<String>();
        ArrayList<String> grayMismatchMods = new ArrayList<String>();
        ArrayList<DiscordWebhook.WarningEntry> modWarnings = new ArrayList<DiscordWebhook.WarningEntry>();
        ArrayList<String> blacklistedPacks = new ArrayList<String>();
        ArrayList<String> grayMismatchPacks = new ArrayList<String>();
        List<DiscordWebhook.WarningEntry> packWarnings = new ArrayList<DiscordWebhook.WarningEntry>();
        ConcurrentHashMap.KeySetView packBypassModIds = ConcurrentHashMap.newKeySet();
        int whitelistedMods = 0;
        int grayCheckedMods = 0;
        boolean seiunACFound = false;
        for (String string : payload.modsWithHashes()) {
            String[] parts = string.split(":", 3);
            if (parts.length < 3) continue;
            String modId = parts[0].toLowerCase();
            String version = parts[1];
            clientHash = parts[2];
            String displayName = modId + " v" + version;
            modNames.add(modId);
            if (modId.equals("SeiunAC")) {
                boolean isVersionMismatch;
                seiunACFound = true;
                String serverSeiunACHash = ServerNetworkHandler.getserverSeiunACHash();
                String serverMinecraftVersion = class_155.method_16673().comp_4025();
                boolean bl = isVersionMismatch = !serverMinecraftVersion.equals(clientMinecraftVersion);
                if (serverSeiunACHash != null && !serverSeiunACHash.equalsIgnoreCase(clientHash)) {
                    if (isVersionMismatch) {
                        String knownHashForClientVersion = hashWhitelist.getSeiunACHashForVersion(clientMinecraftVersion);
                        if (knownHashForClientVersion != null && knownHashForClientVersion.equalsIgnoreCase(clientHash)) {
                            SeiunAC.LOGGER.info("\u2713 Player {} is using SeiunAC for Minecraft version {} with matching known hash", (Object)playerName, (Object)clientMinecraftVersion);
                            packBypassModIds.add(modId);
                            continue;
                        }
                        modWarnings.add(new DiscordWebhook.WarningEntry(displayName, clientHash, serverSeiunACHash));
                        SeiunAC.LOGGER.error("\u26a0 CRITICAL: SeiunAC client code of {} has been altered or it is from an unknown version!", (Object)playerName);
                        SeiunAC.LOGGER.error("  Client Minecraft Version: {}, Client Hash: {}", (Object)clientMinecraftVersion, (Object)ServerNetworkHandler.logHash(clientHash));
                        SeiunAC.LOGGER.error("  Server Minecraft Version: {}, Server Hash: {}", (Object)serverMinecraftVersion, (Object)ServerNetworkHandler.logHash(serverSeiunACHash));
                        statistics.incrementAntiCheatTampered();
                        playerStats.incrementAntiCheatTampered();
                        statistics.incrementKick("Anti-cheat tampered");
                        playerStats.incrementKick("Anti-cheat tampered", "SeiunAC mod hash mismatch (Client Minecraft version: " + clientMinecraftVersion + ", Server Minecraft version: " + serverMinecraftVersion + ")");
                        if (discordWebhook != null) {
                            discordWebhook.sendPlayerKick(playerName, "Anti-cheat hash mismatch (Client Minecraft version: " + clientMinecraftVersion + " Hash: " + ServerNetworkHandler.logHash(clientHash) + ", Server Minecraft version: " + serverMinecraftVersion + " Hash: " + ServerNetworkHandler.logHash(serverSeiunACHash) + ")", "AC-005", playerStats);
                        }
                        pendingPlayers.remove(playerId);
                        ServerNetworkHandler.kickPlayer(player, AntiCheatErrorCode.AC005.formatKickMessage("\u00a7cSeiun AC is present but it is either altered or incompatible with the server!", "\u00a77Client Minecraft version: " + clientMinecraftVersion, "\u00a77Server Minecraft version: " + serverMinecraftVersion, "\u00a77Are you using an official distribution of SeiunAC? Install it from either Modrinth or Curseforge"), playerStats, null);
                        return;
                    }
                    modWarnings.add(new DiscordWebhook.WarningEntry(displayName, clientHash, serverSeiunACHash));
                    SeiunAC.LOGGER.error("\u26a0 CRITICAL: SeiunAC client code of {}, from Minecraft version {}, has been altered!", (Object)playerName, (Object)clientMinecraftVersion);
                    SeiunAC.LOGGER.error("  Client Hash: {}", (Object)ServerNetworkHandler.logHash(clientHash));
                    SeiunAC.LOGGER.error("  Server Hash: {}", (Object)ServerNetworkHandler.logHash(serverSeiunACHash));
                    statistics.incrementAntiCheatTampered();
                    playerStats.incrementAntiCheatTampered();
                    statistics.incrementKick("Anti-cheat tampered");
                    playerStats.incrementKick("Anti-cheat tampered", "SeiunAC mod was loaded with version match but hash mismatch");
                    if (discordWebhook != null) {
                        discordWebhook.sendPlayerKick(playerName, "Anti-cheat hash mismatch while using the same Minecraft version (Client: " + ServerNetworkHandler.logHash(clientHash) + ", Server: " + ServerNetworkHandler.logHash(serverSeiunACHash) + ")", "AC-005", playerStats);
                    }
                    pendingPlayers.remove(playerId);
                    ServerNetworkHandler.kickPlayer(player, AntiCheatErrorCode.AC005.formatKickMessage("\u00a7cSeiun AC server version matches with your client, but your client mod has been altered!", "\u00a77Are you using an official distribution of SeiunAC?", "\u00a77Download and use only official releases of Seiun AC (Modrinth or Curseforge)"), playerStats, null);
                    return;
                }
                SeiunAC.LOGGER.info("\u2713 Player {} matches the server's SeiunAC hash (Minecraft client version: {})", (Object)playerName, (Object)clientMinecraftVersion);
                packBypassModIds.add(modId);
                continue;
            }
            if (hashWhitelist.isModBlacklisted(modId)) {
                blacklistedMods.add(displayName);
                SeiunAC.LOGGER.warn("\u2717 Blacklisted mod detected: {} (hash: {})", (Object)modId, (Object)ServerNetworkHandler.logHash(clientHash));
                continue;
            }
            if (hashWhitelist.isModWhitelisted(modId)) {
                ++whitelistedMods;
                packBypassModIds.add(modId);
                continue;
            }
            if (hashWhitelist.isModGraylisted(modId)) {
                ++grayCheckedMods;
                String serverHash = hashWhitelist.getGrayModHash(modId);
                if (serverHash == null || !serverHash.equalsIgnoreCase(clientHash)) {
                    grayMismatchMods.add(displayName);
                    SeiunAC.LOGGER.warn("\u2717 Hash mismatch for gray mod {}: Client={} Server={}", new Object[]{modId, ServerNetworkHandler.logHash(clientHash), ServerNetworkHandler.logHash(serverHash)});
                    continue;
                }
                SeiunAC.LOGGER.debug("  \u2713 Gray mod {} hash matches", (Object)modId);
                packBypassModIds.add(modId);
                continue;
            }
            modWarnings.add(new DiscordWebhook.WarningEntry(displayName, clientHash, null));
            SeiunAC.LOGGER.warn("\u2717 Unwhitelisted mod detected: {} (hash: {}, not present in mod_whitelist.json or graymods/)", (Object)modId, (Object)ServerNetworkHandler.logHash(clientHash));
            packBypassModIds.add(modId);
        }
        if (!seiunACFound) {
            SeiunAC.LOGGER.error("\u2717 CRITICAL: SeiunAC mod was not found on the client!");
            statistics.incrementKick("Anti-Cheat Mod Not Found");
            playerStats.incrementKick("Anti-Cheat Mod Not Found", "Seiun AC mod was not loaded");
            pendingPlayers.remove(playerId);
            ServerNetworkHandler.kickPlayer(player, "\u00a7c\u00a7lSeiun AC\n\n\u00a77You have been kicked:\n\u00a7cSeiun AC Not Found\n\n\u00a77The Seiun AC mod was not loaded\n\u00a77on your client!", playerStats, null);
            return;
        }
        int totalMods = payload.modsWithHashes().size();
        SeiunAC.LOGGER.info("\ud83d\udec8 Mod verification: {} total, {} whitelisted/system mods skipped, {} gray mods checked, {} warnings", new Object[]{totalMods, whitelistedMods, grayCheckedMods, modWarnings.size()});
        for (String packEntry : payload.resourcePacks()) {
            String[] parts = packEntry.split(":", 2);
            if (parts.length < 2) {
                SeiunAC.LOGGER.warn("  \u26a0 Resource pack without hash: {}", (Object)packEntry);
                continue;
            }
            Iterator packName = parts[0];
            clientHash = parts[1];
            String normalizedPackName = HashWhitelist.normalizePackForStorage((String)((Object)packName));
            if (ServerNetworkHandler.isSystemResourcePack((String)((Object)packName)) || hashWhitelist.isModWhitelisted(normalizedPackName) || packBypassModIds.contains(normalizedPackName) || ServerNetworkHandler.isApprovedModProvidedPack(normalizedPackName, packBypassModIds)) continue;
            if (hashWhitelist.isPackBlacklisted(normalizedPackName)) {
                blacklistedPacks.add(normalizedPackName);
                SeiunAC.LOGGER.warn("\u2717 Prohibited resource pack detected: {} (hash: {})", (Object)normalizedPackName, (Object)ServerNetworkHandler.logHash(clientHash));
                continue;
            }
            if (hashWhitelist.isPackWhitelisted(normalizedPackName)) continue;
            String grayHash = hashWhitelist.getGrayPackHash(normalizedPackName);
            if (grayHash != null) {
                if (!grayHash.equalsIgnoreCase(clientHash)) {
                    grayMismatchPacks.add(normalizedPackName);
                    SeiunAC.LOGGER.warn("\u2717 Gray resource pack {} hash mismatch: Client={} Server={}", new Object[]{normalizedPackName, ServerNetworkHandler.logHash(clientHash), ServerNetworkHandler.logHash(grayHash)});
                    continue;
                }
                SeiunAC.LOGGER.debug("  \u2713 Gray resource pack {} hash matches", (Object)normalizedPackName);
                continue;
            }
            packWarnings.add(new DiscordWebhook.WarningEntry((String)((Object)packName), clientHash, null));
        }
        packWarnings = ServerNetworkHandler.filterPackWarnings(packWarnings, packBypassModIds);
        for (DiscordWebhook.WarningEntry warning : packWarnings) {
            String normalized = HashWhitelist.normalizePackForStorage(warning.name());
            SeiunAC.LOGGER.warn("\u2717 Unwhitelisted resource pack detected: {} (hash: {}, not present in pack_whitelist.json or graypacks/)", (Object)normalized, (Object)ServerNetworkHandler.logHash(warning.clientHash()));
        }
        boolean bl = !blacklistedMods.isEmpty() || !blacklistedPacks.isEmpty();
        boolean bl2 = hasGrayViolations = !grayMismatchMods.isEmpty() || !grayMismatchPacks.isEmpty();
        if (bl || hasGrayViolations) {
            ArrayList<Object> detailsList = new ArrayList<Object>();
            if (!blacklistedMods.isEmpty()) {
                detailsList.add("Blacklisted mods:");
                for (String mod : blacklistedMods) {
                    detailsList.add("  \u2022 " + mod);
                }
                detailsList.add("");
            }
            if (!blacklistedPacks.isEmpty()) {
                detailsList.add("Prohibited resource packs:");
                for (String pack : blacklistedPacks) {
                    detailsList.add("  \u2022 " + pack);
                }
                detailsList.add("");
            }
            if (!grayMismatchMods.isEmpty()) {
                detailsList.add("Graylisted mods with invalid hash:");
                for (String mod : grayMismatchMods) {
                    detailsList.add("  \u2022 " + mod);
                }
                detailsList.add("");
            }
            if (!grayMismatchPacks.isEmpty()) {
                detailsList.add("Graylisted resource packs with invalid hash:");
                for (String pack : grayMismatchPacks) {
                    detailsList.add("  \u2022 " + pack);
                }
                detailsList.add("");
            }
            detailsList.add("Use only the mods and packs authorized by the server!");
            AntiCheatErrorCode errorCode = bl && !hasGrayViolations ? (!blacklistedMods.isEmpty() && blacklistedPacks.isEmpty() ? (blacklistedMods.size() == 1 ? AntiCheatErrorCode.AC201 : AntiCheatErrorCode.AC205) : (blacklistedMods.isEmpty() && !blacklistedPacks.isEmpty() ? (blacklistedPacks.size() == 1 ? AntiCheatErrorCode.AC301 : AntiCheatErrorCode.AC306) : AntiCheatErrorCode.AC604)) : (!bl ? (!grayMismatchMods.isEmpty() && grayMismatchPacks.isEmpty() ? (grayMismatchMods.size() == 1 ? AntiCheatErrorCode.AC203 : AntiCheatErrorCode.AC202) : (grayMismatchMods.isEmpty() && !grayMismatchPacks.isEmpty() ? (grayMismatchPacks.size() == 1 ? AntiCheatErrorCode.AC303 : AntiCheatErrorCode.AC302) : AntiCheatErrorCode.AC604)) : AntiCheatErrorCode.AC604);
            String kickReason = errorCode.getDescription();
            if (!blacklistedMods.isEmpty()) {
                statistics.incrementIllegalMods(blacklistedMods.size());
                playerStats.incrementIllegalMods(blacklistedMods.size());
            }
            if (!blacklistedPacks.isEmpty()) {
                statistics.incrementIllegalResourcePacks(blacklistedPacks.size());
                playerStats.incrementIllegalResourcePacks(blacklistedPacks.size());
            }
            if (!grayMismatchMods.isEmpty()) {
                statistics.incrementModifiedMods(grayMismatchMods.size());
                playerStats.incrementModifiedMods(grayMismatchMods.size());
            }
            String kickMessage = errorCode.formatKickMessage(detailsList.toArray(new String[0]));
            String violationDetails = ServerNetworkHandler.buildViolationDetails(blacklistedMods, blacklistedPacks, grayMismatchMods, grayMismatchPacks);
            statistics.incrementKick(kickReason);
            playerStats.incrementKick(kickReason, violationDetails);
            pendingPlayers.remove(playerId);
            approvedPackBypassIds.remove(playerId);
            ServerNetworkHandler.sendResponse(player, false, kickMessage);
            ServerNetworkHandler.kickPlayer(player, kickMessage, playerStats, violationDetails);
            return;
        }
        if (!modWarnings.isEmpty()) {
            int unapprovedCount = 0;
            int modifiedCount = 0;
            for (DiscordWebhook.WarningEntry warning : modWarnings) {
                if (warning.expectedHash() == null || warning.expectedHash().isBlank()) {
                    ++unapprovedCount;
                    continue;
                }
                ++modifiedCount;
            }
            if (unapprovedCount > 0) {
                statistics.incrementIllegalMods(unapprovedCount);
                playerStats.incrementIllegalMods(unapprovedCount);
            }
            if (modifiedCount > 0) {
                statistics.incrementModifiedMods(modifiedCount);
                playerStats.incrementModifiedMods(modifiedCount);
            }
        }
        if (!packWarnings.isEmpty()) {
            statistics.incrementIllegalResourcePacks(packWarnings.size());
            playerStats.incrementIllegalResourcePacks(packWarnings.size());
        }
        verifiedPlayers.add(playerId);
        pendingPlayers.remove(playerId);
        approvedPackBypassIds.put(playerId, Set.copyOf(packBypassModIds));
        SeiunAC.LOGGER.info("\u2713 Player {} removed from pending - Verified", (Object)playerName);
        statistics.incrementPlayerJoinSuccess();
        playerStats.incrementSuccessfulJoin(modNames);
        if (!modWarnings.isEmpty() || !packWarnings.isEmpty()) {
            ServerNetworkHandler.sendWarningMessages(player, modWarnings, packWarnings);
            if (discordWebhook != null) {
                if (!modWarnings.isEmpty()) {
                    discordWebhook.sendModWarning(playerName, modWarnings, playerStats);
                }
                if (!packWarnings.isEmpty()) {
                    discordWebhook.sendPackWarning(playerName, packWarnings, playerStats, DiscordWebhook.PackWarningContext.JOINING);
                }
            }
        } else {
            SeiunAC.LOGGER.info("\u2713 Player {} is using only authorized mods with valid hashes", (Object)playerName);
        }
        if (discordWebhook != null) {
            discordWebhook.sendPlayerJoin(playerName, playerStats);
        }
        ServerNetworkHandler.sendResponse(player, true, "\u00a7a\u2713 Anti-cheat verification successful");
    }

    private static void handleResourcePackChange(AntiCheatPackets.ResourcePackChangePayload payload, class_3222 player) {
        boolean hasViolation;
        String grayHash;
        String packName;
        Iterator rawName;
        if (player == null || player.field_13987 == null || !player.field_13987.method_48106()) {
            return;
        }
        String playerName = player.method_5477().getString();
        PlayerStatistics playerStats = PlayerStatistics.load(player.method_5667(), playerName);
        List<DiscordWebhook.PackChangeEntry> addedEntries = ServerNetworkHandler.parsePackChangeEntries(payload.addedPacks());
        List<DiscordWebhook.PackChangeEntry> removedEntries = ServerNetworkHandler.parsePackChangeEntries(payload.removedPacks());
        Set<String> approvedPackIds = approvedPackBypassIds.getOrDefault(player.method_5667(), Set.of());
        if (player.method_75004().equals((Object)class_12086.field_63185)) {
            return;
        }
        if (!(!hashWhitelist.isBlockPackChangeEnabled() || addedEntries.isEmpty() && removedEntries.isEmpty())) {
            if (discordWebhook != null) {
                discordWebhook.sendPackChangeLog(playerName, payload.timestamp(), addedEntries, removedEntries);
            }
            SeiunAC.LOGGER.warn("Resource pack changes are disabled on this server; ignoring pack change from {}", (Object)playerName);
            return;
        }
        List<DiscordWebhook.WarningEntry> packWarnings = new ArrayList<DiscordWebhook.WarningEntry>();
        ArrayList<String> blacklistedPacks = new ArrayList<String>();
        ArrayList<String> grayMismatchPacks = new ArrayList<String>();
        for (DiscordWebhook.PackChangeEntry entry : addedEntries) {
            rawName = entry.name();
            packName = HashWhitelist.normalizePackForStorage((String)((Object)rawName));
            if (ServerNetworkHandler.isSystemResourcePack((String)((Object)rawName)) || hashWhitelist.isModWhitelisted(packName) || approvedPackIds.contains(packName) || ServerNetworkHandler.isApprovedModProvidedPack(packName, approvedPackIds)) continue;
            if (hashWhitelist.isPackBlacklisted(packName)) {
                blacklistedPacks.add(packName);
                SeiunAC.LOGGER.warn("\u2717 Prohibited resource pack was activated by the {}'s client: {} (hash: {})", new Object[]{playerName, packName, ServerNetworkHandler.logHash(entry.hash())});
                continue;
            }
            if (hashWhitelist.isPackWhitelisted(packName)) continue;
            grayHash = hashWhitelist.getGrayPackHash(packName);
            if (grayHash == null) {
                packWarnings.add(new DiscordWebhook.WarningEntry((String)((Object)rawName), entry.hash(), null));
                SeiunAC.LOGGER.warn("\u2717 Unwhitelisted resource pack was activated by the {}'s client: {} (hash: {}, not present in pack_whitelist.json or graypacks/)", new Object[]{playerName, packName, ServerNetworkHandler.logHash(entry.hash())});
                continue;
            }
            if (grayHash.equalsIgnoreCase(entry.hash())) continue;
            grayMismatchPacks.add(packName);
        }
        for (DiscordWebhook.PackChangeEntry entry : removedEntries) {
            rawName = entry.name();
            packName = HashWhitelist.normalizePackForStorage((String)((Object)rawName));
            if (ServerNetworkHandler.isSystemResourcePack((String)((Object)rawName)) || hashWhitelist.isModWhitelisted(packName) || approvedPackIds.contains(packName) || ServerNetworkHandler.isApprovedModProvidedPack(packName, approvedPackIds)) continue;
            if (hashWhitelist.isPackBlacklisted(packName)) {
                blacklistedPacks.add(packName);
                SeiunAC.LOGGER.warn("\u2717 Prohibited resource pack was deactivated by the {}'s client: {} (hash: {})", new Object[]{playerName, packName, ServerNetworkHandler.logHash(entry.hash())});
                continue;
            }
            if (hashWhitelist.isPackWhitelisted(packName)) continue;
            grayHash = hashWhitelist.getGrayPackHash(packName);
            if (grayHash == null) {
                packWarnings.add(new DiscordWebhook.WarningEntry((String)((Object)rawName), entry.hash(), null));
                SeiunAC.LOGGER.warn("\u2717 Unwhitelisted resource pack was deactivated by the {}'s client: {} (hash: {}, not present in pack_whitelist.json or graypacks/)", new Object[]{playerName, packName, ServerNetworkHandler.logHash(entry.hash())});
                continue;
            }
            if (grayHash.equalsIgnoreCase(entry.hash())) continue;
            grayMismatchPacks.add(packName);
        }
        if (!(addedEntries.isEmpty() && removedEntries.isEmpty() || discordWebhook == null)) {
            discordWebhook.sendPackChangeLog(playerName, payload.timestamp(), addedEntries, removedEntries);
        }
        packWarnings = ServerNetworkHandler.filterPackWarnings(packWarnings, approvedPackIds);
        boolean bl = hasViolation = !blacklistedPacks.isEmpty() || !grayMismatchPacks.isEmpty();
        if (hasViolation) {
            ArrayList<Object> detailsList = new ArrayList<Object>();
            if (!blacklistedPacks.isEmpty()) {
                detailsList.add("Blacklisted resource packs:");
                for (String pack : blacklistedPacks) {
                    detailsList.add("  \u2022 " + pack);
                }
                detailsList.add("");
            }
            if (!grayMismatchPacks.isEmpty()) {
                detailsList.add("Graylisted resource packs with invalid hash:");
                for (String pack : grayMismatchPacks) {
                    detailsList.add("  \u2022 " + pack);
                }
                detailsList.add("");
            }
            detailsList.add("Use only the packs authorized by the server!");
            AntiCheatErrorCode errorCode = !blacklistedPacks.isEmpty() && grayMismatchPacks.isEmpty() ? (blacklistedPacks.size() == 1 ? AntiCheatErrorCode.AC301 : AntiCheatErrorCode.AC306) : (blacklistedPacks.isEmpty() && !grayMismatchPacks.isEmpty() ? (grayMismatchPacks.size() == 1 ? AntiCheatErrorCode.AC303 : AntiCheatErrorCode.AC302) : AntiCheatErrorCode.AC604);
            if (!blacklistedPacks.isEmpty()) {
                statistics.incrementIllegalResourcePacks(blacklistedPacks.size());
                playerStats.incrementIllegalResourcePacks(blacklistedPacks.size());
            }
            String kickMessage = errorCode.formatKickMessage(detailsList.toArray(new String[0]));
            statistics.incrementKick(errorCode.getDescription());
            playerStats.incrementKick(errorCode.getDescription(), ServerNetworkHandler.buildViolationDetails(Collections.emptyList(), blacklistedPacks, Collections.emptyList(), grayMismatchPacks));
            ServerNetworkHandler.kickPlayer(player, kickMessage, playerStats, ServerNetworkHandler.buildViolationDetails(Collections.emptyList(), blacklistedPacks, Collections.emptyList(), grayMismatchPacks));
            return;
        }
        if (packWarnings.isEmpty()) {
            SeiunAC.LOGGER.info("\u2713 Resource pack change from {} has no verification warnings", (Object)playerName);
            return;
        }
        playerStats.incrementIllegalResourcePacks(packWarnings.size());
        statistics.incrementIllegalResourcePacks(packWarnings.size());
        if (discordWebhook != null) {
            discordWebhook.sendPackWarning(playerName, packWarnings, playerStats, DiscordWebhook.PackWarningContext.CHANGING);
        }
    }

    private static void sendWarningMessages(class_3222 player, List<DiscordWebhook.WarningEntry> modWarnings, List<DiscordWebhook.WarningEntry> packWarnings) {
        if (player == null) {
            return;
        }
        if (!modWarnings.isEmpty()) {
            player.method_43502((class_2561)class_2561.method_43470((String)("\u00a7eYou are using the following mods which haven't been approved by an admin (if you see this message, ping a mod in discord): \u00a77" + ServerNetworkHandler.joinWarningNames(modWarnings))), false);
        }
        if (!packWarnings.isEmpty()) {
            player.method_43502((class_2561)class_2561.method_43470((String)("\u00a7eYou are using the following resource packs which haven't been approved by an admin (if you see this message, ping a mod in discord): \u00a77" + ServerNetworkHandler.joinWarningNames(packWarnings))), false);
        }
        if (!modWarnings.isEmpty() || !packWarnings.isEmpty()) {
            ServerNetworkHandler.sendWarningTitle(player);
        }
    }

    private static void sendWarningTitle(class_3222 player) {
        if (player == null || player.field_13987 == null || !player.field_13987.method_48106()) {
            return;
        }
        player.field_13987.method_14364((class_2596)new class_5905(5, 30, 5));
        player.field_13987.method_14364((class_2596)new class_5904((class_2561)class_2561.method_43470((String)"\u26a0")));
    }

    private static List<DiscordWebhook.PackChangeEntry> parsePackChangeEntries(List<String> entries) {
        ArrayList<DiscordWebhook.PackChangeEntry> parsedEntries = new ArrayList<DiscordWebhook.PackChangeEntry>();
        if (entries == null) {
            return parsedEntries;
        }
        for (String entry : entries) {
            if (entry == null || entry.isBlank()) continue;
            String[] parts = entry.split(":", 2);
            String name = parts.length > 0 ? parts[0].trim() : entry.trim();
            String hash = parts.length > 1 ? parts[1] : "";
            parsedEntries.add(new DiscordWebhook.PackChangeEntry(name, hash));
        }
        return parsedEntries;
    }

    private static String joinWarningNames(List<DiscordWebhook.WarningEntry> warnings) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(8, warnings.size());
        for (int i = 0; i < limit; ++i) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(warnings.get(i).name());
        }
        if (warnings.size() > limit) {
            builder.append(" ...");
        }
        return builder.toString();
    }

    private static List<DiscordWebhook.WarningEntry> filterPackWarnings(List<DiscordWebhook.WarningEntry> warnings, Set<String> approvedModIds) {
        if (warnings == null || warnings.isEmpty()) {
            return warnings;
        }
        LinkedHashMap<String, DiscordWebhook.WarningEntry> filtered = new LinkedHashMap<String, DiscordWebhook.WarningEntry>();
        for (DiscordWebhook.WarningEntry warning : warnings) {
            String packName = HashWhitelist.normalizePackForStorage(warning.name());
            if (ServerNetworkHandler.isSystemResourcePack(warning.name()) || ServerNetworkHandler.isPackCoveredByApprovedMod(packName, approvedModIds)) continue;
            filtered.putIfAbsent(packName, warning);
        }
        return new ArrayList<DiscordWebhook.WarningEntry>(filtered.values());
    }

    private static String buildViolationDetails(List<String> blacklistedMods, List<String> blacklistedPacks) {
        StringBuilder details = new StringBuilder();
        if (!blacklistedMods.isEmpty()) {
            details.append("Blacklisted Mods: ").append(String.join((CharSequence)", ", blacklistedMods));
        }
        if (!blacklistedPacks.isEmpty()) {
            if (details.length() > 0) {
                details.append("; ");
            }
            details.append("Blacklisted Packs: ").append(String.join((CharSequence)", ", blacklistedPacks));
        }
        return details.toString();
    }

    private static String buildViolationDetails(List<String> blacklistedMods, List<String> blacklistedPacks, List<String> grayMismatchMods, List<String> grayMismatchPacks) {
        StringBuilder details = new StringBuilder(ServerNetworkHandler.buildViolationDetails(blacklistedMods, blacklistedPacks));
        if (!grayMismatchMods.isEmpty()) {
            if (details.length() > 0) {
                details.append("; ");
            }
            details.append("Graylisted Mods: ").append(String.join((CharSequence)", ", grayMismatchMods));
        }
        if (!grayMismatchPacks.isEmpty()) {
            if (details.length() > 0) {
                details.append("; ");
            }
            details.append("Graylisted Packs: ").append(String.join((CharSequence)", ", grayMismatchPacks));
        }
        return details.toString();
    }

    private static String previewHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "unknown";
        }
        return hash.length() <= 16 ? hash : hash.substring(0, 16);
    }

    private static String logHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "unknown";
        }
        return hash.trim();
    }

    private static void kickPlayer(class_3222 player, String reason, PlayerStatistics playerStats) {
        ServerNetworkHandler.kickPlayer(player, reason, playerStats, null);
    }

    private static void kickPlayer(class_3222 player, String reason, PlayerStatistics playerStats, String violationDetails) {
        if (player.field_13987 != null) {
            int codeStart;
            String errorCode = null;
            if (reason.contains("Error Code: ") && (codeStart = reason.indexOf("Error Code: ") + 12) + 6 <= reason.length()) {
                errorCode = reason.substring(codeStart, Math.min(codeStart + 6, reason.length()));
            }
            if (discordWebhook != null) {
                discordWebhook.sendPlayerKick(player.method_5477().getString(), reason, errorCode, violationDetails, playerStats);
            }
            player.field_13987.method_52396((class_2561)class_2561.method_43470((String)reason));
            SeiunAC.LOGGER.info("Player {} was kicked by the verification system", (Object)player.method_5477().getString());
        }
    }

    private static void sendResponse(class_3222 player, boolean allowed, String reason) {
        AntiCheatPackets.ServerResponsePayload response = new AntiCheatPackets.ServerResponsePayload(allowed, reason);
        if (ServerPlayNetworking.canSend((class_3222)player, AntiCheatPackets.ServerResponsePayload.ID)) {
            ServerPlayNetworking.send((class_3222)player, (class_8710)response);
        }
    }

    public static HashWhitelist getHashWhitelist() {
        return hashWhitelist;
    }

    public static StatisticsConfig getStatistics() {
        return statistics;
    }

    public static void notifyOperatorChange(String actorName, List<String> targetNames, boolean granted) {
        if (discordWebhook != null) {
            discordWebhook.sendOperatorChange(actorName, targetNames, granted);
        }
    }

    public static void notifyCommandDispatch(String actorName, String commandName, String rawCommand) {
        if (discordWebhook != null && discordWebhook.shouldDispatchCommand(commandName)) {
            discordWebhook.sendDispatchCommand(actorName, commandName, rawCommand);
        }
    }

    private static boolean isSystemMod(String modId) {
        return hashWhitelist != null && hashWhitelist.isModWhitelisted(modId);
    }

    private static String getserverSeiunACHash() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            Optional modContainer = loader.getModContainer("SeiunAC");
            if (modContainer.isPresent()) {
                ModContainer mod = (ModContainer)modContainer.get();
                for (Path path : mod.getOrigin().getPaths()) {
                    try {
                        String hash;
                        if (Files.isDirectory(path, new LinkOption[0]) || !Files.isRegularFile(path, new LinkOption[0]) || "ERROR".equals(hash = ModHasher.generateModHash(path)) || "DEV_MODE".equals(hash)) continue;
                        return hash;
                    }
                    catch (Exception exception) {
                    }
                }
            }
            SeiunAC.LOGGER.debug("SeiunAC is running in development mode - hash comparison skipped");
            return null;
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("Failed to hash the server SeiunAC mod", (Throwable)e);
            return null;
        }
    }

    private static boolean isSystemResourcePack(String pack) {
        if (pack == null) {
            return false;
        }
        String raw = pack.toLowerCase().replace('\\', '/');
        if (raw.startsWith("server/") || raw.startsWith("fabric")) {
            return true;
        }
        String normalized = HashWhitelist.normalizePackForStorage(pack);
        return normalized.equals(HashWhitelist.normalizePackForStorage("vanilla")) || normalized.equals(HashWhitelist.normalizePackForStorage("fabricmods")) || normalized.equals(HashWhitelist.normalizePackForStorage("programerart")) || normalized.equals(HashWhitelist.normalizePackForStorage("programmerart"));
    }

    private static boolean isApprovedModProvidedPack(String packName, Set<String> approvedModIds) {
        if (packName == null || approvedModIds == null || approvedModIds.isEmpty()) {
            return false;
        }
        String normalizedPack = HashWhitelist.normalizePackForStorage(packName);
        for (String modId : approvedModIds) {
            String normalizedMod = HashWhitelist.normalizePackForStorage(modId);
            if (normalizedMod.isEmpty() || !normalizedPack.equals(normalizedMod) && !normalizedPack.startsWith(normalizedMod)) continue;
            return true;
        }
        return false;
    }

    private static boolean isPackCoveredByApprovedMod(String packName, Set<String> approvedModIds) {
        if (packName == null || approvedModIds == null || approvedModIds.isEmpty()) {
            return false;
        }
        String normalizedPack = HashWhitelist.normalizePackForStorage(packName);
        for (String modId : approvedModIds) {
            String normalizedMod = HashWhitelist.normalizePackForStorage(modId);
            if (normalizedMod.isEmpty() || !normalizedPack.equals(normalizedMod) && !normalizedPack.startsWith(normalizedMod)) continue;
            return true;
        }
        return false;
    }

    public static boolean isReloading() {
        return isReloading;
    }

    public static void reloadHashWhitelist() {
        isReloading = true;
        SeiunAC.LOGGER.info("=== SeiunAC verification reload started ===");
        SeiunAC.LOGGER.info("New joins are blocked during reload");
        HashWhitelist backup = hashWhitelist;
        try {
            hashWhitelist.load();
            SeiunAC.LOGGER.info("\u2713 Verification lists reloaded: {} whitelisted mods, {} gray mods, {} blacklisted mods, {} whitelisted packs, {} gray packs, {} blacklisted packs", new Object[]{hashWhitelist.getModWhitelistIds().size(), hashWhitelist.getGrayModHashes().size(), hashWhitelist.getModBlacklistIds().size(), hashWhitelist.getPackWhitelistNames().size(), hashWhitelist.getGrayPackHashes().size(), hashWhitelist.getPackBlacklistNames().size()});
            SeiunAC.LOGGER.info("Verified players remain connected: {}", (Object)verifiedPlayers.size());
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("\u2717 Failed to reload verification lists", (Throwable)e);
            hashWhitelist = backup;
            SeiunAC.LOGGER.warn("Rolled back to previous verification lists");
        }
        finally {
            isReloading = false;
            SeiunAC.LOGGER.info("=== SeiunAC verification reload completed ===");
            ServerNetworkHandler.broadcastVerificationSettings();
        }
    }

    private static void sendVerificationSettings(class_3222 player) {
        if (player == null) {
            return;
        }
        boolean blockPackChangeEnabled = hashWhitelist != null && hashWhitelist.isBlockPackChangeEnabled() && !player.method_75004().equals((Object)class_12086.field_63182);
        AntiCheatPackets.VerificationSettingsPayload payload = new AntiCheatPackets.VerificationSettingsPayload(blockPackChangeEnabled);
        if (ServerPlayNetworking.canSend((class_3222)player, AntiCheatPackets.VerificationSettingsPayload.ID)) {
            ServerPlayNetworking.send((class_3222)player, (class_8710)payload);
            SeiunAC.LOGGER.info("\u2192 Sent verification settings to {} (block-pack-change={})", (Object)player.method_5477().getString(), (Object)blockPackChangeEnabled);
        }
    }

    private static void broadcastVerificationSettings() {
        if (currentServer == null || hashWhitelist == null) {
            return;
        }
        for (class_3222 player : currentServer.method_3760().method_14571()) {
            ServerNetworkHandler.sendVerificationSettings(player);
        }
    }

    static {
        pendingPlayers = new ConcurrentHashMap<UUID, Long>();
        approvedPackBypassIds = new ConcurrentHashMap<UUID, Set<String>>();
        verifiedPlayers = Collections.newSetFromMap(new ConcurrentHashMap());
        isReloading = false;
    }
}
