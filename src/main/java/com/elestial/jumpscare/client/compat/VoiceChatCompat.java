package com.elestial.jumpscare.client.compat;

import com.elestial.jumpscare.JumpscareMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Kompatibilitas khusus Simple Voice Chat (versi forge-1.20.1-2.6.22 & kompatibel).
 * Mengunci dan mematikan mikrofon korban saat kesurupan, memblokir tombol PTT/Mute,
 * serta mencegah pembukaan menu pengaturan voice chat selama kesurupan aktif.
 */
@OnlyIn(Dist.CLIENT)
public class VoiceChatCompat {

    private static boolean initialized = false;
    private static boolean voiceChatPresent = false;

    // Simple Voice Chat 2.6.22 ClientManager & StateManager reflection handles
    private static Method getPlayerStateManagerMethod = null;
    private static Method isMutedMethod = null;
    private static Method setMutedMethod = null;

    // Key mappings
    private static KeyMapping[] allKeys = null;
    private static KeyMapping keyMute = null;
    private static KeyMapping keyPtt = null;
    private static KeyMapping keyWhisper = null;

    // PTTKeyHandler reflection
    private static Method getPttKeyHandlerMethod = null;
    private static Field pttKeyDownField = null;
    private static Field whisperKeyDownField = null;

    private static boolean wasOriginallyMuted = false;
    private static boolean isPossessionMuteActive = false;

    private static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Class<?> clientManagerClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
            getPlayerStateManagerMethod = clientManagerClass.getMethod("getPlayerStateManager");
            Class<?> playerStateManagerClass = Class.forName("de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager");
            isMutedMethod = playerStateManagerClass.getMethod("isMuted");
            setMutedMethod = playerStateManagerClass.getMethod("setMuted", boolean.class);

            // Access KeyEvents
            Class<?> keyEventsClass = Class.forName("de.maxhenkel.voicechat.voice.client.KeyEvents");
            try {
                Field allKeysField = keyEventsClass.getField("ALL_KEYS");
                allKeys = (KeyMapping[]) allKeysField.get(null);
            } catch (Throwable ignored) {}

            try {
                Field muteField = keyEventsClass.getField("KEY_MUTE");
                keyMute = (KeyMapping) muteField.get(null);
            } catch (Throwable ignored) {}

            try {
                Field pttField = keyEventsClass.getField("KEY_PTT");
                keyPtt = (KeyMapping) pttField.get(null);
            } catch (Throwable ignored) {}

            try {
                Field whisperField = keyEventsClass.getField("KEY_WHISPER");
                keyWhisper = (KeyMapping) whisperField.get(null);
            } catch (Throwable ignored) {}

            // Access PTTKeyHandler
            try {
                getPttKeyHandlerMethod = clientManagerClass.getMethod("getPttKeyHandler");
                Class<?> pttHandlerClass = Class.forName("de.maxhenkel.voicechat.voice.client.PTTKeyHandler");
                pttKeyDownField = pttHandlerClass.getDeclaredField("pttKeyDown");
                pttKeyDownField.setAccessible(true);
                whisperKeyDownField = pttHandlerClass.getDeclaredField("whisperKeyDown");
                whisperKeyDownField.setAccessible(true);
            } catch (Throwable ignored) {}

            voiceChatPresent = true;
            JumpscareMod.LOGGER.info("[ElestialJumpscare] Simple Voice Chat (2.6.22) detected and hooked successfully for possession auto-mute!");
        } catch (Throwable t) {
            voiceChatPresent = false;
        }
    }

    public static void onPossessionStart() {
        init();
        if (!voiceChatPresent) return;

        try {
            Object stateManager = getPlayerStateManagerMethod.invoke(null);
            if (stateManager != null) {
                if (!isPossessionMuteActive) {
                    wasOriginallyMuted = (Boolean) isMutedMethod.invoke(stateManager);
                    isPossessionMuteActive = true;
                }
                setMutedMethod.invoke(stateManager, true);
            }
        } catch (Throwable ignored) {}
    }

    public static void tickPossessionMute() {
        init();
        if (!voiceChatPresent) return;

        try {
            // 1. Force microphone muted state
            Object stateManager = getPlayerStateManagerMethod.invoke(null);
            if (stateManager != null) {
                boolean muted = (Boolean) isMutedMethod.invoke(stateManager);
                if (!muted) {
                    setMutedMethod.invoke(stateManager, true);
                }
            }

            // 2. Consume and clear all voicechat key mappings
            if (allKeys != null) {
                for (KeyMapping k : allKeys) {
                    if (k != null) {
                        k.setDown(false);
                        while (k.consumeClick()) {}
                    }
                }
            }

            if (keyMute != null) {
                keyMute.setDown(false);
                while (keyMute.consumeClick()) {}
            }
            if (keyPtt != null) {
                keyPtt.setDown(false);
                while (keyPtt.consumeClick()) {}
            }
            if (keyWhisper != null) {
                keyWhisper.setDown(false);
                while (keyWhisper.consumeClick()) {}
            }

            // 3. Clear PTT internal states
            if (getPttKeyHandlerMethod != null) {
                Object pttHandler = getPttKeyHandlerMethod.invoke(null);
                if (pttHandler != null) {
                    if (pttKeyDownField != null) pttKeyDownField.set(pttHandler, false);
                    if (whisperKeyDownField != null) whisperKeyDownField.set(pttHandler, false);
                }
            }

            // 4. Force-close voice chat settings or GUI screens if opened while possessed
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen != null && mc.screen.getClass().getName().toLowerCase().contains("voicechat")) {
                mc.setScreen(null);
            }
        } catch (Throwable ignored) {}
    }

    public static void onPossessionEnd() {
        init();
        if (!voiceChatPresent) return;

        try {
            if (isPossessionMuteActive) {
                Object stateManager = getPlayerStateManagerMethod.invoke(null);
                if (stateManager != null) {
                    // Restore original mute state (if player was originally not muted, unmute them)
                    setMutedMethod.invoke(stateManager, wasOriginallyMuted);
                }
                isPossessionMuteActive = false;
            }
        } catch (Throwable ignored) {}
    }
}
