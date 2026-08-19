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
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.fabricmc.loader.api.FabricLoader;
import net.kanieoutis.seiunac.SeiunAC;
import net.kanieoutis.seiunac.config.VerificationConfig;
import net.kanieoutis.seiunac.util.ModHasher;

public class HashWhitelist {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>(){}.getType();
    private static final File CONFIG_DIR = new File(FabricLoader.getInstance().getConfigDir().toFile(), "SeiunAC-anticheat");
    private static final File VERIFICATION_DIR = new File(CONFIG_DIR, "verification");
    private static final File MOD_WHITELIST_FILE = new File(VERIFICATION_DIR, "mod_whitelist.json");
    private static final File MOD_BLACKLIST_FILE = new File(VERIFICATION_DIR, "mod_blacklist.json");
    private static final File PACK_WHITELIST_FILE = new File(VERIFICATION_DIR, "pack_whitelist.json");
    private static final File PACK_BLACKLIST_FILE = new File(VERIFICATION_DIR, "pack_blacklist.json");
    private static final File GRAY_MODS_DIR = new File(VERIFICATION_DIR, "graymods");
    private static final File GRAY_PACKS_DIR = new File(VERIFICATION_DIR, "graypacks");
    private static final File SEIUNAC_HASHES_FILE = new File(VERIFICATION_DIR, "Seiun AC_version_hashes.json");
    private static final Set<String> DEFAULT_MOD_WHITELIST = Set.of("fabricloader", "fabric-api", "fabric-api-base", "fabric-resource-loader-v0", "fabric-networking-api-v1", "java", "minecraft", "SeiunAC", "mixinextras", "sodium", "lithium", "phosphor", "iris", "modmenu", "ferritecore", "immediatelyfast", "cloth-basic-math", "cloth-config", "transition", "trender", "voicechat_api", "io_github_douira_glsl-transformer", "org_anarres_jcpp", "org_antlr_antlr4-runtime", "cardinal-components", "owo-lib", "argonauts", "reach-entity-attributes");
    private static final Set<String> DEFAULT_PACK_WHITELIST = Set.of("vanilla", "file/fabric mods", "programer_art", "programmer_art", "fabric*");
    private final Set<String> modWhitelist = new TreeSet<String>();
    private final Set<String> modBlacklist = new TreeSet<String>();
    private final Map<String, String> grayModHashes = new TreeMap<String, String>();
    private final Set<String> packWhitelist = new TreeSet<String>();
    private final Set<String> packBlacklist = new TreeSet<String>();
    private final Map<String, String> grayPackHashes = new TreeMap<String, String>();
    private final Map<String, String> seiunacVersionHashes = new TreeMap<String, String>();
    private VerificationConfig verificationConfig = new VerificationConfig();

    public void load() {
        SeiunAC.LOGGER.info("=== Loading Verification Lists ===");
        this.modWhitelist.clear();
        this.modBlacklist.clear();
        this.grayModHashes.clear();
        this.packWhitelist.clear();
        this.packBlacklist.clear();
        this.grayPackHashes.clear();
        this.seiunacVersionHashes.clear();
        this.ensureDirectories();
        this.verificationConfig = VerificationConfig.load();
        this.loadStringSet(MOD_WHITELIST_FILE, this.modWhitelist, DEFAULT_MOD_WHITELIST, true);
        this.loadStringSet(MOD_BLACKLIST_FILE, this.modBlacklist, Set.of(), true);
        this.loadPackStringSet(PACK_WHITELIST_FILE, this.packWhitelist, DEFAULT_PACK_WHITELIST, true);
        this.loadPackStringSet(PACK_BLACKLIST_FILE, this.packBlacklist, Set.of(), true);
        this.loadseiunacVersionHashes();
        this.scanGrayModsFolder();
        this.scanGrayPacksFolder();
        SeiunAC.LOGGER.info("\u2713 Verification lists loaded: {} whitelisted mods, {} gray mods, {} blacklisted mods, {} whitelisted packs, {} gray packs, {} blacklisted packs, {} SeiunAC version hashes", new Object[]{this.modWhitelist.size(), this.grayModHashes.size(), this.modBlacklist.size(), this.packWhitelist.size(), this.grayPackHashes.size(), this.packBlacklist.size(), this.seiunacVersionHashes.size()});
        SeiunAC.LOGGER.info("Verification config: library-bypass={}, block-pack-change={}", (Object)this.isLibraryBypassEnabled(), (Object)this.isBlockPackChangeEnabled());
    }

