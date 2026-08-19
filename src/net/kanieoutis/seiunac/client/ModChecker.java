/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.fabricmc.loader.api.ModContainer
 *  net.minecraft.class_310
 *  net.minecraft.class_3288
 */
package net.kanieoutis.seiunac.client;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.config.HashWhitelist;
import net.kanieoutis.seiunac.util.ModHasher;
import net.minecraft.class_310;
import net.minecraft.class_3288;

public class ModChecker {
    private static List<String> cachedModHashes = null;
    private static List<String> cachedResourcePacks = null;

    public static void initializeCache() {
        SeiunAC.LOGGER.info("=== Initializing Anti-Cheat Cache ===");
        long startTime = System.currentTimeMillis();
        cachedModHashes = ModChecker.scanModsFolder();
        cachedResourcePacks = ModChecker.scanActiveResourcePacks();
        long duration = System.currentTimeMillis() - startTime;
        SeiunAC.LOGGER.info("\u2713 Cache initialized in {}ms ({} mods, {} resource packs)", new Object[]{duration, cachedModHashes.size(), cachedResourcePacks.size()});
    }

    public static List<String> getInstalledModsWithHashes() {
        return cachedModHashes != null ? new ArrayList<String>(cachedModHashes) : ModChecker.scanModsFolder();
    }

