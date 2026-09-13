package com.elestial.jumpscare.command;

import com.elestial.jumpscare.client.ClientGuiHelper;
import com.elestial.jumpscare.config.JumpscareServerConfig;
import com.elestial.jumpscare.network.JumpscareNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Collection;
import java.util.Map;

public class JumpscareCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("jumpscare")
            .executes(ctx -> executeOpenGui(ctx.getSource()))
            .then(Commands.literal("gui")
                .executes(ctx -> executeOpenGui(ctx.getSource()))
            )
            .then(Commands.literal("menu")
                .executes(ctx -> executeOpenGui(ctx.getSource()))
            )
            .then(Commands.literal("audio")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeFakeStep(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                    .then(Commands.argument("soundId", StringArgumentType.string())
                        .executes(ctx -> executeAudio(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "soundId"), "", 1.0f, 1.0f, false))
                        .then(Commands.argument("behind", BoolArgumentType.bool())
                            .executes(ctx -> executeAudio(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "soundId"), "", 1.0f, 1.0f, BoolArgumentType.getBool(ctx, "behind")))
                            .then(Commands.argument("soundUrl", StringArgumentType.string())
                                .executes(ctx -> executeAudio(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "soundId"), StringArgumentType.getString(ctx, "soundUrl"), 1.0f, 1.0f, BoolArgumentType.getBool(ctx, "behind")))
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("fakestep")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeFakeStep(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                )
            )
            .then(Commands.literal("sound")
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("soundId", StringArgumentType.string())
                        .executes(ctx -> executeAudio(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "soundId"), "", 1.0f, 1.0f, false))
                    )
                )
            )
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), "spooky", 2000, 1.0f))
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "id"), 2000, 1.0f))
                    .then(Commands.argument("durationMs", IntegerArgumentType.integer(500, 10000))
                        .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "durationMs"), 1.0f))
                        .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.1f, 5.0f))
                            .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "id"), IntegerArgumentType.getInteger(ctx, "durationMs"), FloatArgumentType.getFloat(ctx, "intensity")))
                        )
                    )
                )
            )
            .then(Commands.literal("trigger")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), "spooky", 2000, 1.0f))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "id"), 2000, 1.0f))
                    )
                )
            )
            .then(Commands.literal("add")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.string())
                    .then(Commands.argument("imageUrl", StringArgumentType.string())
                        .then(Commands.argument("soundUrl", StringArgumentType.string())
                            .executes(ctx -> executeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "imageUrl"), StringArgumentType.getString(ctx, "soundUrl"), 2500, 1.2f))
                            .then(Commands.argument("durationMs", IntegerArgumentType.integer(500, 10000))
                                .executes(ctx -> executeAdd(ctx.getSource(), StringArgumentType.getString(ctx, "id"), StringArgumentType.getString(ctx, "imageUrl"), StringArgumentType.getString(ctx, "soundUrl"), IntegerArgumentType.getInteger(ctx, "durationMs"), 1.2f))
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("remove")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> executeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                )
            )
            .then(Commands.literal("delete")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> executeRemove(ctx.getSource(), StringArgumentType.getString(ctx, "id")))
                )
            )
            .then(Commands.literal("control")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("action", StringArgumentType.string())
                        .executes(ctx -> executeControl(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "action"), 3000))
                        .then(Commands.argument("durationMs", IntegerArgumentType.integer(500, 30000))
                            .executes(ctx -> executeControl(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "action"), IntegerArgumentType.getInteger(ctx, "durationMs")))
                        )
                    )
                )
            )
            .then(Commands.literal("mindcontrol")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("targets", EntityArgument.players())
                    .then(Commands.argument("action", StringArgumentType.string())
                        .executes(ctx -> executeControl(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "action"), 3000))
                        .then(Commands.argument("durationMs", IntegerArgumentType.integer(500, 30000))
                            .executes(ctx -> executeControl(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "action"), IntegerArgumentType.getInteger(ctx, "durationMs")))
                        )
                    )
                )
            )
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> executeReload(ctx.getSource()))
            )
            .then(Commands.literal("list")
                .executes(ctx -> executeList(ctx.getSource()))
            )
            .then(Commands.literal("ghost")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> GhostCommand.addPlayersToGhost(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                )
            )
            .then(Commands.literal("unghost")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> GhostCommand.removePlayersFromGhost(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                )
            )
        );

        dispatcher.register(Commands.literal("scare")
            .executes(ctx -> executeOpenGui(ctx.getSource()))
            .then(Commands.literal("audio")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeFakeStep(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                    .then(Commands.argument("soundId", StringArgumentType.string())
                        .executes(ctx -> executeAudio(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "soundId"), "", 1.0f, 1.0f, false))
                    )
                )
            )
            .then(Commands.literal("fakestep")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> executeFakeStep(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                )
            )
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), "spooky", 2000, 1.0f))
                .then(Commands.argument("id", StringArgumentType.string())
                    .executes(ctx -> executeTrigger(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "id"), 2000, 1.0f))
                )
            )
        );
    }

    private static int executeAudio(CommandSourceStack source, Collection<ServerPlayer> targets, String soundId, String soundUrl, float volume, float pitch, boolean behindPlayer) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (JumpscareNetworking.sendAudioScarePacket(target, soundId, soundUrl, volume, pitch, behindPlayer)) {
                count++;
            }
        }
        final int c = count;
        source.sendSuccess(() -> Component.literal("§a[Jumpscare] Sent audio scare '" + soundId + "' to " + c + " player(s)! (Behind: " + behindPlayer + ")"), true);
        return count;
    }

    private static int executeFakeStep(CommandSourceStack source, Collection<ServerPlayer> targets) {
        return executeAudio(source, targets, "fakestep", "", 1.0f, 1.0f, true);
    }

    private static int executeOpenGui(CommandSourceStack source) {
        try {
            if (source.getPlayer() != null) {
                JumpscareNetworking.sendOpenGuiPacket(source.getPlayer());
            } else if (FMLEnvironment.dist.isClient()) {
                ClientGuiHelper.openScreen();
            }
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cFailed to open GUI screen."));
        }
        return 1;
    }

    private static int executeTrigger(CommandSourceStack source, Collection<ServerPlayer> targets, String jumpscareId, int durationMs, float intensity) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (JumpscareNetworking.sendJumpscarePacket(target, jumpscareId, durationMs, intensity)) {
                count++;
            }
        }
        return count;
    }

    private static int executeAdd(CommandSourceStack source, String id, String imageUrl, String soundUrl, int durationMs, float intensity) {
        JumpscareServerConfig.addEntry(id, imageUrl, soundUrl, durationMs, intensity);
        if (source.getServer() != null) {
            JumpscareNetworking.syncConfigToAll(source.getServer());
        }
        source.sendSuccess(() -> Component.literal("§a[Jumpscare] Added server jumpscare entry '" + id + "'!"), true);
        return 1;
    }

    private static int executeRemove(CommandSourceStack source, String id) {
        boolean removed = JumpscareServerConfig.removeEntry(id);
        if (removed) {
            if (source.getServer() != null) {
                JumpscareNetworking.syncConfigToAll(source.getServer());
            }
            source.sendSuccess(() -> Component.literal("§a[Jumpscare] Removed server jumpscare entry '" + id + "'!"), true);
            return 1;
        } else {
            source.sendFailure(Component.literal("§c[Jumpscare] Entry '" + id + "' not found on server."));
            return 0;
        }
    }

    private static int executeControl(CommandSourceStack source, Collection<ServerPlayer> targets, String action, int durationMs) {
        int count = 0;
        for (ServerPlayer target : targets) {
            if (JumpscareNetworking.sendMindControlPacket(target, action, durationMs)) {
                count++;
            }
        }
        return count;
    }

    private static int executeReload(CommandSourceStack source) {
        JumpscareServerConfig.loadConfig();
        if (source.getServer() != null) {
            JumpscareNetworking.syncConfigToAll(source.getServer());
        }
        if (source.getPlayer() != null) {
            JumpscareNetworking.sendReloadPacket(source.getPlayer());
        }
        source.sendSuccess(() -> Component.literal("§a[Jumpscare] Reloaded server config and synced to all clients!"), true);
        return 1;
    }

    private static int executeList(CommandSourceStack source) {
        Map<String, JumpscareServerConfig.JumpscareEntry> entries = JumpscareServerConfig.getAllEntries();
        source.sendSuccess(() -> Component.literal("§e[Jumpscare] Server Config Jumpscares (" + entries.size() + "):"), false);
        for (String id : entries.keySet()) {
            final String entryId = id;
            source.sendSuccess(() -> Component.literal(" - §f" + entryId), false);
        }
        source.sendSuccess(() -> Component.literal("§eUse /jumpscare add <id> <imageUrl> <soundUrl> to add new remote assets."), false);
        return 1;
    }
}