    private void ensureDirectories() {
        if (!CONFIG_DIR.exists() && CONFIG_DIR.mkdirs()) {
            SeiunAC.LOGGER.info("Config directory created: {}", (Object)CONFIG_DIR.getAbsolutePath());
        }
        if (!VERIFICATION_DIR.exists() && VERIFICATION_DIR.mkdirs()) {
            SeiunAC.LOGGER.info("Verification directory created: {}", (Object)VERIFICATION_DIR.getAbsolutePath());
        }
        if (!GRAY_MODS_DIR.exists() && GRAY_MODS_DIR.mkdirs()) {
            SeiunAC.LOGGER.info("graymods directory created: {}", (Object)GRAY_MODS_DIR.getAbsolutePath());
        }
        if (!GRAY_PACKS_DIR.exists() && GRAY_PACKS_DIR.mkdirs()) {
            SeiunAC.LOGGER.info("graypacks directory created: {}", (Object)GRAY_PACKS_DIR.getAbsolutePath());
        }
    }

    private void loadseiunacVersionHashes() {
        Type mapType = new TypeToken<Map<String, String>>(this){}.getType();
        if (!SEIUNAC_HASHES_FILE.exists()) {
            TreeMap<String, String> template = new TreeMap<String, String>();
            template.put("26.2", "");
            template.put("26.1", "");
            template.put("1.21.11", "");
            try (FileWriter writer = new FileWriter(SEIUNAC_HASHES_FILE);){
                GSON.toJson(template, (Appendable)writer);
                SeiunAC.LOGGER.info("Created template {} - please add Seiun AC hashes for each Minecraft version", (Object)SEIUNAC_HASHES_FILE.getName());
            }
            catch (IOException e) {
                SeiunAC.LOGGER.warn("Failed to create template {}: {}", (Object)SEIUNAC_HASHES_FILE.getName(), (Object)e.getMessage());
            }
            return;
        }
        try (FileReader reader = new FileReader(SEIUNAC_HASHES_FILE);){
            Map hashes = (Map)GSON.fromJson((Reader)reader, mapType);
            if (hashes != null) {
                for (Map.Entry entry : hashes.entrySet()) {
                    String version = (String)entry.getKey();
                    String hash = (String)entry.getValue();
                    if (version == null || version.trim().isEmpty() || hash == null || hash.trim().isEmpty()) continue;
                    this.seiunacVersionHashes.put(version.trim(), hash.trim());
                    SeiunAC.LOGGER.debug("\u2713 Loaded SeiunAC hash for MC version {}: [{}...]", (Object)version, (Object)hash.substring(0, Math.min(16, hash.length())));
                }
            }
        }
        catch (Exception e) {
            SeiunAC.LOGGER.warn("Failed to read {}: {}", (Object)SEIUNAC_HASHES_FILE.getName(), (Object)e.getMessage());
        }
    }

    private void loadStringSet(File file, Set<String> target, Collection<String> defaults, boolean writeBack) {
        target.addAll(this.readList(file));
        for (String entry : defaults) {
            target.add(HashWhitelist.normalize(entry));
        }
        if (writeBack || !file.exists()) {
            this.saveList(file, target);
        }
    }

    private void loadPackStringSet(File file, Set<String> target, Collection<String> defaults, boolean writeBack) {
        target.addAll(this.readPackList(file));
        for (String entry : defaults) {
            target.add(HashWhitelist.normalizePackName(entry));
        }
        if (writeBack || !file.exists()) {
            this.saveList(file, target);
        }
    }

    private Set<String> readList(File file) {
        TreeSet<String> values = new TreeSet<String>();
        if (!file.exists()) {
            return values;
        }
        try (FileReader reader = new FileReader(file);){
            List entries = (List)GSON.fromJson((Reader)reader, STRING_LIST_TYPE);
            if (entries != null) {
                for (String entry : entries) {
                    if (entry == null || entry.trim().isEmpty()) continue;
                    values.add(HashWhitelist.normalize(entry));
                }
            }
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("Failed to read {}: {}", (Object)file.getName(), (Object)e.getMessage());
        }
        return values;
    }

