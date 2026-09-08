package com.elestial.jumpscare.config;

import com.elestial.jumpscare.JumpscareMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JumpscareServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    public static class JumpscareEntry {
        public String imageUrl = "";
        public String soundUrl = "";
        public int durationMs = 2000;
        public float intensity = 1.0f;

        public JumpscareEntry() {}

        public JumpscareEntry(String imageUrl, String soundUrl, int durationMs, float intensity) {
            this.imageUrl = imageUrl;
            this.soundUrl = soundUrl;
            this.durationMs = durationMs;
            this.intensity = intensity;
        }
    }

    private static final Map<String, JumpscareEntry> ENTRIES = new ConcurrentHashMap<>();

    private static File getConfigFile() {
        if (configFile == null) {
            Path configDir = FMLPaths.CONFIGDIR.get();
            configFile = configDir.resolve("jumpscares.json").toFile();
        }
        return configFile;
    }

    public static boolean isAudioUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains(".mp3") || lower.contains(".wav") || lower.contains(".ogg") || lower.contains(".flac") || lower.contains("format=mp3") || lower.contains("audio");
    }

    public static boolean isImageUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains(".png") || lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".gif") || lower.contains(".webp") || lower.contains("format=png") || lower.contains("format=gif");
    }

    public static void loadConfig() {
        ENTRIES.clear();

        File file = getConfigFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, JumpscareEntry>>() {}.getType();
                Map<String, JumpscareEntry> loaded = GSON.fromJson(reader, type);
                if (loaded != null) {
                    for (Map.Entry<String, JumpscareEntry> entry : loaded.entrySet()) {
                        JumpscareEntry val = entry.getValue();
                        if (val != null) {
                            // Auto-fix if URLs were accidentally swapped or misplaced
                            if (isAudioUrl(val.imageUrl) && isImageUrl(val.soundUrl)) {
                                String temp = val.imageUrl;
                                val.imageUrl = val.soundUrl;
                                val.soundUrl = temp;
                            } else if (isAudioUrl(val.imageUrl) && (val.soundUrl == null || val.soundUrl.isEmpty())) {
                                val.soundUrl = val.imageUrl;
                                val.imageUrl = "";
                            } else if (isImageUrl(val.soundUrl) && (val.imageUrl == null || val.imageUrl.isEmpty())) {
                                val.imageUrl = val.soundUrl;
                                val.soundUrl = "";
                            }
                        }
                    }
                    ENTRIES.putAll(loaded);
                }
            } catch (Exception e) {
                JumpscareMod.LOGGER.error("[JumpscareServerConfig] Error reading config file: " + file.getAbsolutePath(), e);
            }
        } else {
            // Default sample entry
            ENTRIES.put("spooky", new JumpscareEntry("", "", 2000, 1.2f));
            saveConfig();
        }

        JumpscareMod.LOGGER.info("[JumpscareServerConfig] Loaded " + ENTRIES.size() + " server jumpscare entries.");
    }

    public static void saveConfig() {
        File file = getConfigFile();
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(ENTRIES, writer);
            }
        } catch (Exception e) {
            JumpscareMod.LOGGER.error("[JumpscareServerConfig] Error saving config file: " + file.getAbsolutePath(), e);
        }
    }

    public static JumpscareEntry getEntry(String id) {
        if (id == null) return null;
        return ENTRIES.get(id.toLowerCase());
    }

    public static void addEntry(String id, String imageUrl, String soundUrl, int durationMs, float intensity) {
        String finalImg = (imageUrl != null) ? imageUrl.trim() : "";
        String finalSound = (soundUrl != null) ? soundUrl.trim() : "";

        // Auto-fix if URLs were swapped or placed in wrong slot
        if (isAudioUrl(finalImg) && isImageUrl(finalSound)) {
            String temp = finalImg;
            finalImg = finalSound;
            finalSound = temp;
        } else if (isAudioUrl(finalImg) && finalSound.isEmpty()) {
            finalSound = finalImg;
            finalImg = "";
        } else if (isImageUrl(finalSound) && finalImg.isEmpty()) {
            finalImg = finalSound;
            finalSound = "";
        }

        ENTRIES.put(id.toLowerCase(), new JumpscareEntry(finalImg, finalSound, durationMs, intensity));
        saveConfig();
    }

    public static boolean removeEntry(String id) {
        if (id == null) return false;
        JumpscareEntry removed = ENTRIES.remove(id.toLowerCase());
        if (removed != null) {
            saveConfig();
            return true;
        }
        return false;
    }

    public static Map<String, JumpscareEntry> getAllEntries() {
        return ENTRIES;
    }
}
