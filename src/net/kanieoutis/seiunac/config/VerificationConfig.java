/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.annotations.SerializedName
 *  net.fabricmc.loader.api.FabricLoader
 */
package net.kanieoutis.seiunac.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.SeiunAC;

public class VerificationConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "SeiunAC-anticheat");
    private static final File VERIFICATION_DIR = new File(CONFIG_DIR, "verification");
    private static final File CONFIG_FILE = new File(VERIFICATION_DIR, "config.json");
    @SerializedName(value="library-bypass")
    public Boolean libraryBypass = Boolean.TRUE;
    @SerializedName(value="block-pack-change")
    public Boolean blockPackChange = Boolean.FALSE;

    public static VerificationConfig load() {
        VerificationConfig.ensureDirectories();
        VerificationConfig config = new VerificationConfig();
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE);){
                VerificationConfig loaded = (VerificationConfig)GSON.fromJson((Reader)reader, VerificationConfig.class);
                if (loaded != null) {
                    config = loaded;
                }
            }
            catch (IOException e) {
                SeiunAC.LOGGER.error("Failed to load verification config: {}", (Object)e.getMessage());
            }
        }
        config.applyDefaults();
        config.save();
        return config;
    }

    public void save() {
        VerificationConfig.ensureDirectories();
        try (FileWriter writer = new FileWriter(CONFIG_FILE);){
            GSON.toJson((Object)this, (Appendable)writer);
        }
        catch (IOException e) {
            SeiunAC.LOGGER.error("Failed to save verification config: {}", (Object)e.getMessage());
        }
    }

    public boolean isLibraryBypassEnabled() {
        return Boolean.TRUE.equals(this.libraryBypass);
    }

    public boolean isBlockPackChangeEnabled() {
        return Boolean.TRUE.equals(this.blockPackChange);
    }

    private void applyDefaults() {
        if (this.libraryBypass == null) {
            this.libraryBypass = Boolean.TRUE;
        }
        if (this.blockPackChange == null) {
            this.blockPackChange = Boolean.FALSE;
        }
    }

    private static void ensureDirectories() {
        if (!CONFIG_DIR.exists()) {
            CONFIG_DIR.mkdirs();
        }
        if (!VERIFICATION_DIR.exists()) {
            VERIFICATION_DIR.mkdirs();
        }
    }
}
