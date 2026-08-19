/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.loader.api.FabricLoader
 *  net.fabricmc.loader.api.ModContainer
 */
package net.kanieoutis.seiunac.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.kanieoutis.seiunac.SeiunAC;

public class ModHasher {
    private static final Map<String, String> modHashCache = new ConcurrentHashMap<String, String>();
    private static final Map<Path, String> fileHashCache = new ConcurrentHashMap<Path, String>();

    public static String generateModHash(Path modFile) {
        return ModHasher.generateHash(modFile, true);
    }

    public static String generatePackHash(Path packPath) {
        return ModHasher.generateHash(packPath, false);
    }

    private static String generateHash(Path path, boolean treatDirectoriesAsDevMode) {
        try {
            if (path != null && Files.exists(path, new LinkOption[0])) {
                if (Files.isDirectory(path, new LinkOption[0])) {
                    if (treatDirectoriesAsDevMode) {
                        return "DEV_MODE";
                    }
                    return ModHasher.hashDirectory(path);
                }
                if (!Files.isRegularFile(path, new LinkOption[0])) {
                    return "INVALID";
                }
                if (fileHashCache.containsKey(path)) {
                    return fileHashCache.get(path);
                }
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream fis = Files.newInputStream(path, new OpenOption[0]);){
                    int bytesRead;
                    byte[] buffer = new byte[8192];
                    while ((bytesRead = fis.read(buffer)) != -1) {
                        digest.update(buffer, 0, bytesRead);
                    }
                }
                byte[] hashBytes = digest.digest();
                String hash = ModHasher.bytesToHex(hashBytes);
                fileHashCache.put(path, hash);
                return hash;
            }
            return "NOT_FOUND";
        }
        catch (NoSuchAlgorithmException e) {
            SeiunAC.LOGGER.error("SHA-256 algorithm unavailable!", (Throwable)e);
            return "ERROR";
        }
        catch (IOException var8) {
            return "READ_ERROR";
        }
        catch (Exception var9) {
            return "ERROR";
        }
    }

    private static String hashDirectory(Path directory) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (Stream<Path> paths = Files.walk(directory, new FileVisitOption[0]);){
                paths.filter(x$0 -> Files.isRegularFile(x$0, new LinkOption[0])).sorted(Comparator.comparing(path -> directory.relativize((Path)path).toString())).forEach(path -> {
                    try {
                        Path relativePath = directory.relativize((Path)path);
                        digest.update(relativePath.toString().getBytes(StandardCharsets.UTF_8));
                        try (InputStream inputStream = Files.newInputStream(path, new OpenOption[0]);){
                            int read;
                            byte[] buffer = new byte[8192];
                            while ((read = inputStream.read(buffer)) != -1) {
                                digest.update(buffer, 0, read);
                            }
                        }
                    }
                    catch (IOException iOException) {
                        // empty catch block
                    }
                });
            }
            return ModHasher.bytesToHex(digest.digest());
        }
        catch (NoSuchAlgorithmException e) {
            SeiunAC.LOGGER.error("SHA-256 algorithm unavailable!", (Throwable)e);
            return "ERROR";
        }
        catch (IOException e) {
            return "READ_ERROR";
        }
        catch (Exception e) {
            return "ERROR";
        }
    }

    public static String hashFile(File file) {
        return ModHasher.generateModHash(file.toPath());
    }

    public static Map<String, String> generateAllModHashes() {
        if (!modHashCache.isEmpty()) {
            return modHashCache;
        }
        SeiunAC.LOGGER.info("=== Generating mod hashes ===");
        long startTime = System.currentTimeMillis();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            try {
                String modId = mod.getMetadata().getId();
                Optional<Path> modPath = ModHasher.getModFilePath(mod);
                if (modPath.isPresent()) {
                    String hash = ModHasher.generateModHash(modPath.get());
                    modHashCache.put(modId, hash);
                    continue;
                }
                String version = mod.getMetadata().getVersion().getFriendlyString();
                modHashCache.put(modId, "SYSTEM:" + version);
            }
            catch (Exception var7) {
                String modId = mod.getMetadata().getId();
                modHashCache.put(modId, "ERROR");
            }
        }
        long duration = System.currentTimeMillis() - startTime;
        SeiunAC.LOGGER.info("\u2713 {} mod hashes generated in {}ms", (Object)modHashCache.size(), (Object)duration);
        return modHashCache;
    }

    private static Optional<Path> getModFilePath(ModContainer mod) {
        try {
            return mod.getOrigin().getPaths().stream().filter(path -> {
                try {
                    if (!Files.exists(path, new LinkOption[0])) {
                        return false;
                    }
                    if (Files.isDirectory(path, new LinkOption[0])) {
                        return false;
                    }
                    if (!Files.isRegularFile(path, new LinkOption[0])) {
                        return false;
                    }
                    String fileName = path.getFileName().toString().toLowerCase();
                    return fileName.endsWith(".jar");
                }
                catch (Exception var2) {
                    return false;
                }
            }).findFirst();
        }
        catch (Exception var2) {
            return Optional.empty();
        }
    }

    public static String getModHash(String modId) {
        if (modHashCache.isEmpty()) {
            ModHasher.generateAllModHashes();
        }
        return modHashCache.getOrDefault(modId, "UNKNOWN");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static void clearCache() {
        modHashCache.clear();
        fileHashCache.clear();
        SeiunAC.LOGGER.info("Hash cache cleared");
    }

    public static boolean hashesMatch(String hash1, String hash2) {
        return hash1 != null && hash2 != null ? hash1.equalsIgnoreCase(hash2) : false;
    }
}
