/*
 * Decompiled with CFR 0.152.
 */
package net.kanieoutis.seiunac.util;

public enum AntiCheatErrorCode {
    AC001("AC-001", "Incorrect anti-cheat version"),
    AC002("AC-002", "Anti-cheat mod not installed"),
    AC003("AC-003", "Anti-cheat mod not found"),
    AC004("AC-004", "Anti-cheat mod has been modified"),
    AC005("AC-005", "Anti-cheat client-server hash mismatch"),
    AC006("AC-006", "Incompatible anti-cheat version"),
    AC007("AC-007", "Outdated anti-cheat version"),
    AC008("AC-008", "Anti-cheat client missing"),
    AC101("AC-101", "No response from client (timeout)"),
    AC102("AC-102", "Client is not responding"),
    AC103("AC-103", "Connection timeout"),
    AC104("AC-104", "Hash request timeout"),
    AC105("AC-105", "Client did not send mod list"),
    AC106("AC-106", "Verification timeout"),
    AC201("AC-201", "Illegal mods detected"),
    AC202("AC-202", "Modified mods detected"),
    AC203("AC-203", "Mod hash mismatch"),
    AC204("AC-204", "Unauthorized mods installed"),
    AC205("AC-205", "Multiple illegal mods detected"),
    AC206("AC-206", "Cheat mod detected"),
    AC207("AC-207", "Dangerous mod detected"),
    AC208("AC-208", "Mod whitelist violation"),
    AC209("AC-209", "Too many mods installed"),
    AC210("AC-210", "Mod conflict detected"),
    AC211("AC-211", "Invalid mod file"),
    AC212("AC-212", "Invalid mod signature"),
    AC301("AC-301", "Illegal resource packs detected"),
    AC302("AC-302", "Modified resource packs detected"),
    AC303("AC-303", "Resource pack hash mismatch"),
    AC304("AC-304", "Unauthorized resource packs installed"),
    AC305("AC-305", "Resource pack whitelist violation"),
    AC306("AC-306", "Multiple illegal resource packs detected"),
    AC307("AC-307", "Resource pack has been modified"),
    AC308("AC-308", "Invalid resource pack"),
    AC309("AC-309", "Missing resource pack"),
    AC310("AC-310", "Resource pack changed on server"),
    AC401("AC-401", "Server is reloading"),
    AC402("AC-402", "Server maintenance"),
    AC403("AC-403", "Server is updating whitelist"),
    AC404("AC-404", "Server restart"),
    AC405("AC-405", "Server overloaded"),
    AC406("AC-406", "Server error"),
    AC407("AC-407", "Configuration error"),
    AC501("AC-501", "Network connection failed"),
    AC502("AC-502", "Packet transmission error"),
    AC503("AC-503", "Connection lost"),
    AC504("AC-504", "Invalid network packet"),
    AC505("AC-505", "Payload error"),
    AC506("AC-506", "Client cannot send data"),
    AC507("AC-507", "Server cannot receive data"),
    AC601("AC-601", "Player disconnected during verification"),
    AC602("AC-602", "Invalid player data"),
    AC603("AC-603", "Player authentication failed"),
    AC604("AC-604", "Multiple violations detected"),
    AC605("AC-605", "Player banned"),
    AC606("AC-606", "Too many connection attempts"),
    AC701("AC-701", "Hash calculation failed"),
    AC702("AC-702", "Hash cache error"),
    AC703("AC-703", "Invalid hash value"),
    AC704("AC-704", "Hash algorithm unavailable"),
    AC705("AC-705", "File cannot be hashed"),
    AC706("AC-706", "Hash comparison failed"),
    AC801("AC-801", "Whitelist not loaded"),
    AC802("AC-802", "Whitelist file missing"),
    AC803("AC-803", "Whitelist corrupted"),
    AC804("AC-804", "Whitelist reload failed"),
    AC805("AC-805", "No mods in whitelist"),
    AC901("AC-901", "Critical error"),
    AC902("AC-902", "Internal server error"),
    AC903("AC-903", "Unexpected error"),
    AC999("AC-999", "Unknown error");

    private final String code;
    private final String description;

    private AntiCheatErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }

    public static AntiCheatErrorCode fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String normalized = code.trim();
        for (AntiCheatErrorCode value : AntiCheatErrorCode.values()) {
            if (!value.code.equalsIgnoreCase(normalized)) continue;
            return value;
        }
        return null;
    }

    public String formatKickMessage(String details) {
        StringBuilder message = new StringBuilder();
        message.append("\u00a7c\u00a7lSeiun AC\n\n");
        message.append("\u00a77Connection denied:\n");
        message.append("\u00a7c").append(this.description).append("\n\n");
        if (details != null && !details.isEmpty()) {
            message.append("\u00a77").append(details).append("\n\n");
        }
        message.append("\u00a78Error Code: ").append(this.code);
        return message.toString();
    }

    public String formatKickMessage(String ... detailLines) {
        StringBuilder message = new StringBuilder();
        message.append("\u00a7c\u00a7lSeiun AC\n\n");
        message.append("\u00a77Connection denied:\n");
        message.append("\u00a7c").append(this.description).append("\n\n");
        if (detailLines != null && detailLines.length > 0) {
            for (String line : detailLines) {
                if (line == null || line.isEmpty()) continue;
                message.append("\u00a77").append(line).append("\n");
            }
            message.append("\n");
        }
        message.append("\u00a78Error Code: ").append(this.code);
        return message.toString();
    }
}
