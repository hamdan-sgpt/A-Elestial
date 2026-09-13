package com.elestial.jumpscare;

import com.elestial.jumpscare.command.GhostCommand;
import com.elestial.jumpscare.command.JumpscareCommand;
import com.elestial.jumpscare.config.JumpscareServerConfig;
import com.elestial.jumpscare.network.JumpscareNetworking;
import com.elestial.jumpscare.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(JumpscareMod.MOD_ID)
public class JumpscareMod {
    public static final String MOD_ID = "elestialjumpscare";
    public static final Logger LOGGER = LogManager.getLogger("ElestialJumpscare");

    public JumpscareMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::setup);
        ModSounds.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("[ElestialJumpscare] Setting up Forge Jumpscare Mod 1.20.1...");
        JumpscareNetworking.register();
        JumpscareServerConfig.loadConfig();
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        JumpscareCommand.register(event.getDispatcher());
        GhostCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            JumpscareNetworking.syncConfigToPlayer(player);
            String saved = com.elestial.jumpscare.event.GhostAttackHandler.getGhostAttackJumpscare(player.getUUID());
            if (saved != null) {
                JumpscareNetworking.sendSyncGhostAttackPacket(player, saved);
            }
        }
    }

    @SubscribeEvent
    public void onServerChat(ServerChatEvent event) {
        String message = event.getRawText().trim();
        ServerPlayer sender = event.getPlayer();
        if (sender == null || sender.getServer() == null) return;

        // 1. Fakestep chat trigger (!fakestep <target>)
        if (message.startsWith("!fakestep ") || message.startsWith("#fakestep ")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2) {
                String targetName = parts[1];
                if ("@a".equalsIgnoreCase(targetName)) {
                    for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
                        JumpscareNetworking.sendAudioScarePacket(p, "fakestep", "", 1.0f, 1.0f, true);
                    }
                    sender.sendSystemMessage(Component.literal("§a[Jumpscare] Suara Fake Step dikirim ke SEMUA pemain!"));
                } else {
                    ServerPlayer targetPlayer = sender.getServer().getPlayerList().getPlayerByName(targetName);
                    if (targetPlayer != null) {
                        JumpscareNetworking.sendAudioScarePacket(targetPlayer, "fakestep", "", 1.0f, 1.0f, true);
                        sender.sendSystemMessage(Component.literal("§a[Jumpscare] Fake step dimainkan di belakang " + targetPlayer.getScoreboardName() + "!"));
                    } else {
                        sender.sendSystemMessage(Component.literal("§c[Jumpscare] Player '" + targetName + "' tidak ditemukan!"));
                    }
                }
            } else {
                sender.sendSystemMessage(Component.literal("§e[Jumpscare] Penggunaan: !fakestep <player/@a>"));
            }
            return;
        }

        // 2. Custom Audio / Sound chat trigger (!audio <target> [soundId] [behind])
        if (message.startsWith("!audio ") || message.startsWith("#audio ") || message.startsWith("!sound ") || message.startsWith("#sound ")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2) {
                String targetName = parts[1];
                String soundId = (parts.length >= 3) ? parts[2] : "fakestep";
                boolean behind = (parts.length >= 4) ? Boolean.parseBoolean(parts[3]) : soundId.equalsIgnoreCase("fakestep");

                if ("@a".equalsIgnoreCase(targetName)) {
                    for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
                        JumpscareNetworking.sendAudioScarePacket(p, soundId, "", 1.0f, 1.0f, behind);
                    }
                    sender.sendSystemMessage(Component.literal("§a[Jumpscare] Audio '" + soundId + "' dikirim ke SEMUA pemain! (Behind: " + behind + ")"));
                } else {
                    ServerPlayer targetPlayer = sender.getServer().getPlayerList().getPlayerByName(targetName);
                    if (targetPlayer != null) {
                        JumpscareNetworking.sendAudioScarePacket(targetPlayer, soundId, "", 1.0f, 1.0f, behind);
                        sender.sendSystemMessage(Component.literal("§a[Jumpscare] Audio '" + soundId + "' dikirim ke " + targetPlayer.getScoreboardName() + "! (Behind: " + behind + ")"));
                    } else {
                        sender.sendSystemMessage(Component.literal("§c[Jumpscare] Player '" + targetName + "' tidak ditemukan!"));
                    }
                }
            } else {
                sender.sendSystemMessage(Component.literal("§e[Jumpscare] Penggunaan: !audio <player/@a> [soundId/link] [behind: true/false]"));
            }
            return;
        }

        // 3. Normal jumpscare chat trigger
        if (message.startsWith("/jumpscare ") || message.startsWith("/scare ") || message.startsWith("!jumpscare ") || message.startsWith("#jumpscare ")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2) {
                // Check if user typed "/jumpscare fakestep <player>" or "/jumpscare audio <player> [sound]" in regular chat
                if (parts[1].equalsIgnoreCase("fakestep") && parts.length >= 3) {
                    String targetName = parts[2];
                    ServerPlayer targetPlayer = sender.getServer().getPlayerList().getPlayerByName(targetName);
                    if (targetPlayer != null) {
                        JumpscareNetworking.sendAudioScarePacket(targetPlayer, "fakestep", "", 1.0f, 1.0f, true);
                        sender.sendSystemMessage(Component.literal("§a[Jumpscare] Fake step dimainkan di belakang " + targetPlayer.getScoreboardName() + "!"));
                    }
                    return;
                } else if (parts[1].equalsIgnoreCase("audio") && parts.length >= 3) {
                    String targetName = parts[2];
                    String soundId = (parts.length >= 4) ? parts[3] : "fakestep";
                    ServerPlayer targetPlayer = sender.getServer().getPlayerList().getPlayerByName(targetName);
                    if (targetPlayer != null) {
                        JumpscareNetworking.sendAudioScarePacket(targetPlayer, soundId, "", 1.0f, 1.0f, soundId.equalsIgnoreCase("fakestep"));
                        sender.sendSystemMessage(Component.literal("§a[Jumpscare] Audio '" + soundId + "' dikirim ke " + targetPlayer.getScoreboardName() + "!"));
                    }
                    return;
                }

                String targetName = parts[1];
                String jumpscareId = (parts.length >= 3) ? parts[2] : "spooky";

                ServerPlayer targetPlayer = sender.getServer().getPlayerList().getPlayerByName(targetName);
                if (targetPlayer != null) {
                    boolean success = JumpscareNetworking.sendJumpscarePacket(targetPlayer, jumpscareId, 2000, 1.2f);
                    if (success) {
                        sender.sendSystemMessage(Component.literal("§a[Jumpscare] Successfully sent jumpscare '" + jumpscareId + "' to " + targetPlayer.getScoreboardName() + "!"));
                    } else {
                        sender.sendSystemMessage(Component.literal("§c[Jumpscare] Failed to send jumpscare to " + targetName + "."));
                    }
                } else {
                    sender.sendSystemMessage(Component.literal("§c[Jumpscare] Player '" + targetName + "' not found online!"));
                }
            } else {
                sender.sendSystemMessage(Component.literal("§e[Jumpscare] Usage: /jumpscare <player> [jumpscare_id]"));
            }
            return;
        }

        // 4. Ghost team chat trigger (!ghost <player>, /ghost <player>, !unghost <player>, /unghost <player>)
        if (message.startsWith("!ghost ") || message.startsWith("#ghost ") || message.startsWith("/ghost ") ||
            message.equalsIgnoreCase("!ghost") || message.equalsIgnoreCase("#ghost") || message.equalsIgnoreCase("/ghost")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2) {
                if (parts[1].equalsIgnoreCase("select") || parts[1].equalsIgnoreCase("set")) {
                    if (parts.length >= 3) {
                        GhostCommand.executeSetGhostAttack(sender.createCommandSourceStack(), parts[2]);
                    } else {
                        GhostCommand.executeShowSelectedJumpscare(sender.createCommandSourceStack());
                    }
                    return;
                }
                String targetName = parts[1];
                if ("@a".equalsIgnoreCase(targetName)) {
                    for (ServerPlayer p : sender.getServer().getPlayerList().getPlayers()) {
                        GhostCommand.addPlayerByName(sender.getServer(), p.getScoreboardName());
                    }
                    sender.sendSystemMessage(Component.literal("§a[Ghost] Semua pemain telah dimasukkan ke team Ghost!"));
                } else {
                    boolean success = GhostCommand.addPlayerByName(sender.getServer(), targetName);
                    if (success) {
                        sender.sendSystemMessage(Component.literal("§a[Ghost] Player '" + targetName + "' berhasil dimasukkan ke team Ghost!"));
                    } else {
                        sender.sendSystemMessage(Component.literal("§c[Ghost] Gagal memasukkan '" + targetName + "' ke team Ghost."));
                    }
                }
            } else {
                GhostCommand.addPlayerByName(sender.getServer(), sender.getScoreboardName());
                sender.sendSystemMessage(Component.literal("§a[Ghost] Anda telah bergabung ke team Ghost!"));
            }
            return;
        }

        if (message.startsWith("!unghost ") || message.startsWith("#unghost ") || message.startsWith("/unghost ") ||
            message.equalsIgnoreCase("!unghost") || message.equalsIgnoreCase("#unghost") || message.equalsIgnoreCase("/unghost")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2) {
                String targetName = parts[1];
                if ("@a".equalsIgnoreCase(targetName)) {
                    GhostCommand.clearGhostTeam(sender.createCommandSourceStack());
                } else {
                    boolean success = GhostCommand.removePlayerByName(sender.getServer(), targetName);
                    if (success) {
                        sender.sendSystemMessage(Component.literal("§e[Ghost] Player '" + targetName + "' dikeluarkan dari team Ghost."));
                    } else {
                        sender.sendSystemMessage(Component.literal("§c[Ghost] Player '" + targetName + "' tidak ada di team Ghost."));
                    }
                }
            } else {
                boolean success = GhostCommand.removePlayerByName(sender.getServer(), sender.getScoreboardName());
                if (success) {
                    sender.sendSystemMessage(Component.literal("§e[Ghost] Anda telah keluar dari team Ghost."));
                } else {
                    sender.sendSystemMessage(Component.literal("§c[Ghost] Anda tidak sedang berada di team Ghost."));
                }
            }
            return;
        }
    }
}