    private static List<String> scanModsFolder() {
        ArrayList<String> mods = new ArrayList<String>();
        try {
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            if (!Files.exists(modsDir, new LinkOption[0]) || !Files.isDirectory(modsDir, new LinkOption[0])) {
                SeiunAC.LOGGER.warn("Mods folder not found: {}", (Object)modsDir);
                return mods;
            }
            SeiunAC.LOGGER.info("Scanning mods/ folder: {}", (Object)modsDir);
            File[] jarFiles = modsDir.toFile().listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                SeiunAC.LOGGER.warn("No JAR files found in mods/ folder");
                return mods;
            }
            for (File jarFile : jarFiles) {
                try {
                    String hash;
                    Object entry;
                    String version;
                    String modId;
                    block22: {
                        modId = "unknown";
                        version = "unknown";
                        try (JarFile jar = new JarFile(jarFile);){
                            entry = jar.getEntry("fabric.mod.json");
                            if (entry == null) break block22;
                            try (InputStream is = jar.getInputStream((ZipEntry)entry);){
                                String jsonContent = new String(is.readAllBytes());
                                modId = ModChecker.extractJsonValue(jsonContent, "id");
                                version = ModChecker.extractJsonValue(jsonContent, "version");
                            }
                        }
                        catch (Exception var18) {
                            SeiunAC.LOGGER.debug("Could not read fabric.mod.json for {}", (Object)jarFile.getName());
                        }
                    }
                    if (modId.equals("unknown")) {
                        modId = jarFile.getName().replace(".jar", "");
                    }
                    if (!((hash = ModHasher.hashFile(jarFile)) == null || hash.isEmpty() || hash.equals("ERROR") || hash.equals("READ_ERROR") || hash.equals("NOT_FOUND") || hash.equals("INVALID"))) {
                        entry = modId + ":" + version + ":" + hash;
                        mods.add((String)entry);
                        SeiunAC.LOGGER.debug("  \u2713 {} v{} [{}]", new Object[]{modId, version, hash.length() >= 16 ? hash.substring(0, 16) + "..." : hash});
                        continue;
                    }
                    SeiunAC.LOGGER.warn("  \u2717 Could not hash the jar file {} : {}", (Object)jarFile.getName(), (Object)hash);
                }
                catch (Exception e) {
                    SeiunAC.LOGGER.error("Error processing jar file {}: {}", (Object)jarFile.getName(), (Object)e.getMessage());
                }
            }
            SeiunAC.LOGGER.info("\u2713 {} JAR files in mods/ folder hashed", (Object)mods.size());
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("Error scanning mods/ folder", (Throwable)e);
        }
        return mods;
    }

    private static String extractJsonValue(String json, String key) {
        try {
            String searchKey = "\"" + key + "\"";
            int keyIndex = json.indexOf(searchKey);
            if (keyIndex == -1) {
                return "unknown";
            }
            int colonIndex = json.indexOf(":", keyIndex);
            if (colonIndex == -1) {
                return "unknown";
            }
            int startQuote = json.indexOf("\"", colonIndex);
            if (startQuote == -1) {
                return "unknown";
            }
            int endQuote = json.indexOf("\"", startQuote + 1);
            return endQuote == -1 ? "unknown" : json.substring(startQuote + 1, endQuote);
        }
        catch (Exception var7) {
            return "unknown";
        }
    }

    public static List<String> getInstalledMods() {
        ArrayList<String> mods = new ArrayList<String>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            String version = mod.getMetadata().getVersion().getFriendlyString();
            mods.add(modId + ":" + version);
        }
        SeiunAC.LOGGER.info("Found {} mods", (Object)mods.size());
        return mods;
    }

    public static List<String> getActiveResourcePacks() {
        return ModChecker.scanActiveResourcePacks();
    }

    private static List<String> scanActiveResourcePacks() {
        ArrayList<String> packs = new ArrayList<String>();
        try {
            class_310 client = class_310.method_1551();
            if (client == null || client.method_1520() == null) {
                SeiunAC.LOGGER.debug("Minecraft client is not available - using resourcepacks folder scan as fallback");
                return ModChecker.scanResourcePacksFolder();
            }
            Collection enabledProfiles = client.method_1520().method_14444();
            if (enabledProfiles == null || enabledProfiles.isEmpty()) {
                SeiunAC.LOGGER.debug("No enabled resource packs found in the client resource pack manager");
                return packs;
            }
            SeiunAC.LOGGER.info("Scanning enabled client resource packs: {}", (Object)enabledProfiles.size());
            for (class_3288 profile : enabledProfiles) {
                try {
                    String packName = profile.method_14463();
                    String hash = ModChecker.resolveResourcePackHash(packName);
                    String stored = HashWhitelist.normalizePackForStorage(packName);
                    packs.add(stored + ":" + hash);
                    SeiunAC.LOGGER.debug("\u2713 Resource pack: {} [{}]", (Object)stored, hash.length() >= 16 ? hash.substring(0, 16) + "..." : hash);
                }
                catch (Exception e) {
                    SeiunAC.LOGGER.error("Error processing resource pack {}: {}", (Object)profile.method_14463(), (Object)e.getMessage());
                }
            }
            SeiunAC.LOGGER.info("\u2713 {} enabled resource packs hashed", (Object)packs.size());
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("Error scanning enabled resource packs", (Throwable)e);
        }
        return packs;
    }

    private static List<String> scanResourcePacksFolder() {
        ArrayList<String> packs = new ArrayList<String>();
        try {
            Path resourcepacksDir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
            if (!Files.exists(resourcepacksDir, new LinkOption[0]) || !Files.isDirectory(resourcepacksDir, new LinkOption[0])) {
                SeiunAC.LOGGER.debug("Resourcepacks folder not found or empty: {}", (Object)resourcepacksDir);
                return packs;
            }
            File[] packFiles = resourcepacksDir.toFile().listFiles();
            if (packFiles == null || packFiles.length == 0) {
                SeiunAC.LOGGER.debug("No resource packs found");
                return packs;
            }
            for (File packFile : packFiles) {
                try {
                    String stored;
                    String hash;
                    String packName;
                    if (packFile.isFile() && packFile.getName().toLowerCase().endsWith(".zip")) {
                        packName = HashWhitelist.stripZipExtension(packFile.getName());
                        hash = ModHasher.generatePackHash(packFile.toPath());
                        if (!HashWhitelist.isValidHash(hash)) continue;
                        stored = HashWhitelist.normalizePackForStorage(packName);
                        packs.add(stored + ":" + hash);
                        continue;
                    }
                    if (!packFile.isDirectory()) continue;
                    packName = packFile.getName();
                    hash = ModHasher.generatePackHash(packFile.toPath());
                    if (!HashWhitelist.isValidHash(hash)) continue;
                    stored = HashWhitelist.normalizePackForStorage(packName);
                    packs.add(stored + ":" + hash);
                }
                catch (Exception e) {
                    SeiunAC.LOGGER.error("Error processing resource pack {}: {}", (Object)packFile.getName(), (Object)e.getMessage());
                }
            }
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("Error scanning resourcepacks/ folder", (Throwable)e);
        }
        return packs;
    }

    public static boolean isPotentiallyDangerous(String modId) {
        String[] dangerousKeywords;
        modId = modId.toLowerCase();
        for (String keyword : dangerousKeywords = new String[]{"cheat", "hack", "xray", "killaura", "fly", "speed", "grief", "exploit", "auto", "bot", "macro", "baritone", "wrust", "meteor"}) {
            if (!modId.contains(keyword)) continue;
            return true;
        }
        return false;
    }

    private static String resolveResourcePackHash(String packName) {
        try {
            Path resourcepacksDir = FabricLoader.getInstance().getGameDir().resolve("resourcepacks");
            if (!Files.exists(resourcepacksDir, new LinkOption[0]) || !Files.isDirectory(resourcepacksDir, new LinkOption[0])) {
                return "UNKNOWN";
            }
            String normalizedPackName = HashWhitelist.normalizePackForStorage(packName);
            File[] candidates = resourcepacksDir.toFile().listFiles();
            if (candidates == null) {
                return "UNKNOWN";
            }
            for (File candidate : candidates) {
                String candidateName = HashWhitelist.normalizePackForStorage(candidate.getName());
                if (!candidateName.equals(normalizedPackName)) continue;
                return ModHasher.generatePackHash(candidate.toPath());
            }
            Path zipCandidate = resourcepacksDir.resolve(normalizedPackName + ".zip");
            if (Files.exists(zipCandidate, new LinkOption[0])) {
                return ModHasher.generatePackHash(zipCandidate);
            }
            Path dirCandidate = resourcepacksDir.resolve(normalizedPackName);
            if (Files.exists(dirCandidate, new LinkOption[0])) {
                return ModHasher.generatePackHash(dirCandidate);
            }
        }
        catch (Exception e) {
            SeiunAC.LOGGER.debug("Could not resolve hash for resource pack {}: {}", (Object)packName, (Object)e.getMessage());
        }
        return "UNKNOWN";
    }

    private static String normalizePackName(String packName) {
        return HashWhitelist.normalizePackForStorage(packName);
    }

    public static String generateModReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Anti-Cheat Mod Report ===\n");
        List<String> mods = ModChecker.getInstalledMods();
        report.append("Installed Mods: ").append(mods.size()).append("\n");
        for (String mod : mods) {
            String modId = mod.split(":")[0];
            if (ModChecker.isPotentiallyDangerous(modId)) {
                report.append(" [!] ").append(mod).append(" (POTENTIALLY DANGEROUS)\n");
                continue;
            }
            report.append(" [\u2713] ").append(mod).append("\n");
        }
        List<String> packs = ModChecker.getActiveResourcePacks();
        report.append("\nActive Resource Packs: ").append(packs.size()).append("\n");
        for (String pack : packs) {
            report.append(" - ").append(pack).append("\n");
        }
        return report.toString();
    }
}
