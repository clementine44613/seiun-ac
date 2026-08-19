/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 */
package net.kanieoutis.seiunac.discord;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.SeiunAC;

public class PlayerStatistics {
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
    private static final File ANTICHEAT_DIR = new File(CONFIG_DIR, "SeiunAC-anticheat");
    private static final File PLAYERS_DIR = new File(ANTICHEAT_DIR, "players");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public String playerName = "";
    public String playerUuid = "";
    public String firstSeen = "";
    public String lastSeen = "";
    public String lastIpAddress = "";
    public long totalConnections = 0L;
    public long successfulJoins = 0L;
    public long totalKicks = 0L;
    public double averageSessionTime = 0.0;
    public long totalPlayTime = 0L;
    public long kicksIllegalMods = 0L;
    public long kicksModifiedMods = 0L;
    public long kicksTimeout = 0L;
    public long kicksNoAntiCheatMod = 0L;
    public long kicksIllegalResourcePacks = 0L;
    public long illegalModsDetected = 0L;
    public long modifiedModsDetected = 0L;
    public long antiCheatTampered = 0L;
    public long illegalResourcePacksDetected = 0L;
    public List<ViolationRecord> recentViolations = new ArrayList<ViolationRecord>();
    public transient long currentSessionStart = 0L;
    public List<String> lastKnownMods = new ArrayList<String>();
    public int lastModCount = 0;
    public boolean hasUsedOpBypass = false;
    public long opBypassCount = 0L;

    public static PlayerStatistics load(UUID playerUuid, String playerName) {
        File playerFile;
        if (!PLAYERS_DIR.exists() && !PLAYERS_DIR.mkdirs()) {
            SeiunAC.LOGGER.warn("Could not create players directory: {}", (Object)PLAYERS_DIR);
        }
        if ((playerFile = new File(PLAYERS_DIR, playerUuid.toString() + ".json")).exists()) {
            PlayerStatistics playerStatistics;
            FileReader reader = new FileReader(playerFile);
            try {
                PlayerStatistics stats = (PlayerStatistics)GSON.fromJson((Reader)reader, PlayerStatistics.class);
                stats.lastSeen = LocalDateTime.now().format(DATE_FORMAT);
                stats.playerName = playerName;
                playerStatistics = stats;
            }
            catch (Throwable throwable) {
                try {
                    try {
                        reader.close();
                    }
                    catch (Throwable throwable2) {
                        throwable.addSuppressed(throwable2);
                    }
                    throw throwable;
                }
                catch (IOException e) {
                    SeiunAC.LOGGER.error("Error loading player statistics for {}: {}", (Object)playerName, (Object)e.getMessage());
                }
            }
            reader.close();
            return playerStatistics;
        }
        PlayerStatistics stats = new PlayerStatistics();
        stats.playerName = playerName;
        stats.playerUuid = playerUuid.toString();
        stats.lastSeen = stats.firstSeen = LocalDateTime.now().format(DATE_FORMAT);
        return stats;
    }

