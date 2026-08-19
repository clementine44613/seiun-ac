/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.mojang.brigadier.CommandDispatcher
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.StringArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_12086
 *  net.minecraft.class_2168
 *  net.minecraft.class_2170
 *  net.minecraft.class_2561
 *  net.minecraft.class_3222
 */
package net.kanieoutis.seiunac.command;

import com.google.gson.Gson;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.config.HashWhitelist;
import net.kanieoutis.seiunac.discord.PlayerStatistics;
import net.kanieoutis.seiunac.discord.StatisticsConfig;
import net.kanieoutis.seiunac.server.ServerNetworkHandler;
import net.kanieoutis.seiunac.util.ModHasher;
import net.minecraft.class_12086;
import net.minecraft.class_2168;
import net.minecraft.class_2170;
import net.minecraft.class_2561;
import net.minecraft.class_3222;

public class AntiCheatCommand {
    public static void register(CommandDispatcher<class_2168> dispatcher) {
        dispatcher.register(AntiCheatCommand.buildRootCommand("SeiunAC"));
    }

    private static LiteralArgumentBuilder<class_2168> buildRootCommand(String rootName) {
        return (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)class_2170.method_9247((String)rootName).requires(source -> source.method_75037().equals((Object)class_12086.field_63185))).then(((LiteralArgumentBuilder)class_2170.method_9247((String)"list").then(class_2170.method_9247((String)"mods").executes(AntiCheatCommand::listMods))).then(class_2170.method_9247((String)"packs").executes(AntiCheatCommand::listResourcePacks)))).then(((LiteralArgumentBuilder)class_2170.method_9247((String)"scan").then(class_2170.method_9247((String)"mods").executes(AntiCheatCommand::scanFolder))).then(class_2170.method_9247((String)"packs").executes(AntiCheatCommand::scanResourcePacks)))).then(class_2170.method_9247((String)"reload").executes(AntiCheatCommand::reload))).then(class_2170.method_9247((String)"stats").executes(AntiCheatCommand::showStats))).then(class_2170.method_9247((String)"cleanup").executes(AntiCheatCommand::cleanupStats))).then(class_2170.method_9247((String)"player").then(class_2170.method_9244((String)"playerName", (ArgumentType)StringArgumentType.word()).executes(context -> AntiCheatCommand.showPlayerStats((CommandContext<class_2168>)context, StringArgumentType.getString((CommandContext)context, (String)"playerName")))))).then(class_2170.method_9247((String)"hash").then(class_2170.method_9244((String)"modId", (ArgumentType)StringArgumentType.word()).executes(context -> AntiCheatCommand.showHash((CommandContext<class_2168>)context, StringArgumentType.getString((CommandContext)context, (String)"modId")))));
    }

    private static int listMods(CommandContext<class_2168> context) {
        HashWhitelist verification = ServerNetworkHandler.getHashWhitelist();
        class_2168 source = (class_2168)context.getSource();
        source.method_9226(() -> class_2561.method_43470((String)"\u00a7e=== SeiunAC Mod Verification ==="), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Whitelist: config/SeiunAC-anticheat/verification/mod_whitelist.json"), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Graylist: config/SeiunAC-anticheat/verification/graymods/"), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Blacklist: config/SeiunAC-anticheat/verification/mod_blacklist.json"), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Config: config/SeiunAC-anticheat/verification/config.json"), false);
        source.method_9226(() -> class_2561.method_43470((String)("\u00a77 - library-bypass: " + verification.isLibraryBypassEnabled())), false);
        source.method_9226(() -> class_2561.method_43470((String)("\u00a77 - block-pack-change: " + verification.isBlockPackChangeEnabled())), false);
        AntiCheatCommand.printStringSet(source, "\u00a76Whitelisted Mods (" + verification.getModWhitelistIds().size() + "):", verification.getModWhitelistIds());
        AntiCheatCommand.printGrayMap(source, "\u00a76Graylisted Mods (" + verification.getGrayModHashes().size() + "):", verification.getGrayModHashes());
        AntiCheatCommand.printStringSet(source, "\u00a76Blacklisted Mods (" + verification.getModBlacklistIds().size() + "):", verification.getModBlacklistIds());
        return 1;
    }

    private static int listResourcePacks(CommandContext<class_2168> context) {
        HashWhitelist verification = ServerNetworkHandler.getHashWhitelist();
        class_2168 source = (class_2168)context.getSource();
        source.method_9226(() -> class_2561.method_43470((String)"\u00a7e=== SeiunAC Resource Pack Verification ==="), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Whitelist: config/SeiunAC-anticheat/verification/pack_whitelist.json"), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Graylist: config/SeiunAC-anticheat/verification/graypacks/"), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Blacklist: config/SeiunAC-anticheat/verification/pack_blacklist.json"), false);
        source.method_9226(() -> class_2561.method_43470((String)"\u00a77Config: config/SeiunAC-anticheat/verification/config.json"), false);
        source.method_9226(() -> class_2561.method_43470((String)("\u00a77 - library-bypass: " + verification.isLibraryBypassEnabled())), false);
        source.method_9226(() -> class_2561.method_43470((String)("\u00a77 - block-pack-change: " + verification.isBlockPackChangeEnabled())), false);
        AntiCheatCommand.printStringSet(source, "\u00a76Whitelisted Packs (" + verification.getPackWhitelistNames().size() + "):", verification.getPackWhitelistNames());
        AntiCheatCommand.printGrayMap(source, "\u00a76Graylisted Packs (" + verification.getGrayPackHashes().size() + "):", verification.getGrayPackHashes());
        AntiCheatCommand.printStringSet(source, "\u00a76Blacklisted Packs (" + verification.getPackBlacklistNames().size() + "):", verification.getPackBlacklistNames());
        return 1;
    }

    private static int scanFolder(CommandContext<class_2168> context) {
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7e[SeiunAC] \u00a77Reloading verification lists..."), true);
        ServerNetworkHandler.reloadHashWhitelist();
        ModHasher.clearCache();
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7a[SeiunAC] \u00a77Verification reload completed!"), true);
        return 1;
    }

    private static int scanResourcePacks(CommandContext<class_2168> context) {
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7e[SeiunAC] \u00a77Reloading resource pack verification..."), true);
        Thread scanThread = new Thread(() -> {
            try {
                ServerNetworkHandler.reloadHashWhitelist();
                ModHasher.clearCache();
                ((class_2168)context.getSource()).method_9211().execute(() -> ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7a[SeiunAC] \u00a77Resource pack verification reload completed!"), true));
            }
            catch (Exception e) {
                ((class_2168)context.getSource()).method_9211().execute(() -> ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)("\u00a7c[SeiunAC] Failed to reload resource pack verification: " + e.getMessage()))));
            }
        }, "SeiunAC-ResourcePackScan");
        scanThread.setDaemon(true);
        scanThread.start();
        return 1;
    }

    private static int reload(CommandContext<class_2168> context) {
        if (ServerNetworkHandler.isReloading()) {
            ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)"\u00a7c[SeiunAC] A reload is already in progress!"));
            return 0;
        }
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7e[SeiunAC] \u00a77Starting reload..."), true);
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7e[SeiunAC] \u00a77New joins are blocked during reload"), true);
        Thread reloadThread = new Thread(() -> {
            try {
                ServerNetworkHandler.reloadHashWhitelist();
                ModHasher.clearCache();
                ((class_2168)context.getSource()).method_9211().execute(() -> {
                    HashWhitelist verification = ServerNetworkHandler.getHashWhitelist();
                    ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7a[SeiunAC] \u00a77Reload completed!"), true);
                    ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a7a[SeiunAC] \u00a77\u2713 " + verification.getModWhitelistIds().size() + " whitelisted mods")), true);
                    ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a7a[SeiunAC] \u00a77\u2713 " + verification.getGrayModHashes().size() + " gray mods")), true);
                    ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a7a[SeiunAC] \u00a77\u2713 " + verification.getPackWhitelistNames().size() + " whitelisted packs")), true);
                    ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a7a[SeiunAC] \u00a77Config: library-bypass=" + verification.isLibraryBypassEnabled() + ", block-pack-change=" + verification.isBlockPackChangeEnabled())), true);
                    ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7a[SeiunAC] \u00a77Players may now join again"), true);
                });
            }
            catch (Exception e) {
                ((class_2168)context.getSource()).method_9211().execute(() -> ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)("\u00a7c[SeiunAC] Reload failed: " + e.getMessage()))));
            }
        }, "SeiunAC-Reload");
        reloadThread.setDaemon(true);
        reloadThread.start();
        return 1;
    }

    private static int cleanupStats(CommandContext<class_2168> context) {
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7e[SeiunAC] \u00a77Starting cleanup of old player statistics..."), true);
        Thread cleanupThread = new Thread(() -> {
            try {
                PlayerStatistics.cleanupOldStatistics();
                ((class_2168)context.getSource()).method_9211().execute(() -> ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)"\u00a7a[SeiunAC] \u00a77Cleanup completed!"), true));
            }
            catch (Exception e) {
                ((class_2168)context.getSource()).method_9211().execute(() -> ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)("\u00a7c[SeiunAC] Cleanup failed: " + e.getMessage()))));
            }
        }, "SeiunAC-Cleanup");
        cleanupThread.setDaemon(true);
        cleanupThread.start();
        return 1;
    }

    private static int showHash(CommandContext<class_2168> context, String modId) {
        String hash = ModHasher.getModHash(modId);
        if (hash.equals("UNKNOWN")) {
            ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)("\u00a7c[SeiunAC] Mod '" + modId + "' not found!")));
            return 0;
        }
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a7e[SeiunAC] Hash for '" + modId + "':")), false);
        ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a77" + hash)), false);
        return 1;
    }

    private static int showPlayerStats(CommandContext<class_2168> context, String playerName) {
        class_3222 player = ((class_2168)context.getSource()).method_9211().method_3760().method_14566(playerName);
        if (player == null) {
            File playersDir = new File(FabricLoader.getInstance().getConfigDir().toFile(), "SeiunAC-anticheat/players");
            if (!playersDir.exists()) {
                ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)("\u00a7c[SeiunAC] Player '" + playerName + "' not found!")));
                return 0;
            }
            File[] files = playersDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (files == null) {
                ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)"\u00a7c[SeiunAC] No player data available"));
                return 0;
            }
            for (File file : files) {
                FileReader reader;
                block14: {
                    int n;
                    reader = new FileReader(file);
                    PlayerStatistics stats = (PlayerStatistics)new Gson().fromJson((Reader)reader, PlayerStatistics.class);
                    if (stats == null || stats.playerName == null || !stats.playerName.equalsIgnoreCase(playerName)) break block14;
                    try {
                        String report = stats.generateFullReport();
                        for (String line : report.split("\n")) {
                            ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a77" + line)), false);
                        }
                        n = 1;
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
                        catch (Exception exception) {
                            // empty catch block
                        }
                    }
                    reader.close();
                    return n;
                }
                reader.close();
            }
            ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)("\u00a7c[SeiunAC] Player '" + playerName + "' not found!")));
            return 0;
        }
        PlayerStatistics stats = PlayerStatistics.load(player.method_5667(), player.method_5477().getString());
        String report = stats.generateFullReport();
        for (String line : report.split("\n")) {
            ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a77" + line)), false);
        }
        return 1;
    }

    private static int showStats(CommandContext<class_2168> context) {
        StatisticsConfig stats = ServerNetworkHandler.getStatistics();
        if (stats == null) {
            ((class_2168)context.getSource()).method_9213((class_2561)class_2561.method_43470((String)"\u00a7c[SeiunAC] Error loading statistics"));
            return 0;
        }
        String report = stats.generateReport();
        for (String line : report.split("\n")) {
            ((class_2168)context.getSource()).method_9226(() -> class_2561.method_43470((String)("\u00a77" + line)), false);
        }
        return 1;
    }

    private static void printStringSet(class_2168 source, String header, Iterable<String> values) {
        source.method_9226(() -> class_2561.method_43470((String)header), false);
        int count = 0;
        Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            String value;
            String entry = value = iterator.next();
            source.method_9226(() -> class_2561.method_43470((String)("\u00a77- " + entry)), false);
            ++count;
        }
        if (count == 0) {
            source.method_9226(() -> class_2561.method_43470((String)"\u00a77- (empty)"), false);
        }
    }

    private static void printGrayMap(class_2168 source, String header, Map<String, String> values) {
        source.method_9226(() -> class_2561.method_43470((String)header), false);
        if (values.isEmpty()) {
            source.method_9226(() -> class_2561.method_43470((String)"\u00a77- (empty)"), false);
            return;
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String key = entry.getKey();
            String hash = entry.getValue();
            String preview = hash.length() > 16 ? hash.substring(0, 16) + "..." : hash;
            source.method_9226(() -> class_2561.method_43470((String)("\u00a77- " + key + " \u00a78[" + preview + "]")), false);
        }
    }
}
