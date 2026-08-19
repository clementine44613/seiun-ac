/*
 * Decompiled with CFR 0.152.
 */
package net.kanieoutis.seiunac.client;

public final class ClientVerificationState {
    private static volatile boolean serverSettingsSynced = false;
    private static volatile boolean blockPackChangeEnabled = false;

    private ClientVerificationState() {
    }

    public static void applyServerBlockPackChange(boolean enabled) {
        blockPackChangeEnabled = enabled;
        serverSettingsSynced = true;
    }

    public static void reset() {
        serverSettingsSynced = false;
        blockPackChangeEnabled = false;
    }

    public static boolean hasServerSettings() {
        return serverSettingsSynced;
    }

    public static boolean isBlockPackChangeEnabled() {
        return serverSettingsSynced && blockPackChangeEnabled;
    }
}
