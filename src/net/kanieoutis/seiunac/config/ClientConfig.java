/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.loader.api.FabricLoader
 */
package net.kanieoutis.seiunac.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.SeiunAC;

public class ClientConfig {
    private static final File CONFIG_DIR = FabricLoader.getInstance().getConfigDir().toFile();
    private static final File ANTICHEAT_DIR = new File(CONFIG_DIR, "SeiunAC-anticheat");
    private static final File CONFIG_FILE = new File(ANTICHEAT_DIR, "client-config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public String serverAddress = "localhost";
    public int serverPort = 25565;

    public static ClientConfig load() {
        if (!ANTICHEAT_DIR.exists() && !ANTICHEAT_DIR.mkdirs()) {
            SeiunAC.LOGGER.warn("Could not create config directory: {}", (Object)ANTICHEAT_DIR);
        }
        if (CONFIG_FILE.exists()) {
            ClientConfig clientConfig;
            FileReader reader = new FileReader(CONFIG_FILE);
            try {
                ClientConfig config = (ClientConfig)GSON.fromJson((Reader)reader, ClientConfig.class);
                SeiunAC.LOGGER.info("Client config loaded: {}:{}", (Object)config.serverAddress, (Object)config.serverPort);
                clientConfig = config;
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
                    SeiunAC.LOGGER.error("Failed to load client config: {}", (Object)e.getMessage());
                }
            }
            reader.close();
            return clientConfig;
        }
        ClientConfig config = new ClientConfig();
        config.save();
        SeiunAC.LOGGER.info("Created new client config with default values");
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE);){
            GSON.toJson((Object)this, (Appendable)writer);
            SeiunAC.LOGGER.info("Client config saved");
        }
        catch (IOException e) {
            SeiunAC.LOGGER.error("Failed to save client config: {}", (Object)e.getMessage());
        }
    }

    public String getFullAddress() {
        return this.serverPort == 25565 ? this.serverAddress : this.serverAddress + ":" + this.serverPort;
    }
}
