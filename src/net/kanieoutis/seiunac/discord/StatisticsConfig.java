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
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.SeiunAC;

public class StatisticsConfig {
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
    private static final File ANTICHEAT_DIR = new File(CONFIG_DIR, "SeiunAC-anticheat");
    private static final File STATS_FILE = new File(ANTICHEAT_DIR, "statistics.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public Statistics stats = new Statistics();
    public String firstStarted = "";
    public String lastUpdated = "";
    public long totalPlayersChecked = 0L;

    public static StatisticsConfig load() {
        if (!ANTICHEAT_DIR.exists() && !ANTICHEAT_DIR.mkdirs()) {
            SeiunAC.LOGGER.warn("Could not create statistics directory: {}", (Object)ANTICHEAT_DIR);
        }
        if (STATS_FILE.exists()) {
            StatisticsConfig statisticsConfig;
            FileReader reader = new FileReader(STATS_FILE);
            try {
                StatisticsConfig config = (StatisticsConfig)GSON.fromJson((Reader)reader, StatisticsConfig.class);
                SeiunAC.LOGGER.info("Statistics loaded ({} players checked)", (Object)config.totalPlayersChecked);
                statisticsConfig = config;
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
                    SeiunAC.LOGGER.error("Error loading statistics: {}", (Object)e.getMessage());
                }
            }
            reader.close();
            return statisticsConfig;
        }
        StatisticsConfig config = new StatisticsConfig();
        config.firstStarted = LocalDateTime.now().format(DATE_FORMAT);
        config.save();
        SeiunAC.LOGGER.info("Created new statistics");
        return config;
    }

    public void save() {
        this.lastUpdated = LocalDateTime.now().format(DATE_FORMAT);
        try (FileWriter writer = new FileWriter(STATS_FILE);){
            GSON.toJson((Object)this, (Appendable)writer);
        }
        catch (IOException e) {
            SeiunAC.LOGGER.error("Error saving statistics: {}", (Object)e.getMessage());
        }
    }

    public void incrementPlayerChecked() {
        ++this.totalPlayersChecked;
        this.save();
    }

    public void incrementPlayerJoinSuccess() {
        ++this.stats.playersJoinedSuccessfully;
        this.save();
    }

    public void incrementKick(String reason) {
        ++this.stats.playersKickedTotal;
        if (!reason.contains("Prohibited mods") && !reason.contains("prohibited mods")) {
            if (!reason.contains("Modified") && !reason.contains("modified")) {
                if (!reason.contains("Timeout") && !reason.contains("no mods received")) {
                    if (!reason.contains("Anti-Cheat Mod missing") && !reason.contains("not installed")) {
                        if (reason.contains("Resource Pack") || reason.contains("resource pack")) {
                            ++this.stats.kicksIllegalResourcePacks;
                        }
                    } else {
                        ++this.stats.kicksNoAntiCheatMod;
                    }
                } else {
                    ++this.stats.kicksTimeout;
                }
            } else {
                ++this.stats.kicksModifiedMods;
            }
        } else {
            ++this.stats.kicksIllegalMods;
        }
        this.save();
    }

    public void incrementIllegalMods(int count) {
        Statistics var10000 = this.stats;
        var10000.totalIllegalModsDetected += (long)count;
        this.save();
    }

    public void incrementModifiedMods(int count) {
        Statistics var10000 = this.stats;
        var10000.totalModifiedModsDetected += (long)count;
        this.save();
    }

    public void incrementAntiCheatTampered() {
        ++this.stats.antiCheatModTampered;
        this.save();
    }

    public void incrementIllegalResourcePacks(int count) {
        Statistics var10000 = this.stats;
        var10000.illegalResourcePacksDetected += (long)count;
        this.save();
    }

    public void incrementDisconnect() {
        ++this.stats.playerDisconnects;
        this.save();
    }

    public void incrementServerStart() {
        ++this.stats.serverStarts;
        this.save();
    }

    public void incrementServerStop() {
        ++this.stats.serverStops;
        this.save();
    }

    public void incrementModeratorBypass() {
        ++this.stats.moderatorBypassesUsed;
        this.save();
    }

    public static class Statistics {
        public long playersJoinedSuccessfully = 0L;
        public long playersKickedTotal = 0L;
        public long kicksIllegalMods = 0L;
        public long kicksModifiedMods = 0L;
        public long kicksTimeout = 0L;
        public long kicksNoAntiCheatMod = 0L;
        public long kicksIllegalResourcePacks = 0L;
        public long totalIllegalModsDetected = 0L;
        public long totalModifiedModsDetected = 0L;
        public long antiCheatModTampered = 0L;
        public long illegalResourcePacksDetected = 0L;
        public long playerDisconnects = 0L;
        public long serverStarts = 0L;
        public long serverStops = 0L;
        public long moderatorBypassesUsed = 0L;
    }
}
