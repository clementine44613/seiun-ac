/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.reflect.TypeToken
 *  net.fabricmc.loader.api.FabricLoader
 */
package net.kanieoutis.seiunac.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.config.HashWhitelist;

public class ModWhitelist {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
    private static final File WHITELIST_FILE = new File(CONFIG_DIR, "mod_whitelist_legacy.json");
    private Set<String> allowedMods = new HashSet<String>();
    private Set<String> allowedResourcePacks = new HashSet<String>();
    private boolean strictMode = true;

    public ModWhitelist() {
        this.addDefaultMods();
    }

    private void addDefaultMods() {
        this.allowedMods.add("fabricloader");
        this.allowedMods.add("fabric-api");
        this.allowedMods.add("fabric-api-base");
        this.allowedMods.add("fabric-resource-loader-v0");
        this.allowedMods.add("fabric-networking-api-v1");
        this.allowedMods.add("java");
        this.allowedMods.add("minecraft");
        this.allowedMods.add("SeiunAC");
        this.allowedMods.add("sodium");
        this.allowedMods.add("lithium");
        this.allowedMods.add("phosphor");
        this.allowedMods.add("iris");
        this.allowedMods.add("modmenu");
    }

    public void load() {
        if (!WHITELIST_FILE.exists()) {
            this.save();
        } else {
            try (FileReader reader = new FileReader(WHITELIST_FILE);){
                Type type = new TypeToken<WhitelistConfig>(this){}.getType();
                WhitelistConfig config = (WhitelistConfig)GSON.fromJson((Reader)reader, type);
                if (config != null) {
                    this.allowedMods = new HashSet<String>(config.allowedMods);
                    this.allowedResourcePacks = new HashSet<String>();
                    for (String entry : config.allowedResourcePacks) {
                        this.allowedResourcePacks.add(HashWhitelist.normalizePackRule(entry));
                    }
                    this.strictMode = config.strictMode;
                    this.addDefaultMods();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void save() {
        try {
            CONFIG_DIR.mkdirs();
            try (FileWriter writer = new FileWriter(WHITELIST_FILE);){
                WhitelistConfig config = new WhitelistConfig();
                config.allowedMods = new ArrayList<String>(this.allowedMods);
                config.allowedResourcePacks = new ArrayList<String>(this.allowedResourcePacks);
                config.strictMode = this.strictMode;
                GSON.toJson((Object)config, (Appendable)writer);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isModAllowed(String modId) {
        return this.allowedMods.contains(modId.toLowerCase());
    }

    public boolean isResourcePackAllowed(String packName) {
        if (packName == null) {
            return false;
        }
        String normalized = HashWhitelist.normalizePackForStorage(packName);
        if (this.allowedResourcePacks.contains(normalized)) {
            return true;
        }
        for (String rule : this.allowedResourcePacks) {
            String prefix;
            if (!rule.endsWith("*") || !normalized.startsWith(prefix = rule.substring(0, rule.length() - 1))) continue;
            return true;
        }
        return false;
    }

    public void addMod(String modId) {
        this.allowedMods.add(modId.toLowerCase());
        this.save();
    }

    public void removeMod(String modId) {
        this.allowedMods.remove(modId.toLowerCase());
        this.save();
    }

    public void addResourcePack(String packName) {
        if (packName == null) {
            return;
        }
        this.allowedResourcePacks.add(HashWhitelist.normalizePackRule(packName));
        this.save();
    }

    public void removeResourcePack(String packName) {
        if (packName == null) {
            return;
        }
        this.allowedResourcePacks.remove(HashWhitelist.normalizePackRule(packName));
        this.save();
    }

    public Set<String> getAllowedMods() {
        return new HashSet<String>(this.allowedMods);
    }

    public Set<String> getAllowedResourcePacks() {
        return new HashSet<String>(this.allowedResourcePacks);
    }

    public boolean isStrictMode() {
        return this.strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
        this.save();
    }

    private static class WhitelistConfig {
        List<String> allowedMods = new ArrayList<String>();
        List<String> allowedResourcePacks = new ArrayList<String>();
        boolean strictMode = true;

        private WhitelistConfig() {
        }
    }
}
