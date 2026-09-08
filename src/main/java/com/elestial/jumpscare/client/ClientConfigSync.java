package com.elestial.jumpscare.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public class ClientConfigSync {
    public static class EntryData {
        public final String imageUrl;
        public final String soundUrl;
        public final int durationMs;
        public final float intensity;

        public EntryData(String imageUrl, String soundUrl, int durationMs, float intensity) {
            this.imageUrl = imageUrl != null ? imageUrl : "";
            this.soundUrl = soundUrl != null ? soundUrl : "";
            this.durationMs = durationMs;
            this.intensity = intensity;
        }
    }

    private static final Map<String, EntryData> ENTRIES = new ConcurrentHashMap<>();

    public static void updateEntries(Map<String, EntryData> newEntries) {
        ENTRIES.clear();
        if (newEntries != null) {
            for (Map.Entry<String, EntryData> e : newEntries.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    ENTRIES.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue());
                }
            }
        }
    }

    public static void setServerIds(List<String> ids) {
        if (ids != null) {
            for (String id : ids) {
                if (id != null && !ENTRIES.containsKey(id.toLowerCase(Locale.ROOT))) {
                    ENTRIES.put(id.toLowerCase(Locale.ROOT), new EntryData("", "", 2000, 1.0f));
                }
            }
        }
    }

    public static List<String> getServerIds() {
        return new ArrayList<>(ENTRIES.keySet());
    }

    public static Map<String, EntryData> getAllEntries() {
        return Collections.unmodifiableMap(ENTRIES);
    }

    public static EntryData getEntry(String id) {
        if (id == null) return null;
        return ENTRIES.get(id.toLowerCase(Locale.ROOT));
    }

    public static String getSoundUrl(String id) {
        EntryData e = getEntry(id);
        return e != null ? e.soundUrl : "";
    }

    public static String getImageUrl(String id) {
        EntryData e = getEntry(id);
        return e != null ? e.imageUrl : "";
    }
}

