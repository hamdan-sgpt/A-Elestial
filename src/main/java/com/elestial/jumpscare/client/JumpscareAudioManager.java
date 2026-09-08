package com.elestial.jumpscare.client;

import com.elestial.jumpscare.JumpscareMod;
import com.elestial.jumpscare.asset.JumpscareAssetManager;
import com.elestial.jumpscare.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class JumpscareAudioManager {

    public static void playJumpscareSound(String jumpscareId) {
        File soundFile = JumpscareAssetManager.getInstance().getSoundFile(jumpscareId);
        if (soundFile == null || !soundFile.exists()) {
            soundFile = JumpscareAssetManager.getInstance().getSoundFile("spooky");
        }
        if (soundFile == null || !soundFile.exists()) {
            soundFile = JumpscareAssetManager.getInstance().getSoundFile("default");
        }
        if (soundFile == null || !soundFile.exists()) {
            JumpscareMod.LOGGER.warn("[JumpscareAudioManager] No sound file found for ID: " + jumpscareId + ", triggering fallback heartbeat");
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.HEARTBEAT.get(), 1.0f, 1.2f));
            }
            return;
        }

        playLocalSoundFile(soundFile);
    }

    public static void playLocalSoundFile(File soundFile) {
        if (soundFile == null || !soundFile.exists()) return;

        new Thread(() -> {
            String fileName = soundFile.getName().toLowerCase(Locale.ROOT);
            if (fileName.endsWith(".mp3")) {
                if (playMp3(soundFile)) return;
            }

            if (!playWavOrStandard(soundFile)) {
                // Fallback to JLayer MP3 decoder if standard AudioSystem fails
                playMp3(soundFile);
            }
        }, "JumpscareAudioThread").start();
    }

    public static void playAudioScare(String soundId, String soundUrl, float volume, float pitch, boolean behindPlayer) {
        String cleanId = (soundId != null && !soundId.isEmpty()) ? soundId.trim().toLowerCase(Locale.ROOT) : "fakestep";

        // 1. If remote sound URL is provided, ensure it is downloaded and cached
        if (soundUrl != null && !soundUrl.isEmpty() && (soundUrl.startsWith("http://") || soundUrl.startsWith("https://"))) {
            new Thread(() -> {
                File cachedFile = JumpscareAssetManager.getInstance().getExactSoundFile(cleanId);
                if (cachedFile == null || !cachedFile.exists()) {
                    File cacheDir = JumpscareAssetManager.getInstance().getCacheDir();
                    String ext = ".mp3";
                    String lowerUrl = soundUrl.toLowerCase(Locale.ROOT);
                    if (lowerUrl.contains(".wav")) ext = ".wav";
                    else if (lowerUrl.contains(".ogg")) ext = ".ogg";
                    else if (lowerUrl.contains(".flac")) ext = ".flac";

                    File targetSoundFile = new File(cacheDir, cleanId + "_remote" + ext);
                    boolean success = JumpscareAssetManager.getInstance().downloadFile(soundUrl, targetSoundFile);
                    if (success && targetSoundFile.exists() && targetSoundFile.length() > 0) {
                        JumpscareAssetManager.getInstance().registerSound(cleanId, targetSoundFile);
                        cachedFile = targetSoundFile;
                    } else {
                        JumpscareMod.LOGGER.error("[JumpscareAudioManager] Failed to download audio from URL: " + soundUrl);
                    }
                }

                final File targetFile = cachedFile;
                if (targetFile != null && targetFile.exists()) {
                    playLocalSoundFile(targetFile);
                } else {
                    Minecraft.getInstance().execute(() -> {
                        playRegisteredOrFallback(cleanId, volume, pitch, behindPlayer);
                    });
                }
            }, "AudioScareDownloadThread").start();
            return;
        }

        // 2. Play immediately if local exact match exists
        File localFile = JumpscareAssetManager.getInstance().getExactSoundFile(cleanId);
        if (localFile != null && localFile.exists()) {
            playLocalSoundFile(localFile);
            return;
        }

        Minecraft.getInstance().execute(() -> {
            playRegisteredOrFallback(cleanId, volume, pitch, behindPlayer);
        });
    }

    private static void playRegisteredOrFallback(String soundId, float volume, float pitch, boolean behindPlayer) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Check registered SoundEvents (ModSounds or vanilla Minecraft or other mods)
        SoundEvent soundEvent = null;
        if (soundId.equals("fakestep") || soundId.equals("fake_step") || soundId.equals("step")) {
            soundEvent = ModSounds.FAKESTEP.get();
        } else if (soundId.equals("heartbeat")) {
            soundEvent = ModSounds.HEARTBEAT.get();
        } else {
            ResourceLocation loc = soundId.contains(":") ? new ResourceLocation(soundId) : new ResourceLocation(JumpscareMod.MOD_ID, soundId);
            if (ForgeRegistries.SOUND_EVENTS.containsKey(loc)) {
                soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(loc);
            }
        }

        if (soundEvent != null) {
            if (behindPlayer) {
                // Calculate position 2.5 blocks directly behind player's look direction
                Vec3 look = player.getLookAngle();
                double bx = player.getX() - look.x * 2.5;
                double by = player.getY() + 0.1;
                double bz = player.getZ() - look.z * 2.5;

                mc.getSoundManager().play(
                    new SimpleSoundInstance(
                        soundEvent.getLocation(),
                        SoundSource.PLAYERS,
                        volume,
                        pitch,
                        SoundInstance.createUnseededRandom(),
                        false,
                        0,
                        SoundInstance.Attenuation.LINEAR,
                        bx, by, bz,
                        false
                    )
                );
            } else {
                mc.getSoundManager().play(
                    SimpleSoundInstance.forUI(soundEvent, pitch, volume)
                );
            }
            return;
        }

        // Check local custom file in jumpscares directory
        File customFile = JumpscareAssetManager.getInstance().getSoundFile(soundId);
        if (customFile != null && customFile.exists()) {
            playLocalSoundFile(customFile);
            return;
        }

        // Fallback default: fakestep
        mc.getSoundManager().play(
            SimpleSoundInstance.forUI(ModSounds.FAKESTEP.get(), pitch, volume)
        );
    }

    private static Class<?> fallbackPlayerClass = null;

    private static boolean playMp3(File mp3File) {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(mp3File))) {
            Class<?> playerClass = null;
            // 1. Try standard unshaded class
            try {
                playerClass = Class.forName("javazoom.jl.player.Player");
            } catch (Throwable ignored) {}

            // 2. Try shaded relocated class
            if (playerClass == null) {
                try {
                    playerClass = Class.forName("com.elestial.jumpscare.shadow.javazoom.jl.player.Player");
                } catch (Throwable ignored) {}
            }

            // 3. Try dynamic fallback classloader from cache if ModLauncher didn't include it in dev mode
            if (playerClass == null) {
                playerClass = getFallbackJLayerPlayerClass();
            }

            if (playerClass != null) {
                java.lang.reflect.Constructor<?> ctor = playerClass.getConstructor(java.io.InputStream.class);
                Object player = ctor.newInstance(bis);
                java.lang.reflect.Method playMethod = playerClass.getMethod("play");
                JumpscareMod.LOGGER.info("[JumpscareAudioManager] Playing MP3 audio via JLayer (" + playerClass.getName() + "): " + mp3File.getName());
                playMethod.invoke(player);
                return true;
            } else {
                JumpscareMod.LOGGER.error("[JumpscareAudioManager] JLayer Player class not found on classpath!");
            }
        } catch (Throwable t) {
            JumpscareMod.LOGGER.error("[JumpscareAudioManager] Failed to play MP3 with JLayer (" + mp3File.getName() + "): " + t.getMessage(), t);
        }
        return false;
    }

    private static Class<?> getFallbackJLayerPlayerClass() {
        if (fallbackPlayerClass != null) return fallbackPlayerClass;
        try {
            File userHome = new File(System.getProperty("user.home", "."));
            File gradleCache = new File("D:/codingan/.gradle/caches/modules-2/files-2.1/javazoom/jlayer/1.0.1");
            if (!gradleCache.exists()) {
                gradleCache = new File(userHome, ".gradle/caches/modules-2/files-2.1/javazoom/jlayer/1.0.1");
            }
            if (gradleCache.exists()) {
                java.util.List<File> jarList = new java.util.ArrayList<>();
                findJars(gradleCache, jarList);
                if (!jarList.isEmpty()) {
                    java.net.URLClassLoader ucl = new java.net.URLClassLoader(new java.net.URL[]{jarList.get(0).toURI().toURL()}, JumpscareAudioManager.class.getClassLoader());
                    fallbackPlayerClass = ucl.loadClass("javazoom.jl.player.Player");
                    return fallbackPlayerClass;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static void findJars(File dir, java.util.List<File> list) {
        if (dir == null || !dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                findJars(f, list);
            } else if (f.getName().endsWith(".jar") && !f.getName().contains("sources") && !f.getName().contains("javadoc")) {
                list.add(f);
            }
        }
    }

    private static boolean playWavOrStandard(File soundFile) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
            AudioFormat baseFormat = audioStream.getFormat();

            AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate() > 0 ? baseFormat.getSampleRate() : 44100F,
                16,
                baseFormat.getChannels() > 0 ? baseFormat.getChannels() : 2,
                (baseFormat.getChannels() > 0 ? baseFormat.getChannels() : 2) * 2,
                baseFormat.getSampleRate() > 0 ? baseFormat.getSampleRate() : 44100F,
                false
            );

            AudioInputStream decodedStream = AudioSystem.isConversionSupported(decodedFormat, baseFormat)
                ? AudioSystem.getAudioInputStream(decodedFormat, audioStream)
                : audioStream;

            Clip audioClip = AudioSystem.getClip();
            audioClip.open(decodedStream);

            if (audioClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) audioClip.getControl(FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(gainControl.getMaximum());
            }

            audioClip.start();
            return true;
        } catch (Exception e) {
            JumpscareMod.LOGGER.warn("[JumpscareAudioManager] Standard AudioSystem playback failed for " + soundFile.getName() + ": " + e.getMessage());
            return false;
        }
    }
}
