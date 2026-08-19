/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package net.kanieoutis.seiunac.discord;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.discord.DiscordConfig;
import net.kanieoutis.seiunac.discord.PlayerStatistics;
import net.kanieoutis.seiunac.util.AntiCheatErrorCode;

public class DiscordWebhook {
    private final DiscordConfig config;

    public DiscordWebhook(DiscordConfig config) {
        this.config = config;
    }

    public void sendMessage(String feature, String message) {
        String webhookUrl;
        if (this.config.isFeatureEnabled(feature) && (webhookUrl = this.config.getWebhookUrl(feature)) != null && !webhookUrl.isEmpty()) {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("content", message);
                this.sendWebhook(webhookUrl, json.toString());
            }
            catch (Exception e) {
                SeiunAC.LOGGER.error("Error sending Discord message: {}", (Object)e.getMessage());
            }
        }
    }

    public void sendEmbed(String feature, String title, String description, int color, Map<String, String> fields) {
        String webhookUrl;
        if (this.config.isFeatureEnabled(feature) && (webhookUrl = this.config.getWebhookUrl(feature)) != null && !webhookUrl.isEmpty()) {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", title);
                embed.addProperty("description", description);
                embed.addProperty("color", (Number)color);
                embed.addProperty("timestamp", Instant.now().toString());
                if (fields != null && !fields.isEmpty()) {
                    JsonArray fieldsArray = new JsonArray();
                    for (Map.Entry<String, String> entry : fields.entrySet()) {
                        JsonObject field = new JsonObject();
                        field.addProperty("name", entry.getKey());
                        field.addProperty("value", entry.getValue());
                        field.addProperty("inline", Boolean.valueOf(false));
                        fieldsArray.add((JsonElement)field);
}
                    embed.add("fields", (JsonElement)fieldsArray);
                }
                JsonObject footer = new JsonObject();
                footer.addProperty("text", "SeiunAC Anti-Cheat");
                embed.add("footer", (JsonElement)footer);
                JsonObject json = new JsonObject();
                json.add("embeds", (JsonElement)new JsonArray());
                json.getAsJsonArray("embeds").add((JsonElement)embed);
                this.sendWebhook(webhookUrl, json.toString());
            }
            catch (Exception e) {
                SeiunAC.LOGGER.error("Error sending Discord embed: {}", (Object)e.getMessage());
            }
        }
    }

    public void sendEmbed(String feature, String title, String description, int color) {
        this.sendEmbed(feature, title, description, color, null);
    }

    public void sendPlayerKick(String playerName, String reason, PlayerStatistics playerStats) {
        this.sendPlayerKick(playerName, reason, null, null, playerStats);
    }

    public void sendPlayerKick(String playerName, String reason, String errorCode, PlayerStatistics playerStats) {
        this.sendPlayerKick(playerName, reason, errorCode, null, playerStats);
    }

    public void sendPlayerKick(String playerName, String reason, String errorCode, String violationDetails, PlayerStatistics playerStats) {
        String cleanReason = reason.replace("\u00a7", "").replaceAll("[0-9a-fk-or]", "");
        AntiCheatErrorCode resolved = AntiCheatErrorCode.fromCode(errorCode);
        String reasonLabel = resolved != null ? resolved.getDescription() : cleanReason.split("\n")[0];
        String errorLabel = resolved != null ? resolved.getCode() : errorCode;
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Reason:** ").append(reasonLabel).append("\n");
        if (errorLabel != null && !errorLabel.isEmpty()) {
            description.append("**Error-Code:** `").append(errorLabel).append("`\n");
        }
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (playerStats != null) {
            fields.put("\ud83d\udcca Statistics", String.format("Total: `%d` | Successful: `%d` | Kicks: `%d`", playerStats.totalConnections, playerStats.successfulJoins, playerStats.totalKicks));
            List<String> prohibitedMods = DiscordWebhook.extractViolationEntries(violationDetails, "Blacklisted Mods:");
            if (!prohibitedMods.isEmpty()) {
                fields.put("\ud83d\udeab Prohibited Mods Triggered", DiscordWebhook.formatEntryList(prohibitedMods, 10));
            }
            if (playerStats.totalKicks > 1L) {
                fields.put("\u26a0\ufe0f Violations", String.format("Prohibited Mods: `%d` | Mod code edited: `%d` | Timeouts: `%d`", playerStats.kicksIllegalMods, playerStats.kicksModifiedMods, playerStats.kicksTimeout));
            }
            fields.put("\ud83d\udd50 Times", String.format("First Connection: `%s`\nLast Connection: `%s`", playerStats.firstSeen, playerStats.lastSeen));
        }
        this.sendEmbed("playerKick", "\ud83d\udeab Player Kicked" + (String)(reasonLabel != null ? " [" + reasonLabel + "]" : ""), description.toString(), 0xFF0000, fields);
    }

    private static List<String> extractViolationEntries(String violationDetails, String label) {
        String[] segments;
        if (violationDetails == null || violationDetails.isBlank() || label == null || label.isBlank()) {
            return List.of();
        }
        for (String segment : segments = violationDetails.split(";")) {
            String trimmed = segment.trim();
            if (!trimmed.regionMatches(true, 0, label, 0, label.length())) continue;
            String rawEntries = trimmed.substring(label.length()).trim();
            if (rawEntries.isEmpty()) {
                return List.of();
            }
            String[] values = rawEntries.split(",");
            ArrayList<String> entries = new ArrayList<String>(values.length);
            for (String value : values) {
                String normalized = value.trim();
                if (normalized.isEmpty()) continue;
                entries.add(normalized);
            }
            return entries;
        }
        return List.of();
    }

    private static String formatEntryList(List<String> entries, int limit) {
        StringBuilder builder = new StringBuilder();
        int max = Math.min(limit, entries.size());
        for (int i = 0; i < max; ++i) {
            if (i > 0) {
                builder.append('\n');
            }
            builder.append("\u2022 `").append(entries.get(i)).append("`");
        }
        if (entries.size() > limit) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("... and ").append(entries.size() - limit).append(" more");
        }
        return builder.toString();
    }

    public void sendPlayerJoin(String playerName, PlayerStatistics playerStats) {
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Status:** \u2705 Successfully Joined\n");
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (playerStats != null) {
            fields.put("\ud83d\udcca Statistics", String.format("Connections: `%d` | Successful Joins: `%d`", playerStats.totalConnections, playerStats.successfulJoins));
            if (playerStats.totalKicks > 0L) {
                fields.put("\u26a0\ufe0f Warning", String.format("This player has been kicked `%d` times!", playerStats.totalKicks));
            }
            fields.put("\ud83d\udd50 Times", String.format("First Connection: `%s`\nLast Connection: `%s`", playerStats.firstSeen, playerStats.lastSeen));
        }
        this.sendEmbed("playerJoin", "\u2705 Player Joined", description.toString(), 65280, fields);
    }

    public void sendOpJoin(String playerName) {
        String description = "**Player:** " + playerName + "\n**Status:** OP detected - Verification skipped";
        this.sendEmbed("opJoin", "\ud83d\udc51 OP Joined", description, 16766720);
    }

    public void sendOperatorChange(String actorName, List<String> targetNames, boolean granted) {
        if (targetNames == null || targetNames.isEmpty()) {
            return;
        }
        StringBuilder titlebuilder = new StringBuilder(granted ? "\ud83d\udc51 OP Granted for " : "\ud83d\udd3b OP Revoked for ");
        titlebuilder.append(String.join((CharSequence)", ", targetNames));
        StringBuilder description = new StringBuilder();
        description.append("**Command Executed by:** `").append(actorName == null || actorName.isBlank() ? "Unknown" : actorName).append("`\n");
        description.append("**Action:** ").append(granted ? "Operator granted" : "Operator revoked").append("\n");
        description.append("**Count:** `").append(targetNames.size()).append("`\n");
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        this.sendEmbed("opChange", titlebuilder.toString(), description.toString(), granted ? 16766720 : 427980, fields);
    }

    public boolean shouldDispatchCommand(String commandName) {
        return this.config != null && this.config.isFeatureEnabled("dispatchCommands") && this.config.isDispatchCommandTracked(commandName);
    }

    public void sendDispatchCommand(String actorName, String commandName, String rawCommand) {
        String executor = actorName == null || actorName.isBlank() ? "Unknown" : actorName;
        String normalizedCommand = commandName == null || commandName.isBlank() ? "unknown" : commandName.trim();
        String normalizedRaw = rawCommand == null || rawCommand.isBlank() ? normalizedCommand : rawCommand.trim();
        StringBuilder description = new StringBuilder();
        description.append("**Executor:** `").append(executor).append("`\n");
        description.append("**Command:** `").append(normalizedCommand).append("`\n");
        description.append("**Raw Input:** `").append(normalizedRaw).append("`\n");
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("\u2699\ufe0f Dispatch", "Tracked command execution");
        this.sendEmbed("dispatchCommands", "\u2699\ufe0f Command Executed", description.toString(), 16753920, fields);
    }

    public void sendIllegalMods(String playerName, List<String> mods, String errorCode, PlayerStatistics playerStats) {
        StringBuilder modsText = new StringBuilder();
        for (int i = 0; i < Math.min(15, mods.size()); ++i) {
            modsText.append("\u2022 `").append(mods.get(i)).append("`\n");
        }
        if (mods.size() > 15) {
            modsText.append("... and ").append(mods.size() - 15).append(" more");
        }
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Prohibited Mod Count:** `").append(mods.size()).append("`\n");
        if (errorCode != null) {
            description.append("**Error-Code:** `").append(errorCode).append("`\n");
        }
        description.append("\n**Prohibited Mods:**\n").append((CharSequence)modsText);
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (playerStats != null) {
            fields.put("\ud83d\udcca Player Statistics", String.format("Total Violations: `%d`\nKicks: `%d`", playerStats.illegalModsDetected + playerStats.modifiedModsDetected, playerStats.totalKicks));
        }
        this.sendEmbed("illegalMods", "\u26a0\ufe0f Prohibited Mods Detected" + (String)(errorCode != null ? " [" + errorCode + "]" : ""), description.toString(), 16753920, fields);
    }

    public void sendModifiedMod(String playerName, String modName, String clientHash, String serverHash, String errorCode, PlayerStatistics playerStats) {
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Mod:** `").append(modName).append("`\n");
        if (errorCode != null) {
            description.append("**Error-Code:** `").append(errorCode).append("`\n");
        }
        description.append("\n\u26a0\ufe0f **The Mod code has been modified!**\n");
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (serverHash != null && serverHash.length() >= 16) {
            fields.put("\ud83d\udd12 Server Hash (Original)", String.format("```\n%s...\n```", serverHash.substring(0, 32)));
            fields.put("\ud83d\udd13 Client Hash (Modified)", String.format("```\n%s...\n```", clientHash.substring(0, Math.min(32, clientHash.length()))));
        }
        if (playerStats != null) {
            fields.put("\ud83d\udcca Player Info", String.format("Modified Mods Detected: `%d`\nTotal Kicks: `%d`", playerStats.modifiedModsDetected, playerStats.totalKicks));
        }
        this.sendEmbed("modifiedMods", "\ud83d\udd27 Modified mod detected" + (String)(errorCode != null ? " [" + errorCode + "]" : ""), description.toString(), 0xFF6600, fields);
    }

    public void sendModWarning(String playerName, List<WarningEntry> warnings, PlayerStatistics playerStats) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Status:** Warning only - unapproved or unverified mods detected\n");
        description.append("**Count:** `").append(warnings.size()).append("`\n\n");
        description.append(this.formatWarningEntries(warnings));
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (playerStats != null) {
            fields.put("\ud83d\udcca Player Statistics", String.format("Mod warnings: `%d`\nKicks: `%d`", playerStats.illegalModsDetected + playerStats.modifiedModsDetected, playerStats.totalKicks));
        }
        this.sendEmbed("modsWarning", "\u26a0\ufe0f Unwhitelisted mods detected while joining \u26a0\ufe0f", description.toString(), 16753920, fields);
    }

    public void sendPackWarning(String playerName, List<WarningEntry> warnings, PlayerStatistics playerStats, PackWarningContext context) {
        if (warnings == null || warnings.isEmpty()) {
            return;
        }
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Status:** Unwhitelisted or unverified resource packs detected\n");
        description.append("**Count:** `").append(warnings.size()).append("`\n\n");
        description.append(this.formatWarningEntries(warnings));
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (playerStats != null) {
            fields.put("\ud83d\udcca Player Statistics", String.format("Pack warnings: `%d`\nKicks: `%d`", playerStats.illegalResourcePacksDetected, playerStats.totalKicks));
        }
        this.sendEmbed("packsWarning", context.getTitle(), description.toString(), 16753920, fields);
    }

    public void sendPackChangeLog(String playerName, String timestamp, List<PackChangeEntry> added, List<PackChangeEntry> removed) {
        if ((added == null || added.isEmpty()) && (removed == null || removed.isEmpty())) {
            return;
        }
        StringBuilder description = new StringBuilder();
        description.append("**Player:** `").append(playerName).append("`\n");
        description.append("**Timestamp:** `").append(timestamp).append("`\n");
        description.append("**Status:** Resource pack change detected in-game\n");
        LinkedHashMap<String, String> fields = new LinkedHashMap<String, String>();
        if (added != null && !added.isEmpty()) {
            fields.put("+ Added", this.formatPackChangeEntries(added));
        }
        if (removed != null && !removed.isEmpty()) {
            fields.put("- Removed", this.formatPackChangeEntries(removed));
        }
        this.sendEmbed("packChangeLog", "Resource pack changelog", description.toString(), 427980, fields);
    }

    public void sendServerStart() {
        this.sendEmbed("serverStart", "\ud83d\udfe2 Server started", "Anti-Cheat system activated", 65280);
    }

    public void sendServerStop() {
        this.sendEmbed("serverStop", "\ud83d\udd34 Server stopped", "Anti-Cheat system deactivated", 0xFF0000);
    }

    public void sendHashMismatch(String playerName, String errorCode, PlayerStatistics playerStats) {
        String description = "**Player:** `" + playerName + "`\n**Warning:** Anti-Cheat Mod code has been modified!\n" + (String)(errorCode != null ? "**Error-Code:** `" + errorCode + "`" : "");
        if (playerStats != null) {
            description = description + "\n\n" + playerStats.generateDiscordSummary();
        }
        this.sendEmbed("hashMismatch", "\ud83d\udd34 Hash mismatch detected" + (String)(errorCode != null ? " [" + errorCode + "]" : ""), description, 0xFF0000);
    }

    private String formatWarningEntries(List<WarningEntry> warnings) {
        StringBuilder text = new StringBuilder();
        int limit = Math.min(15, warnings.size());
        for (int i = 0; i < limit; ++i) {
            WarningEntry warning = warnings.get(i);
            text.append("\u2022 `").append(warning.name()).append("`\n");
            if (warning.clientHash() != null && !warning.clientHash().isBlank()) {
                text.append("  hash: `").append(DiscordWebhook.previewHash(warning.clientHash())).append("`\n");
            }
            if (warning.expectedHash() == null || warning.expectedHash().isBlank()) continue;
            text.append("  expected: `").append(DiscordWebhook.previewHash(warning.expectedHash())).append("`\n");
        }
        if (warnings.size() > limit) {
            text.append("... and ").append(warnings.size() - limit).append(" more\n");
        }
        return text.toString();
    }

    private String formatPackChangeEntries(List<PackChangeEntry> entries) {
        StringBuilder text = new StringBuilder();
        int limit = Math.min(15, entries.size());
        for (int i = 0; i < limit; ++i) {
            PackChangeEntry entry = entries.get(i);
            text.append("\u2022 `").append(entry.name()).append("`\n");
            if (entry.hash() == null || entry.hash().isBlank()) continue;
            text.append("  hash: `").append(DiscordWebhook.previewHash(entry.hash())).append("`\n");
        }
        if (entries.size() > limit) {
            text.append("... and ").append(entries.size() - limit).append(" more\n");
        }
        return text.toString();
    }

    private static String previewHash(String hash) {
        if (hash == null) {
            return "UNKNOWN";
        }
        return hash.length() <= 32 ? hash : hash.substring(0, 32) + "...";
    }

    private void sendWebhook(String webhookUrl, String jsonPayload) {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "SeiunAC-AntiCheat/1.0");
                connection.setDoOutput(true);
                try (OutputStream os = connection.getOutputStream();){
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    SeiunAC.LOGGER.warn("Discord Webhook failed: HTTP {}", (Object)responseCode);
                }
                connection.disconnect();
            }
            catch (Exception e) {
                SeiunAC.LOGGER.error("Error sending Discord webhook: {}", (Object)e.getMessage());
            }
        }, "Discord-Webhook-Sender");
        thread.setDaemon(true);
        thread.start();
    }

    public static enum PackWarningContext {
        JOINING("\u26a0\ufe0f Unwhitelisted packs detected while joining \u26a0\ufe0f"),
        CHANGING("\u26a0\ufe0f Unwhitelisted packs detected while changing resource pack \u26a0\ufe0f");

        private final String title;

        private PackWarningContext(String title) {
            this.title = title;
        }

        public String getTitle() {
            return this.title;
        }
    }

    public record WarningEntry(String name, String clientHash, String expectedHash) {
    }

    public record PackChangeEntry(String name, String hash) {
    }
}
