/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 */
package net.kanieoutis.seiunac;

import net.fabricmc.api.ClientModInitializer;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.client.ModChecker;
import net.kanieoutis.seiunac.client.NetworkHandler;
import net.kanieoutis.seiunac.config.ClientConfig;

public class SeiunACClient
implements ClientModInitializer {
    private static ClientConfig clientConfig;

    public void onInitializeClient() {
        SeiunAC.LOGGER.info("=== Anti-Cheat Client is being initialized ===");
        clientConfig = ClientConfig.load();
        ModChecker.initializeCache();
        NetworkHandler.register();
        SeiunAC.LOGGER.info("=== Anti-Cheat Client loaded successfully ===");
    }

    public static ClientConfig getClientConfig() {
        if (clientConfig == null) {
            clientConfig = ClientConfig.load();
        }
        return clientConfig;
    }
}
