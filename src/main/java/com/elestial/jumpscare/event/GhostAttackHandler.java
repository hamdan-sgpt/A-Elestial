package com.elestial.jumpscare.event;

import com.elestial.jumpscare.JumpscareMod;
import com.elestial.jumpscare.command.GhostCommand;
import com.elestial.jumpscare.config.JumpscareServerConfig;
import com.elestial.jumpscare.network.JumpscareNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Mod.EventBusSubscriber(modid = JumpscareMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GhostAttackHandler {

    // Cooldown 2.5 detik per korban agar suara dan layar jumpscare tidak spam/glitch saat dipukul bertubi-tubi
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 2500L;

    // Penyimpanan ID jumpscare pilihan masing-masing player Ghost
    private static final Map<UUID, String> GHOST_ATTACK_SELECTION = new ConcurrentHashMap<>();

    public static void setGhostAttackJumpscare(UUID playerUuid, String jumpscareId) {
        if (playerUuid == null) return;
        String val;
        if (jumpscareId == null || jumpscareId.trim().isEmpty() || jumpscareId.equalsIgnoreCase("random")) {
            GHOST_ATTACK_SELECTION.remove(playerUuid);
            val = "random";
        } else {
            val = jumpscareId.trim().toLowerCase();
            GHOST_ATTACK_SELECTION.put(playerUuid, val);
        }

        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
            if (player != null) {
                JumpscareNetworking.sendSyncGhostAttackPacket(player, val);
            }
        }
    }

    public static String getGhostAttackJumpscare(UUID playerUuid) {
        if (playerUuid == null) return null;
        return GHOST_ATTACK_SELECTION.get(playerUuid);
    }

    /**
     * Menangani serangan jarak dekat / klik kiri ke player lain (bahkan jika damage 0 / mode kreatif)
     */
    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer attacker && event.getTarget() instanceof ServerPlayer victim) {
            handleAttackInteraction(attacker, victim, false);
        }
    }

    /**
     * Menangani serangan berbasis damage, termasuk panah / proyektil yang ditembakkan
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() instanceof ServerPlayer attacker) {
            boolean canceled = handleAttackInteraction(attacker, victim, true);
            if (canceled && event.isCancelable()) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * Logika utama interaksi serangan antar Ghost dan Non-Ghost
     * @return true jika damage perlu di-cancel (misal saat Ghost diubah ke Spectator)
     */
    private static boolean handleAttackInteraction(ServerPlayer attacker, ServerPlayer victim, boolean fromDamageEvent) {
        if (attacker == null || victim == null) return false;
        if (attacker.equals(victim)) return false;

        boolean attackerIsGhost = GhostCommand.isGhost(attacker);
        boolean victimIsGhost = GhostCommand.isGhost(victim);

        // KASUS 1: Ghost menyerang Non-Ghost -> Korban Non-Ghost terkena Jumpscare!
        if (attackerIsGhost && !victimIsGhost) {
            handleGhostAttacksHuman(attacker, victim);
            return false;
        }

        // KASUS 2: Player Non-Ghost menyerang Ghost -> Player Ghost yang kena serang otomatis /gmsp (Spectator)!
        if (!attackerIsGhost && victimIsGhost) {
            return handleHumanAttacksGhost(attacker, victim);
        }

        return false;
    }

    /**
     * Eksekusi ketika Ghost menyerang Non-Ghost (Korban terkena jumpscare)
     */
    private static void handleGhostAttacksHuman(ServerPlayer attacker, ServerPlayer victim) {
        // Cek cooldown agar jumpscare tidak tumpang tindih
        long now = System.currentTimeMillis();
        Long lastTrigger = COOLDOWNS.get(victim.getUUID());
        if (lastTrigger != null && (now - lastTrigger) < COOLDOWN_MS) {
            return;
        }
        COOLDOWNS.put(victim.getUUID(), now);

        // Tentukan jumpscare ID sesuai pilihan khusus Ghost penyerang
        String selected = getGhostAttackJumpscare(attacker.getUUID());
        String jumpscareId;
        if (selected != null && !selected.isEmpty() && !selected.equalsIgnoreCase("random")) {
            if (JumpscareServerConfig.getEntry(selected) != null || selected.equalsIgnoreCase("spooky")) {
                jumpscareId = selected;
            } else {
                jumpscareId = selectJumpscareId();
            }
        } else {
            jumpscareId = selectJumpscareId();
        }

        int durationMs = 2000;
        float intensity = 1.2f;

        JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(jumpscareId);
        if (entry != null) {
            if (entry.durationMs > 0) durationMs = entry.durationMs;
            if (entry.intensity > 0) intensity = entry.intensity;
        }

        // Tembakkan jumpscare packet ke korban
        boolean success = JumpscareNetworking.sendJumpscarePacket(victim, jumpscareId, durationMs, intensity);
        if (success) {
            JumpscareMod.LOGGER.info("[GhostAttackHandler] Ghost '{}' attacked non-ghost '{}' -> triggered jumpscare '{}'",
                attacker.getScoreboardName(), victim.getScoreboardName(), jumpscareId);
        }
    }

    /**
     * Eksekusi ketika Player Non-Ghost menyerang Ghost -> Player Ghost otomatis menjadi Spectator (/gmsp)
     * @return true untuk membatalkan physical damage agar tidak mati/drop inventory
     */
    private static boolean handleHumanAttacksGhost(ServerPlayer attacker, ServerPlayer victim) {
        // Jika Ghost sudah dalam mode Spectator, abaikan
        if (victim.isSpectator()) {
            return true;
        }

        // Ubah mode Ghost yang diserang ke SPECTATOR (/gmsp)
        victim.setGameMode(GameType.SPECTATOR);

        JumpscareMod.LOGGER.info("[GhostAttackHandler] Non-ghost '{}' attacked ghost '{}' -> ghost switched to SPECTATOR mode",
            attacker.getScoreboardName(), victim.getScoreboardName());

        return true;
    }

    public static String selectJumpscareId() {
        Map<String, JumpscareServerConfig.JumpscareEntry> entries = JumpscareServerConfig.getAllEntries();
        if (entries.containsKey("ghost")) {
            return "ghost";
        }
        if (!entries.isEmpty()) {
            List<String> keys = new ArrayList<>(entries.keySet());
            return keys.get(ThreadLocalRandom.current().nextInt(keys.size()));
        }
        return "spooky";
    }
}
