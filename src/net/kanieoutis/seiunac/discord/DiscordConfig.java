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
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.SeiunAC;

public class DiscordConfig {
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
    private static final File ANTICHEAT_DIR = new File(CONFIG_DIR, "SeiunAC-anticheat");
    private static final File DISCORD_DIR = new File(ANTICHEAT_DIR, "discord");
    private static final File CONFIG_FILE = new File(DISCORD_DIR, "webhook.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = false;
    public WebhookFeatures webhooks = new WebhookFeatures();
    public FeatureToggles features = new FeatureToggles();
    public String dispatchCommands = "gamemode, give, effect, recipe";

    public String getWebhookUrl(String feature) {
        if (!this.enabled) {
            return null;
        }
        String webhookUrl = null;
        switch (feature.toLowerCase()) {
            case "playerjoin": {
                webhookUrl = this.webhooks.playerJoin;
                break;
            }
            case "opjoin": {
                webhookUrl = this.webhooks.opJoin;
                break;
            }
            case "opchange": {
                webhookUrl = this.webhooks.opChange;
                break;
            }
            case "dispatchcommands": {
                webhookUrl = this.webhooks.dispatchCommands;
                break;
            }
            case "playerkick": {
                webhookUrl = this.webhooks.playerKick;
                break;
            }
            case "illegalmods": {
                webhookUrl = this.webhooks.illegalMods;
                break;
            }
            case "modifiedmods": {
                webhookUrl = this.webhooks.modifiedMods;
                break;
            }
            case "modswarning": {
                webhookUrl = DiscordConfig.firstNonEmpty(this.webhooks.modsWarning, this.webhooks.illegalMods);
                break;
            }
            case "packswarning": {
                webhookUrl = DiscordConfig.firstNonEmpty(this.webhooks.packsWarning, this.webhooks.modifiedMods);
                break;
            }
            case "packchangelog": {
                webhookUrl = DiscordConfig.firstNonEmpty(this.webhooks.packChangeLog, this.webhooks.modifiedMods, this.webhooks.hashMismatch);
                break;
            }
            case "playerdisconnect": {
                webhookUrl = this.webhooks.playerDisconnect;
                break;
            }
            case "serverstart": {
                webhookUrl = this.webhooks.serverStart;
                break;
            }
            case "serverstop": {
                webhookUrl = this.webhooks.serverStop;
                break;
            }
            case "hashmismatch": {
                webhookUrl = this.webhooks.hashMismatch;
            }
        }
        return webhookUrl != null && !webhookUrl.isEmpty() ? webhookUrl : null;
    }

    public boolean isFeatureEnabled(String feature) {
        if (!this.enabled) {
            return false;
        }
        switch (feature.toLowerCase()) {
            case "playerjoin": {
                return this.features.playerJoin;
            }
            case "opjoin": {
                return this.features.opJoin;
            }
            case "opchange": {
                return this.features.opChange;
            }
            case "dispatchcommands": {
                return this.features.dispatchCommandsTrigger;
            }
            case "playerkick": {
                return this.features.playerKick;
            }
            case "illegalmods": {
                return this.features.illegalMods;
            }
            case "modifiedmods": {
                return this.features.modifiedMods;
            }
            case "modswarning": {
                return this.features.modsWarning;
            }
            case "packswarning": {
                return this.features.packsWarning;
            }
            case "packchangelog": {
                return this.features.packChangeLog;
            }
            case "playerdisconnect": {
                return this.features.playerDisconnect;
            }
            case "serverstart": {
                return this.features.serverStart;
            }
            case "serverstop": {
                return this.features.serverStop;
            }
            case "hashmismatch": {
                return this.features.hashMismatch;
            }
        }
        return false;
    }

    public static DiscordConfig load() {
        if (!DISCORD_DIR.exists() && !DISCORD_DIR.mkdirs()) {
            SeiunAC.LOGGER.warn("Could not create Discord config directory: {}", (Object)DISCORD_DIR);
        }
        if (CONFIG_FILE.exists()) {
            DiscordConfig discordConfig;
            FileReader reader = new FileReader(CONFIG_FILE);
            try {
                DiscordConfig config = (DiscordConfig)GSON.fromJson((Reader)reader, DiscordConfig.class);
                if (config == null) {
                    config = new DiscordConfig();
                }
                config.applyDefaults();
                config.save();
                SeiunAC.LOGGER.info("Discord-Config loaded (enabled: {})", (Object)config.enabled);
                discordConfig = config;
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
                    SeiunAC.LOGGER.error("Error loading Discord-Config: {}", (Object)e.getMessage());
                }
            }
            reader.close();
            return discordConfig;
        }
        DiscordConfig config = new DiscordConfig();
        config.save();
        SeiunAC.LOGGER.info("New Discord-Config created (disabled - please enter webhook URL)");
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE);){
            this.applyDefaults();
            GSON.toJson((Object)this, (Appendable)writer);
            SeiunAC.LOGGER.info("Discord-Config saved");
        }
        catch (IOException e) {
            SeiunAC.LOGGER.error("Error saving Discord-Config: {}", (Object)e.getMessage());
        }
    }

    private void applyDefaults() {
        if (this.webhooks == null) {
            this.webhooks = new WebhookFeatures();
        }
        if (this.features == null) {
            this.features = new FeatureToggles();
        }
        if (this.dispatchCommands == null) {
            this.dispatchCommands = "gamemode, give, effect, recipe";
        }
        if (this.webhooks.dispatchCommands == null) {
            this.webhooks.dispatchCommands = "";
        }
        if (this.features.playerJoin == null) {
            this.features.playerJoin = Boolean.TRUE;
        }
        if (this.features.opJoin == null) {
            this.features.opJoin = Boolean.TRUE;
        }
        if (this.features.opChange == null) {
            this.features.opChange = Boolean.TRUE;
        }
        if (this.features.dispatchCommandsTrigger == null) {
            this.features.dispatchCommandsTrigger = Boolean.TRUE;
        }
        if (this.features.playerKick == null) {
            this.features.playerKick = Boolean.TRUE;
        }
        if (this.features.illegalMods == null) {
            this.features.illegalMods = Boolean.TRUE;
        }
        if (this.features.modifiedMods == null) {
            this.features.modifiedMods = Boolean.TRUE;
        }
        if (this.features.modsWarning == null) {
            this.features.modsWarning = Boolean.TRUE;
        }
        if (this.features.packsWarning == null) {
            this.features.packsWarning = Boolean.TRUE;
        }
        if (this.features.packChangeLog == null) {
            this.features.packChangeLog = Boolean.TRUE;
        }
        if (this.features.playerDisconnect == null) {
            this.features.playerDisconnect = Boolean.TRUE;
        }
        if (this.features.serverStart == null) {
            this.features.serverStart = Boolean.TRUE;
        }
        if (this.features.serverStop == null) {
            this.features.serverStop = Boolean.TRUE;
        }
        if (this.features.hashMismatch == null) {
            this.features.hashMismatch = Boolean.TRUE;
        }
    }

    public boolean isDispatchCommandTracked(String commandName) {
        if (commandName == null || commandName.isBlank()) {
            return false;
        }
        String normalizedCommand = DiscordConfig.normalizeCommandName(commandName);
        if (normalizedCommand.isEmpty()) {
            return false;
        }
        for (String entry : this.getDispatchCommandList()) {
            if (!normalizedCommand.equals(entry)) continue;
            return true;
        }
        return false;
    }

    public List<String> getDispatchCommandList() {
        ArrayList<String> commands = new ArrayList<String>();
        if (this.dispatchCommands == null || this.dispatchCommands.isBlank()) {
            return commands;
        }
        for (String entry : this.dispatchCommands.split(",")) {
            String normalized = DiscordConfig.normalizeCommandName(entry);
            if (normalized.isEmpty() || commands.contains(normalized)) continue;
            commands.add(normalized);
        }
        return commands;
    }

    private static String normalizeCommandName(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toLowerCase();
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }

    private static String firstNonEmpty(String ... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            return value;
        }
        return null;
    }

    public static class WebhookFeatures {
        public String playerJoin = "";
        public String opJoin = "";
        public String opChange = "";
        public String dispatchCommands = "";
        public String playerKick = "";
        public String illegalMods = "";
        public String modifiedMods = "";
        public String modsWarning = "";
        public String packsWarning = "";
        public String packChangeLog = "";
        public String playerDisconnect = "";
        public String serverStart = "";
        public String serverStop = "";
        public String hashMismatch = "";
    }

    public static class FeatureToggles {
        public Boolean playerJoin = Boolean.TRUE;
        public Boolean opJoin = Boolean.TRUE;
        public Boolean opChange = Boolean.TRUE;
        public Boolean dispatchCommandsTrigger = Boolean.TRUE;
        public Boolean playerKick = Boolean.TRUE;
        public Boolean illegalMods = Boolean.TRUE;
        public Boolean modifiedMods = Boolean.TRUE;
        public Boolean modsWarning = Boolean.TRUE;
        public Boolean packsWarning = Boolean.TRUE;
        public Boolean packChangeLog = Boolean.TRUE;
        public Boolean playerDisconnect = Boolean.TRUE;
        public Boolean serverStart = Boolean.TRUE;
        public Boolean serverStop = Boolean.TRUE;
        public Boolean hashMismatch = Boolean.TRUE;
    }
}