    private Set<String> readPackList(File file) {
        TreeSet<String> values = new TreeSet<String>();
        if (!file.exists()) {
            return values;
        }
        try (FileReader reader = new FileReader(file);){
            List entries = (List)GSON.fromJson((Reader)reader, STRING_LIST_TYPE);
            if (entries != null) {
                for (String entry : entries) {
                    if (entry == null || entry.trim().isEmpty()) continue;
                    values.add(HashWhitelist.normalizePackRule(entry));
                }
            }
        }
        catch (Exception e) {
            SeiunAC.LOGGER.error("Failed to read {}: {}", (Object)file.getName(), (Object)e.getMessage());
        }
        return values;
    }

    private void saveList(File file, Collection<String> entries) {
        try (FileWriter writer = new FileWriter(file);){
            GSON.toJson(new ArrayList<String>(entries), (Appendable)writer);
        }
        catch (IOException e) {
            SeiunAC.LOGGER.error("Failed to save {}: {}", (Object)file.getName(), (Object)e.getMessage());
        }
    }

    private void scanGrayModsFolder() {
        File[] jarFiles = GRAY_MODS_DIR.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            SeiunAC.LOGGER.info("No JAR files found in graymods/");
            return;
        }
        SeiunAC.LOGGER.info("Scanning {} JAR files in graymods/", (Object)jarFiles.length);
        for (File jarFile : jarFiles) {
            try {
                String modId = this.extractModIdFromJar(jarFile);
                String hash = ModHasher.generateModHash(jarFile.toPath());
                if (HashWhitelist.isValidHash(hash)) {
                    this.grayModHashes.put(modId, hash);
                    SeiunAC.LOGGER.info("  \u2713 gray mod {} [{}...]", (Object)modId, (Object)HashWhitelist.preview(hash));
                    continue;
                }
                SeiunAC.LOGGER.error("  \u2717 Hash error for gray mod {}: {}", (Object)jarFile.getName(), (Object)hash);
            }
            catch (Exception e) {
                SeiunAC.LOGGER.error("  \u2717 Error processing gray mod {}: {}", (Object)jarFile.getName(), (Object)e.getMessage());
            }
        }
    }

    private void scanGrayPacksFolder() {
        File[] packFiles = GRAY_PACKS_DIR.listFiles((dir, name) -> {
            File candidate = new File(dir, name);
            return candidate.isDirectory() || name.toLowerCase().endsWith(".zip");
        });
        if (packFiles == null || packFiles.length == 0) {
            SeiunAC.LOGGER.info("No ZIP files found in graypacks/");
            return;
        }
        SeiunAC.LOGGER.info("Scanning {} ZIP files in graypacks/", (Object)packFiles.length);
        for (File packFile : packFiles) {
            try {
                String packName = HashWhitelist.stripZipExtension(packFile.getName());
                String hash = ModHasher.generatePackHash(packFile.toPath());
                if (HashWhitelist.isValidHash(hash)) {
                    String stored = HashWhitelist.normalizePackForStorage(packName);
                    this.grayPackHashes.put(stored, hash);
                    SeiunAC.LOGGER.info("  \u2713 gray pack {} [{}...]", (Object)stored, (Object)HashWhitelist.preview(hash));
                    continue;
                }
                SeiunAC.LOGGER.error("  \u2717 Hash error for gray pack {}: {}", (Object)packFile.getName(), (Object)hash);
            }
            catch (Exception e) {
                SeiunAC.LOGGER.error("  \u2717 Error processing gray pack {}: {}", (Object)packFile.getName(), (Object)e.getMessage());
            }
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private String extractModIdFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile);){
            String string;
            block19: {
                String modId;
                InputStream is;
                block17: {
                    String string2;
                    block18: {
                        ZipEntry entry = jar.getEntry("fabric.mod.json");
                        if (entry == null) {
                            String string3 = HashWhitelist.normalize(HashWhitelist.stripJarExtension(jarFile.getName()));
                            return string3;
                        }
                        is = jar.getInputStream(entry);
                        try {
                            String json = new String(is.readAllBytes());
                            modId = this.extractJsonValue(json, "id");
                            if (!"unknown".equalsIgnoreCase(modId)) break block17;
                            string2 = HashWhitelist.normalize(HashWhitelist.stripJarExtension(jarFile.getName()));
                            if (is == null) break block18;
                        }
                        catch (Throwable throwable) {
                            if (is != null) {
                                try {
                                    is.close();
                                }
                                catch (Throwable throwable2) {
                                    throwable.addSuppressed(throwable2);
                                }
                            }
                            throw throwable;
                        }
                        is.close();
                    }
                    return string2;
                }
                string = HashWhitelist.normalize(modId);
                if (is == null) break block19;
                is.close();
            }
            return string;
        }
        catch (Exception e) {
            return HashWhitelist.normalize(HashWhitelist.stripJarExtension(jarFile.getName()));
        }
    }

    private String extractJsonValue(String json, String key) {
        try {
            int keyIndex = json.indexOf("\"" + key + "\"");
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
            if (endQuote == -1) {
                return "unknown";
            }
            return json.substring(startQuote + 1, endQuote);
        }
        catch (Exception e) {
            return "unknown";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    public static String normalizePackForStorage(String value) {
        int idx;
        int lastSlash;
        if (value == null) {
            return "";
        }
        String s = value.trim().toLowerCase(Locale.ROOT).replace('\\', '/');
        if (s.startsWith("file:")) {
            s = s.substring("file:".length());
        }
        if (s.startsWith("file/") && !s.equals("file/fabric mods")) {
            s = s.substring("file/".length());
        }
        if ((lastSlash = s.lastIndexOf(47)) >= 0 && lastSlash + 1 < s.length()) {
            s = s.substring(lastSlash + 1);
        }
        if ((s.endsWith(".zip") || s.endsWith(".jar") || s.endsWith(".mrpack") || s.endsWith(".mcpack")) && (idx = s.lastIndexOf(46)) > 0) {
            s = s.substring(0, idx);
        }
        s = s.replaceAll("\\u00A7.", "");
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.replaceAll("[^a-z0-9]", "");
        return s;
    }

    public static String normalizePackRule(String rule) {
        if (rule == null) {
            return "";
        }
        String trimmed = rule.trim();
        boolean hasWildcard = trimmed.endsWith("*");
        String base = hasWildcard ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        String normalizedBase = HashWhitelist.normalizePackForStorage(base);
        return hasWildcard ? normalizedBase + "*" : normalizedBase;
    }

    private static String normalizePackName(String value) {
        return HashWhitelist.normalizePackForStorage(value);
    }

    private static String preview(String hash) {
        return hash.substring(0, Math.min(16, hash.length()));
    }

    public static boolean isValidHash(String hash) {
        return hash != null && !hash.isEmpty() && !hash.equals("ERROR") && !hash.equals("READ_ERROR") && !hash.equals("NOT_FOUND") && !hash.equals("INVALID") && !hash.equals("DEV_MODE");
    }

    private static String stripJarExtension(String name) {
        return name.toLowerCase().endsWith(".jar") ? name.substring(0, name.length() - 4) : name;
    }

    public static String stripZipExtension(String name) {
        return name.toLowerCase().endsWith(".zip") ? name.substring(0, name.length() - 4) : name;
    }

    private static boolean matchesPackRule(String packName, String rule) {
        if (rule.endsWith("*")) {
            return packName.startsWith(rule.substring(0, rule.length() - 1));
        }
        return packName.equals(rule);
    }

    public boolean isModWhitelisted(String modId) {
        if (this.modWhitelist.contains(HashWhitelist.normalize(modId))) {
            return true;
        }
        return this.isLibraryBypassEnabled() && HashWhitelist.isLibraryBypassMod(modId);
    }

    public boolean isModBlacklisted(String modId) {
        return this.modBlacklist.contains(HashWhitelist.normalize(modId));
    }

    public boolean isModGraylisted(String modId) {
        return this.grayModHashes.containsKey(HashWhitelist.normalize(modId));
    }

    public String getGrayModHash(String modId) {
        return this.grayModHashes.get(HashWhitelist.normalize(modId));
    }

    public boolean isPackWhitelisted(String packName) {
        String normalizedPack = HashWhitelist.normalizePackName(packName);
        for (String rule : this.packWhitelist) {
            if (!HashWhitelist.matchesPackRule(normalizedPack, rule)) continue;
            return true;
        }
        return false;
    }

    public boolean isPackBlacklisted(String packName) {
        String normalizedPack = HashWhitelist.normalizePackName(packName);
        for (String rule : this.packBlacklist) {
            if (!HashWhitelist.matchesPackRule(normalizedPack, rule)) continue;
            return true;
        }
        return false;
    }

    public boolean isPackGraylisted(String packName) {
        return this.grayPackHashes.containsKey(HashWhitelist.normalizePackName(packName));
    }

    public String getGrayPackHash(String packName) {
        return this.grayPackHashes.get(HashWhitelist.normalizePackName(packName));
    }

    public Set<String> getModWhitelistIds() {
        return new TreeSet<String>(this.modWhitelist);
    }

    public Set<String> getModBlacklistIds() {
        return new TreeSet<String>(this.modBlacklist);
    }

    public Map<String, String> getGrayModHashes() {
        return new TreeMap<String, String>(this.grayModHashes);
    }

    public Set<String> getPackWhitelistNames() {
        return new TreeSet<String>(this.packWhitelist);
    }

    public Set<String> getPackBlacklistNames() {
        return new TreeSet<String>(this.packBlacklist);
    }

    public Map<String, String> getGrayPackHashes() {
        return new TreeMap<String, String>(this.grayPackHashes);
    }

    public String getSeiunACHashForVersion(String minecraftVersion) {
        if (minecraftVersion == null || minecraftVersion.trim().isEmpty()) {
            return null;
        }
        return this.seiunacVersionHashes.get(minecraftVersion.trim());
    }

    public Map<String, String> getAllseiunacVersionHashes() {
        return new TreeMap<String, String>(this.seiunacVersionHashes);
    }

    public boolean isModHashAllowed(String modId, String clientHash) {
        if (this.isModBlacklisted(modId)) {
            return false;
        }
        if (this.isModWhitelisted(modId)) {
            return true;
        }
        String grayHash = this.getGrayModHash(modId);
        return grayHash != null && grayHash.equalsIgnoreCase(clientHash);
    }

    public String getServerHash(String modId) {
        return this.getGrayModHash(modId);
    }

    public Set<String> getAllowedModIds() {
        TreeSet<String> ids = new TreeSet<String>(this.modWhitelist);
        ids.addAll(this.grayModHashes.keySet());
        return ids;
    }

    public Map<String, String> getAllowedModHashes() {
        TreeMap<String, String> hashes = new TreeMap<String, String>();
        for (String modId : this.modWhitelist) {
            hashes.put(modId, "WHITELIST");
        }
        hashes.putAll(this.grayModHashes);
        return hashes;
    }

    public boolean isResourcePackAllowed(String packName) {
        return this.isPackWhitelisted(packName) || this.isPackGraylisted(packName);
    }

    public boolean isResourcePackHashAllowed(String packName, String clientHash) {
        String grayHash = this.getGrayPackHash(packName);
        return grayHash != null && grayHash.equalsIgnoreCase(clientHash);
    }

    public String getResourcePackHash(String packName) {
        return this.getGrayPackHash(packName);
    }

    public Set<String> getAllowedResourcePackNames() {
        TreeSet<String> names = new TreeSet<String>(this.packWhitelist);
        names.addAll(this.grayPackHashes.keySet());
        return names;
    }

    public Map<String, String> getAllowedResourcePackHashes() {
        TreeMap<String, String> hashes = new TreeMap<String, String>();
        for (String packName : this.packWhitelist) {
            hashes.put(packName, "WHITELIST");
        }
        hashes.putAll(this.grayPackHashes);
        return hashes;
    }

    public boolean isLibraryBypassEnabled() {
        return this.verificationConfig != null && this.verificationConfig.isLibraryBypassEnabled();
    }

    public boolean isBlockPackChangeEnabled() {
        return this.verificationConfig != null && this.verificationConfig.isBlockPackChangeEnabled();
    }

    private static boolean isLibraryBypassMod(String modId) {
        String[] knownLibraries;
        if ((modId = HashWhitelist.normalize(modId)).isEmpty()) {
            return false;
        }
        if (modId.startsWith("fabric") || modId.equals("fabricloader") || modId.equals("mixinextras")) {
            return true;
        }
        if (modId.equals("minecraft") || modId.equals("java")) {
            return true;
        }
        if (modId.startsWith("org_") || modId.startsWith("io_") || modId.startsWith("com_") || modId.startsWith("net_")) {
            return true;
        }
        if (modId.contains("_api") || modId.contains("-api") || modId.endsWith("api") || modId.endsWith("lib") || modId.endsWith("libs")) {
            return true;
        }
        for (String lib : knownLibraries = new String[]{"cloth-basic-math", "cloth-config", "transition", "trender", "voicechat_api", "io_github_douira_glsl-transformer", "org_anarres_jcpp", "org_antlr_antlr4-runtime", "cardinal-components", "owo-lib", "argonauts", "reach-entity-attributes", "ferritecore", "immediatelyfast"}) {
            if (!modId.equals(lib) && !modId.replace("-", "_").equals(lib.replace("-", "_"))) continue;
            return true;
        }
        return false;
    }
}