    public static void cleanupOldStatistics() {
        File[] playerFiles;
        if (PLAYERS_DIR.exists() && (playerFiles = PLAYERS_DIR.listFiles((dir, name) -> name.endsWith(".json"))) != null) {
            int deletedCount = 0;
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90L);
            for (File file : playerFiles) {
                try (FileReader reader = new FileReader(file);){
                    PlayerStatistics stats = (PlayerStatistics)GSON.fromJson((Reader)reader, PlayerStatistics.class);
                    LocalDateTime lastSeen = LocalDateTime.parse(stats.lastSeen, DATE_FORMAT);
                    if (!lastSeen.isBefore(cutoffDate) || !file.delete()) continue;
                    ++deletedCount;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            if (deletedCount > 0) {
                SeiunAC.LOGGER.info("Auto-Cleanup: {} old player statistics deleted", (Object)deletedCount);
            }
        }
    }

    public void save() {
        File playerFile = new File(PLAYERS_DIR, this.playerUuid + ".json");
        try (FileWriter writer = new FileWriter(playerFile);){
            GSON.toJson((Object)this, (Appendable)writer);
        }
        catch (IOException e) {
            SeiunAC.LOGGER.error("Error saving player statistics for {}: {}", (Object)this.playerName, (Object)e.getMessage());
        }
    }

    public void incrementConnection() {
        ++this.totalConnections;
        this.lastSeen = LocalDateTime.now().format(DATE_FORMAT);
        this.currentSessionStart = System.currentTimeMillis();
        this.save();
    }

    public void incrementConnection(String ipAddress) {
        this.incrementConnection();
        this.lastIpAddress = ipAddress;
        this.save();
    }

    public void incrementSuccessfulJoin() {
        ++this.successfulJoins;
        this.save();
    }

    public void incrementSuccessfulJoin(List<String> mods) {
        this.incrementSuccessfulJoin();
        this.lastKnownMods = new ArrayList<String>(mods);
        this.lastModCount = mods.size();
        this.save();
    }

    public void endSession() {
        if (this.currentSessionStart > 0L) {
            long sessionDuration = (System.currentTimeMillis() - this.currentSessionStart) / 1000L;
            this.totalPlayTime += sessionDuration;
            if (this.successfulJoins > 0L) {
                this.averageSessionTime = (double)this.totalPlayTime / (double)this.successfulJoins / 60.0;
            }
            this.currentSessionStart = 0L;
            this.save();
        }
    }

    public void incrementKick(String reason, String details) {
        ++this.totalKicks;
        if (!reason.contains("Prohibited Mods") && !reason.contains("prohibited Mods")) {
            if (!reason.contains("Modified") && !reason.contains("modified")) {
                if (!reason.contains("Timeout") && !reason.contains("no mods received")) {
                    if (!reason.contains("Anti-Cheat Mod missing") && !reason.contains("not installed")) {
                        if (reason.contains("Resource Pack") || reason.contains("resource pack")) {
                            ++this.kicksIllegalResourcePacks;
                            this.addViolation("ILLEGAL_RESOURCEPACK", details);
                        }
                    } else {
                        ++this.kicksNoAntiCheatMod;
                        this.addViolation("NO_ANTICHEAT", details);
                    }
                } else {
                    ++this.kicksTimeout;
                    this.addViolation("TIMEOUT", details);
                }
            } else {
                ++this.kicksModifiedMods;
                this.addViolation("MODIFIED_MODS", details);
            }
        } else {
            ++this.kicksIllegalMods;
            this.addViolation("ILLEGAL_MODS", details);
        }
        this.save();
    }

    public void incrementIllegalMods(int count) {
        this.illegalModsDetected += (long)count;
        this.save();
    }

    public void incrementModifiedMods(int count) {
        this.modifiedModsDetected += (long)count;
        this.save();
    }

    public void incrementAntiCheatTampered() {
        ++this.antiCheatTampered;
        this.addViolation("ANTICHEAT_TAMPERED", "Anti-Cheat Mod has been modified");
        this.save();
    }

    public void incrementIllegalResourcePacks(int count) {
        this.illegalResourcePacksDetected += (long)count;
        this.save();
    }

    public void incrementOpBypass() {
        this.hasUsedOpBypass = true;
        ++this.opBypassCount;
        this.save();
    }

    private void addViolation(String type, String details) {
        this.recentViolations.add(0, new ViolationRecord(type, details));
        if (this.recentViolations.size() > 10) {
            this.recentViolations = this.recentViolations.subList(0, 10);
        }
    }

    public String generateDiscordSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("**Player Statistics:**\n");
        summary.append("\u251c First connection: `").append(this.firstSeen).append("`\n");
        summary.append("\u251c Last connection: `").append(this.lastSeen).append("`\n");
        summary.append("\u251c Connections: `").append(this.totalConnections).append("`\n");
        summary.append("\u251c Successful joins: `").append(this.successfulJoins).append("`\n");
        summary.append("\u251c Kicks: `").append(this.totalKicks).append("`\n");
        if (this.totalPlayTime > 0L) {
            long hours = this.totalPlayTime / 3600L;
            long minutes = this.totalPlayTime % 3600L / 60L;
            summary.append("\u251c Total play time: `").append(hours).append("h ").append(minutes).append("m`\n");
            summary.append("\u251c Average session time: `").append(String.format("%.1f", this.averageSessionTime)).append(" Minutes`\n");
        }
        if (this.lastModCount > 0) {
            summary.append("\u251c Last mod count: `").append(this.lastModCount).append("`\n");
        }
        if (this.lastIpAddress != null && !this.lastIpAddress.isEmpty()) {
            summary.append("\u251c Last IP: `").append(this.lastIpAddress).append("`\n");
        }
        if (this.totalKicks > 0L) {
            summary.append("\u2514 **Violations:**\n");
            if (this.kicksIllegalMods > 0L) {
                summary.append("  \u251c Prohibited Mods: `").append(this.kicksIllegalMods).append("`\n");
            }
            if (this.kicksModifiedMods > 0L) {
                summary.append("  \u251c Modified Mods: `").append(this.kicksModifiedMods).append("`\n");
            }
            if (this.kicksTimeout > 0L) {
                summary.append("  \u251c Timeouts: `").append(this.kicksTimeout).append("`\n");
            }
            if (this.kicksIllegalResourcePacks > 0L) {
                summary.append("  \u251c Prohibited Resource Packs: `").append(this.kicksIllegalResourcePacks).append("`\n");
            }
            if (this.antiCheatTampered > 0L) {
                summary.append("  \u2514 Anti-Cheat tampered: `").append(this.antiCheatTampered).append("`\n");
            }
        }
        return summary.toString();
    }

    public static class ViolationRecord {
        public String timestamp = LocalDateTime.now().format(DATE_FORMAT);
        public String type;
        public String details;

        public ViolationRecord(String type, String details) {
            this.type = type;
            this.details = details;
        }
    }
}
