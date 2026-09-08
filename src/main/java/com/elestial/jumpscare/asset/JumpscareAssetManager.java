package com.elestial.jumpscare.asset;

import com.elestial.jumpscare.JumpscareMod;
import com.elestial.jumpscare.client.JumpscareOverlayRenderer;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class JumpscareAssetManager {
    private static JumpscareAssetManager INSTANCE;

    private final Map<String, ResourceLocation> loadedTextures = new ConcurrentHashMap<>();
    private final Map<String, File> loadedSounds = new ConcurrentHashMap<>();
    private final Map<String, Long> failedTextures = new ConcurrentHashMap<>();
    private final Map<String, Long> failedSounds = new ConcurrentHashMap<>();
    private File gameDir;
    private File jumpscareDir;
    private File cacheDir;

    public static JumpscareAssetManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JumpscareAssetManager();
        }
        return INSTANCE;
    }

    public void init() {
        gameDir = FMLPaths.GAMEDIR.get().toFile();
        jumpscareDir = new File(gameDir, "jumpscares");
        cacheDir = new File(gameDir, "jumpscares_cache");

        if (!jumpscareDir.exists()) jumpscareDir.mkdirs();
        if (!cacheDir.exists()) cacheDir.mkdirs();

        createTutorialFile();
        ensureDefaultJumpscareExists();
        reloadAssets();
    }

    public void openAssetFolder() {
        if (jumpscareDir != null && jumpscareDir.exists()) {
            Util.getPlatform().openFile(jumpscareDir);
        }
    }

    public void reloadAssets() {
        JumpscareMod.LOGGER.info("[JumpscareAssetManager] Scanning custom jumpscares...");
        loadedTextures.clear();
        loadedSounds.clear();
        failedTextures.clear();
        failedSounds.clear();

        scanDirectory(jumpscareDir);
        scanDirectory(cacheDir);

        JumpscareMod.LOGGER.info("[JumpscareAssetManager] Loaded " + loadedTextures.size() + " texture(s) & " + loadedSounds.size() + " sound(s).");
    }

    private void scanDirectory(File parentDir) {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) return;

        File[] files = parentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String id = file.getName().toLowerCase(Locale.ROOT);
                File[] contents = file.listFiles();
                if (contents != null) {
                    for (File c : contents) {
                        processFile(id, c);
                    }
                }
            } else {
                String fileName = file.getName();
                int lastDot = fileName.lastIndexOf('.');
                if (lastDot > 0) {
                    String id = fileName.substring(0, lastDot).toLowerCase(Locale.ROOT);
                    processFile(id, file);
                }
            }
        }
    }

    private void processFile(String id, File file) {
        String cleanId = id.toLowerCase(Locale.ROOT);
        if (cleanId.endsWith("_remote")) {
            cleanId = cleanId.substring(0, cleanId.length() - 7);
        }

        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp")) {
            loadDynamicTexture(cleanId, file);
        } else if (name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".mp3") || name.endsWith(".flac")) {
            loadedSounds.put(cleanId, file);
        }
    }

    public static boolean isValidImageFile(File file) {
        if (file == null || !file.exists() || !file.isFile() || file.length() < 12) {
            return false;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] header = new byte[12];
            int read = in.read(header);
            if (read < 12) return false;

            // PNG: 89 50 4E 47
            if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) return true;
            // JPEG: FF D8 FF
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) return true;
            // GIF: GIF8
            if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') return true;
            // WebP: RIFF .... WEBP
            if (header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') return true;
            // BMP: BM
            if (header[0] == 0x42 && header[1] == 0x4D) return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isAudioFileOrHeader(File file) {
        if (file == null || !file.exists() || !file.isFile() || file.length() < 4) {
            return false;
        }
        try (FileInputStream in = new FileInputStream(file)) {
            byte[] header = new byte[12];
            int read = in.read(header);
            if (read < 4) return false;

            // ID3 (MP3)
            if (header[0] == 'I' && header[1] == 'D' && header[2] == '3') return true;
            // MPEG audio frame (FF E0+)
            if ((header[0] & 0xFF) == 0xFF && ((header[1] & 0xE0) == 0xE0)) return true;
            // OggS
            if (header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S') return true;
            // RIFF .... WAVE
            if (read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'A' && header[10] == 'V' && header[11] == 'E') return true;
        } catch (Exception ignored) {}
        return false;
    }

    public static boolean isAudioUrl(String url) {
        if (url == null) return false;
        String clean = url.split("\\?")[0].toLowerCase(Locale.ROOT);
        return clean.endsWith(".mp3") || clean.endsWith(".ogg") || clean.endsWith(".wav") || clean.endsWith(".flac") || clean.contains("audio") || clean.contains("format=mp3");
    }

    public static boolean isImageUrl(String url) {
        if (url == null) return false;
        String clean = url.split("\\?")[0].toLowerCase(Locale.ROOT);
        return clean.endsWith(".png") || clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".webp") || clean.endsWith(".gif") || clean.contains("image") || clean.contains("format=png");
    }

    public void loadDynamicTexture(String id, File imageFile) {
        String cleanId = id.toLowerCase(Locale.ROOT);
        if (imageFile == null || !imageFile.exists() || imageFile.length() < 12) {
            failedTextures.put(cleanId, System.currentTimeMillis() + 10000L);
            return;
        }

        // Validate image magic bytes before attempting NativeImage decode
        if (!isValidImageFile(imageFile)) {
            if (isAudioFileOrHeader(imageFile)) {
                JumpscareMod.LOGGER.warn("[JumpscareAssetManager] Detected audio data inside image file (" + imageFile.getName() + "). Rescuing as sound...");
                File targetAudio = new File(imageFile.getParentFile(), cleanId + "_remote.mp3");
                if (!targetAudio.exists()) {
                    imageFile.renameTo(targetAudio);
                } else {
                    imageFile.delete();
                }
                if (targetAudio.exists()) {
                    loadedSounds.put(cleanId, targetAudio);
                }
            } else {
                JumpscareMod.LOGGER.warn("[JumpscareAssetManager] Deleting invalid/corrupt image file: " + imageFile.getAbsolutePath());
                try {
                    imageFile.delete();
                } catch (Exception ignored) {}
            }
            failedTextures.put(cleanId, System.currentTimeMillis() + 15000L);
            return;
        }

        try {
            NativeImage nativeImage;
            try (InputStream stream = new FileInputStream(imageFile)) {
                nativeImage = NativeImage.read(stream);
            }

            ResourceLocation textureId = new ResourceLocation(JumpscareMod.MOD_ID, "dynamic_" + cleanId);

            Runnable registerTask = () -> {
                try {
                    DynamicTexture texture = new DynamicTexture(nativeImage);
                    Minecraft.getInstance().getTextureManager().register(textureId, texture);
                    loadedTextures.put(cleanId, textureId);
                    failedTextures.remove(cleanId);
                    JumpscareMod.LOGGER.info("[JumpscareAssetManager] Successfully loaded GPU texture for ID: " + cleanId);
                } catch (Exception e) {
                    JumpscareMod.LOGGER.error("[JumpscareAssetManager] Error registering GPU texture: " + cleanId, e);
                    failedTextures.put(cleanId, System.currentTimeMillis() + 10000L);
                }
            };

            if (Minecraft.getInstance().isSameThread()) {
                registerTask.run();
            } else {
                Minecraft.getInstance().execute(registerTask);
            }
        } catch (Throwable e) {
            JumpscareMod.LOGGER.error("[JumpscareAssetManager] Failed to read texture file (corrupt cache deleted): " + imageFile.getAbsolutePath() + " - " + e.getMessage());
            try {
                imageFile.delete();
            } catch (Exception ignored) {}
            failedTextures.put(cleanId, System.currentTimeMillis() + 15000L);
        }
    }

    public void fetchAndTrigger(String id, String rawImageUrl, String rawSoundUrl, int durationMs, float intensity) {
        new Thread(() -> {
            String cleanId = id.toLowerCase(Locale.ROOT);
            String imageUrl = rawImageUrl;
            String soundUrl = rawSoundUrl;

            // Auto-swap if inputs were accidentally swapped or misplaced
            if (isAudioUrl(imageUrl) && (soundUrl == null || soundUrl.isEmpty() || isImageUrl(soundUrl))) {
                String temp = imageUrl;
                imageUrl = (soundUrl != null && isImageUrl(soundUrl)) ? soundUrl : "";
                soundUrl = temp;
            } else if (isAudioUrl(imageUrl) && (soundUrl == null || soundUrl.isEmpty())) {
                soundUrl = imageUrl;
                imageUrl = "";
            } else if (isImageUrl(soundUrl) && (imageUrl == null || imageUrl.isEmpty())) {
                imageUrl = soundUrl;
                soundUrl = "";
            }

            if (imageUrl != null && !imageUrl.isEmpty() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                String imgExt = ".png";
                String lower = imageUrl.toLowerCase(Locale.ROOT);
                if (lower.contains(".jpg") || lower.contains(".jpeg")) imgExt = ".jpg";
                else if (lower.contains(".gif")) imgExt = ".gif";
                else if (lower.contains(".webp")) imgExt = ".webp";

                File targetImgFile = new File(cacheDir, cleanId + "_remote" + imgExt);
                if (!targetImgFile.exists() || targetImgFile.length() == 0 || !isValidImageFile(targetImgFile)) {
                    downloadFile(imageUrl, targetImgFile);
                }
                if (targetImgFile.exists() && isValidImageFile(targetImgFile)) {
                    loadDynamicTexture(cleanId, targetImgFile);
                }
            }

            if (soundUrl != null && !soundUrl.isEmpty() && (soundUrl.startsWith("http://") || soundUrl.startsWith("https://"))) {
                String ext = ".mp3";
                String lowerUrl = soundUrl.toLowerCase(Locale.ROOT);
                if (lowerUrl.contains(".wav")) ext = ".wav";
                else if (lowerUrl.contains(".ogg")) ext = ".ogg";
                else if (lowerUrl.contains(".flac")) ext = ".flac";

                File targetSoundFile = new File(cacheDir, cleanId + "_remote" + ext);
                if (!targetSoundFile.exists() || targetSoundFile.length() == 0) {
                    downloadFile(soundUrl, targetSoundFile);
                }
                if (targetSoundFile.exists() && targetSoundFile.length() > 0) {
                    loadedSounds.put(cleanId, targetSoundFile);
                }
            }

            Minecraft.getInstance().execute(() -> {
                JumpscareOverlayRenderer.getInstance().triggerJumpscare(cleanId, durationMs, intensity);
            });
        }, "JumpscareFetchThread").start();
    }

    public void preloadAsset(String id, String rawImageUrl, String rawSoundUrl) {
        new Thread(() -> {
            String cleanId = id.toLowerCase(Locale.ROOT);
            String imageUrl = rawImageUrl;
            String soundUrl = rawSoundUrl;

            // Auto-swap if inputs were accidentally swapped or misplaced
            if (isAudioUrl(imageUrl) && (soundUrl == null || soundUrl.isEmpty() || isImageUrl(soundUrl))) {
                String temp = imageUrl;
                imageUrl = (soundUrl != null && isImageUrl(soundUrl)) ? soundUrl : "";
                soundUrl = temp;
            } else if (isAudioUrl(imageUrl) && (soundUrl == null || soundUrl.isEmpty())) {
                soundUrl = imageUrl;
                imageUrl = "";
            } else if (isImageUrl(soundUrl) && (imageUrl == null || imageUrl.isEmpty())) {
                imageUrl = soundUrl;
                soundUrl = "";
            }

            if (imageUrl != null && !imageUrl.isEmpty() && (imageUrl.startsWith("http://") || imageUrl.startsWith("https://"))) {
                String imgExt = ".png";
                String lower = imageUrl.toLowerCase(Locale.ROOT);
                if (lower.contains(".jpg") || lower.contains(".jpeg")) imgExt = ".jpg";
                else if (lower.contains(".gif")) imgExt = ".gif";
                else if (lower.contains(".webp")) imgExt = ".webp";

                File targetImgFile = new File(cacheDir, cleanId + "_remote" + imgExt);
                if (!targetImgFile.exists() || targetImgFile.length() == 0 || !isValidImageFile(targetImgFile)) {
                    downloadFile(imageUrl, targetImgFile);
                }
                if (targetImgFile.exists() && isValidImageFile(targetImgFile)) {
                    loadDynamicTexture(cleanId, targetImgFile);
                }
            }

            if (soundUrl != null && !soundUrl.isEmpty() && (soundUrl.startsWith("http://") || soundUrl.startsWith("https://"))) {
                String ext = ".mp3";
                String lowerUrl = soundUrl.toLowerCase(Locale.ROOT);
                if (lowerUrl.contains(".wav")) ext = ".wav";
                else if (lowerUrl.contains(".ogg")) ext = ".ogg";
                else if (lowerUrl.contains(".flac")) ext = ".flac";

                File targetSoundFile = new File(cacheDir, cleanId + "_remote" + ext);
                if (!targetSoundFile.exists() || targetSoundFile.length() == 0) {
                    downloadFile(soundUrl, targetSoundFile);
                }
                if (targetSoundFile.exists() && targetSoundFile.length() > 0) {
                    loadedSounds.put(cleanId, targetSoundFile);
                }
            }
        }, "JumpscarePreloadThread").start();
    }

    public boolean downloadFile(String urlStr, File destination) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setInstanceFollowRedirects(true);

            int status = conn.getResponseCode();
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
                String newUrl = conn.getHeaderField("Location");
                if (newUrl != null && !newUrl.isEmpty()) {
                    return downloadFile(newUrl, destination);
                }
            }

            if (status == 200) {
                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }

                if (destination.length() == 0) {
                    destination.delete();
                    return false;
                }

                String name = destination.getName().toLowerCase(Locale.ROOT);
                if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp")) {
                    if (!isValidImageFile(destination)) {
                        if (isAudioFileOrHeader(destination)) {
                            String cleanName = name.substring(0, name.lastIndexOf('.'));
                            File rescued = new File(destination.getParentFile(), cleanName + ".mp3");
                            destination.renameTo(rescued);
                            JumpscareMod.LOGGER.warn("[JumpscareAssetManager] Rescued audio file mistakenly saved as image: " + rescued.getName());
                            return true;
                        } else {
                            destination.delete();
                            JumpscareMod.LOGGER.warn("[JumpscareAssetManager] Downloaded corrupt or non-image file from: " + urlStr);
                            return false;
                        }
                    }
                } else if (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac")) {
                    try (FileInputStream fis = new FileInputStream(destination)) {
                        byte[] firstBytes = new byte[16];
                        int r = fis.read(firstBytes);
                        if (r > 0 && (firstBytes[0] == '<' || (r >= 4 && firstBytes[0] == '{'))) {
                            destination.delete();
                            JumpscareMod.LOGGER.warn("[JumpscareAssetManager] Downloaded HTML/JSON error page instead of audio from: " + urlStr);
                            return false;
                        }
                    }
                }

                return true;
            } else {
                JumpscareMod.LOGGER.error("[JumpscareAssetManager] Download failed with status " + status + " for: " + urlStr);
            }
        } catch (Exception e) {
            JumpscareMod.LOGGER.error("[JumpscareAssetManager] Download failed for: " + urlStr, e);
        }

        // Recovery: Check sibling client directories (run, run2, etc.) for existing downloaded copy
        File siblingMatch = findInSiblingCache(destination.getName());
        if (siblingMatch != null && siblingMatch.exists() && siblingMatch.length() > 0) {
            boolean isImg = destination.getName().endsWith(".png") || destination.getName().endsWith(".jpg") || destination.getName().endsWith(".jpeg") || destination.getName().endsWith(".gif") || destination.getName().endsWith(".webp");
            if (!isImg || isValidImageFile(siblingMatch)) {
                JumpscareMod.LOGGER.info("[JumpscareAssetManager] Recovered '" + destination.getName() + "' from sibling cache: " + siblingMatch.getAbsolutePath());
                try {
                    java.nio.file.Files.copy(siblingMatch.toPath(), destination.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    public File findInSiblingCache(String pattern) {
        if (gameDir == null || pattern == null || pattern.isEmpty()) return null;
        File parent = gameDir.getParentFile();
        if (parent == null || !parent.exists()) return null;

        String[] candidateDirs = {"run", "run2", "run3", "run4", "run5"};
        for (String dirName : candidateDirs) {
            File siblingCache = new File(new File(parent, dirName), "jumpscares_cache");
            if (siblingCache.exists() && (cacheDir == null || !siblingCache.getAbsolutePath().equalsIgnoreCase(cacheDir.getAbsolutePath()))) {
                File exact = new File(siblingCache, pattern);
                if (exact.exists() && exact.length() > 0) {
                    if (isSafeFile(exact)) return exact;
                }
                File[] files = siblingCache.listFiles();
                if (files != null) {
                    String lowerPattern = pattern.toLowerCase(Locale.ROOT);
                    for (File f : files) {
                        String name = f.getName().toLowerCase(Locale.ROOT);
                        if ((name.startsWith(lowerPattern) || name.contains(lowerPattern)) && f.length() > 0) {
                            if (isSafeFile(f)) return f;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSafeFile(File f) {
        if (f == null || !f.exists() || f.length() == 0) return false;
        String name = f.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp")) {
            return isValidImageFile(f);
        }
        return true;
    }

    public File getExactSoundFile(String id) {
        if (id == null || id.isEmpty()) return null;
        String cleanId = id.toLowerCase(Locale.ROOT);
        if (cleanId.endsWith("_remote")) {
            cleanId = cleanId.substring(0, cleanId.length() - 7);
        }
        if (loadedSounds.containsKey(cleanId)) return loadedSounds.get(cleanId);

        if (cacheDir != null && cacheDir.exists()) {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().toLowerCase(Locale.ROOT);
                    if (name.startsWith(cleanId) && (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac"))) {
                        loadedSounds.put(cleanId, file);
                        return file;
                    }
                }
            }
        }
        if (jumpscareDir != null && jumpscareDir.exists()) {
            File[] files = jumpscareDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    String name = file.getName().toLowerCase(Locale.ROOT);
                    if (name.startsWith(cleanId) && (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac"))) {
                        loadedSounds.put(cleanId, file);
                        return file;
                    }
                }
            }
        }
        File siblingSound = findInSiblingCache(cleanId);
        if (siblingSound != null) {
            loadedSounds.put(cleanId, siblingSound);
            return siblingSound;
        }
        return null;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public File getJumpscareDir() {
        return jumpscareDir;
    }

    public void registerSound(String id, File file) {
        if (id != null && file != null) {
            loadedSounds.put(id.toLowerCase(Locale.ROOT), file);
        }
    }

    public ResourceLocation getTextureId(String id) {
        if (id != null && !id.isEmpty()) {
            String cleanId = id.toLowerCase(Locale.ROOT);
            if (cleanId.endsWith("_remote")) {
                cleanId = cleanId.substring(0, cleanId.length() - 7);
            }
            if (loadedTextures.containsKey(cleanId)) return loadedTextures.get(cleanId);

            // Avoid per-frame disk scans if recently failed
            Long failedUntil = failedTextures.get(cleanId);
            if (failedUntil != null && System.currentTimeMillis() < failedUntil) {
                return getFallbackTexture();
            }

            if (cacheDir != null && cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = file.getName().toLowerCase(Locale.ROOT);
                        if (name.startsWith(cleanId) && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".gif") || name.endsWith(".webp"))) {
                            loadDynamicTexture(cleanId, file);
                            if (loadedTextures.containsKey(cleanId)) return loadedTextures.get(cleanId);
                        }
                    }
                }
            }

            File siblingImg = findInSiblingCache(cleanId);
            if (siblingImg != null && (siblingImg.getName().endsWith(".png") || siblingImg.getName().endsWith(".jpg") || siblingImg.getName().endsWith(".jpeg") || siblingImg.getName().endsWith(".gif") || siblingImg.getName().endsWith(".webp"))) {
                loadDynamicTexture(cleanId, siblingImg);
                if (loadedTextures.containsKey(cleanId)) return loadedTextures.get(cleanId);
            }

            // Cooldown if not found
            if (!loadedTextures.containsKey(cleanId)) {
                failedTextures.put(cleanId, System.currentTimeMillis() + 5000L);
            }
        }
        return getFallbackTexture();
    }

    private ResourceLocation getFallbackTexture() {
        if (loadedTextures.containsKey("spooky")) return loadedTextures.get("spooky");
        if (loadedTextures.containsKey("default")) return loadedTextures.get("default");
        if (!loadedTextures.isEmpty()) return loadedTextures.values().iterator().next();
        return null;
    }

    public File getSoundFile(String id) {
        if (id != null && !id.isEmpty()) {
            String cleanId = id.toLowerCase(Locale.ROOT);
            if (cleanId.endsWith("_remote")) {
                cleanId = cleanId.substring(0, cleanId.length() - 7);
            }
            if (loadedSounds.containsKey(cleanId)) return loadedSounds.get(cleanId);

            Long failedUntil = failedSounds.get(cleanId);
            if (failedUntil != null && System.currentTimeMillis() < failedUntil) {
                return getFallbackSound();
            }

            if (cacheDir != null && cacheDir.exists()) {
                File[] files = cacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        String name = file.getName().toLowerCase(Locale.ROOT);
                        if (name.startsWith(cleanId) && (name.endsWith(".mp3") || name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".flac"))) {
                            loadedSounds.put(cleanId, file);
                            return file;
                        }
                    }
                }
            }

            File siblingSound = findInSiblingCache(cleanId);
            if (siblingSound != null) {
                loadedSounds.put(cleanId, siblingSound);
                return siblingSound;
            }

            failedSounds.put(cleanId, System.currentTimeMillis() + 5000L);
        }
        return getFallbackSound();
    }

    private File getFallbackSound() {
        if (loadedSounds.containsKey("spooky")) return loadedSounds.get("spooky");
        if (loadedSounds.containsKey("default")) return loadedSounds.get("default");
        if (!loadedSounds.isEmpty()) return loadedSounds.values().iterator().next();
        return null;
    }

    public Set<String> getAvailableJumpscares() {
        Set<String> all = new HashSet<>(loadedTextures.keySet());
        all.addAll(loadedSounds.keySet());
        return all;
    }

    private static final ResourceLocation DEFAULT_GUI_BG = new ResourceLocation(JumpscareMod.MOD_ID, "textures/gui/menu_background.png");

    public ResourceLocation getGuiBackgroundTexture() {
        if (loadedTextures.containsKey("gui_background")) return loadedTextures.get("gui_background");
        if (loadedTextures.containsKey("background")) return loadedTextures.get("background");
        if (loadedTextures.containsKey("menu_bg")) return loadedTextures.get("menu_bg");

        // Check in jumpscareDir for custom user background file
        if (jumpscareDir != null && jumpscareDir.exists()) {
            File[] bgs = jumpscareDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase(Locale.ROOT);
                return (lower.startsWith("gui_background") || lower.startsWith("background") || lower.startsWith("menu_bg"))
                    && (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".webp"));
            });
            if (bgs != null && bgs.length > 0) {
                loadDynamicTexture("gui_background", bgs[0]);
                if (loadedTextures.containsKey("gui_background")) {
                    return loadedTextures.get("gui_background");
                }
            }
        }
        return DEFAULT_GUI_BG;
    }

    private void createTutorialFile() {
        File txtFile = new File(jumpscareDir, "BACA_CARA_INPUT_JUMPSCARE.txt");
        if (!txtFile.exists()) {
            try (FileWriter writer = new FileWriter(txtFile, StandardCharsets.UTF_8)) {
                writer.write("=====================================================\n");
                writer.write("    CARA GAMPANG MEMASUKKAN JUMPSCARE CUSTOM\n");
                writer.write("=====================================================\n\n");
                writer.write("CARA 1: MASUKKAN FILE DI FOLDER INI\n");
                writer.write("1. Buat folder baru di sini, contoh nama folder: kunti\n");
                writer.write("2. Masukkan file gambar (.png / .jpg / .gif) & file suara (.wav / .mp3) ke dalam folder kunti tersebut.\n");
                writer.write("3. Di game ketik: /jumpscare reload\n");
                writer.write("4. Tembak player: /jumpscare NamaPlayer kunti\n\n");
                writer.write("CARA 2: PAKE MENU IN-GAME (/jumpscare)\n");
                writer.write("1. Ketik /jumpscare di Minecraft.\n");
                writer.write("2. Pilih tab [➕ TAMBAH JUMPSCARE] lalu tempel link gambar & suara dari Discord / internet!\n");
            } catch (Exception ignored) {}
        }
    }

    private void ensureDefaultJumpscareExists() {
        File defaultDir = new File(jumpscareDir, "default");
        if (!defaultDir.exists()) defaultDir.mkdirs();

        File defaultImg = new File(defaultDir, "default.png");
        File defaultSound = new File(defaultDir, "default.wav");

        if (!defaultImg.exists()) generateDefaultScaryImage(defaultImg);
        if (!defaultSound.exists()) generateDefaultScaryAudio(defaultSound);
    }

    private void generateDefaultScaryImage(File file) {
        try {
            int w = 800, h = 800;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, w, h);

            g.setColor(new Color(180, 0, 0));
            g.fillOval(150, 100, 500, 600);

            g.setColor(Color.BLACK);
            g.fillOval(250, 260, 100, 140);
            g.fillOval(450, 260, 100, 140);

            g.setColor(Color.RED);
            g.fillOval(285, 310, 30, 40);
            g.fillOval(485, 310, 30, 40);

            g.setColor(Color.BLACK);
            g.fillOval(320, 480, 160, 200);

            g.setColor(Color.WHITE);
            int[] xTeeth = {330, 360, 390, 420, 450, 470};
            for (int x : xTeeth) {
                g.fillPolygon(new int[]{x, x + 15, x + 30}, new int[]{480, 530, 480}, 3);
            }

            g.dispose();
            ImageIO.write(img, "PNG", file);
        } catch (Exception e) {
            JumpscareMod.LOGGER.error("Error generating default scary image", e);
        }
    }

    private void generateDefaultScaryAudio(File file) {
        try {
            float sampleRate = 44100;
            double duration = 1.5;
            int numSamples = (int) (duration * sampleRate);
            byte[] pcmData = new byte[numSamples * 2];

            Random rnd = new Random();
            for (int i = 0; i < numSamples; i++) {
                double t = i / sampleRate;
                double freq = 2200 - (t * 1200) + (rnd.nextDouble() * 400);
                double wave = Math.sin(2 * Math.PI * freq * t);
                double env = Math.min(1.0, t * 20.0) * Math.max(0.0, 1.0 - (t / duration));
                short sample = (short) (wave * env * 30000.0);

                pcmData[i * 2] = (byte) (sample & 0xff);
                pcmData[i * 2 + 1] = (byte) ((sample >> 8) & 0xff);
            }

            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
                 AudioInputStream ais = new AudioInputStream(bais, format, numSamples)) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, file);
            }
        } catch (Exception e) {
            JumpscareMod.LOGGER.error("Error generating default scary audio", e);
        }
    }
}
