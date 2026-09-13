package com.elestial.jumpscare.command;

import com.elestial.jumpscare.config.JumpscareServerConfig;
import com.elestial.jumpscare.event.GhostAttackHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GhostCommand {
    public static final String GHOST_TEAM_NAME = "Ghost";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ghost")
            .executes(ctx -> executeAddSelf(ctx.getSource()))
            .then(Commands.literal("add")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> addPlayersToGhost(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                )
                .then(Commands.argument("playerName", StringArgumentType.word())
                    .executes(ctx -> executeAddByName(ctx.getSource(), StringArgumentType.getString(ctx, "playerName")))
                )
            )
            .then(Commands.literal("remove")
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> removePlayersFromGhost(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
                )
                .then(Commands.argument("playerName", StringArgumentType.word())
                    .executes(ctx -> executeRemoveByName(ctx.getSource(), StringArgumentType.getString(ctx, "playerName")))
                )
            )
            .then(Commands.literal("leave")
                .executes(ctx -> executeRemoveSelf(ctx.getSource()))
            )
            .then(Commands.literal("list")
                .executes(ctx -> listGhostPlayers(ctx.getSource()))
            )
            .then(Commands.literal("clear")
                .executes(ctx -> clearGhostTeam(ctx.getSource()))
            )
            .then(Commands.literal("select")
                .executes(ctx -> executeShowSelectedJumpscare(ctx.getSource()))
                .then(Commands.argument("jumpscareId", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("random");
                        for (String id : JumpscareServerConfig.getAllEntries().keySet()) {
                            builder.suggest(id);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> executeSetGhostAttack(ctx.getSource(), StringArgumentType.getString(ctx, "jumpscareId")))
                )
            )
            .then(Commands.literal("set")
                .executes(ctx -> executeShowSelectedJumpscare(ctx.getSource()))
                .then(Commands.argument("jumpscareId", StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        builder.suggest("random");
                        for (String id : JumpscareServerConfig.getAllEntries().keySet()) {
                            builder.suggest(id);
                        }
                        return builder.buildFuture();
                    })
                    .executes(ctx -> executeSetGhostAttack(ctx.getSource(), StringArgumentType.getString(ctx, "jumpscareId")))
                )
            )
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> addPlayersToGhost(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
            )
            .then(Commands.argument("playerName", StringArgumentType.word())
                .executes(ctx -> executeAddByName(ctx.getSource(), StringArgumentType.getString(ctx, "playerName")))
            )
        );

        dispatcher.register(Commands.literal("unghost")
            .executes(ctx -> executeRemoveSelf(ctx.getSource()))
            .then(Commands.argument("targets", EntityArgument.players())
                .executes(ctx -> removePlayersFromGhost(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))
            )
            .then(Commands.argument("playerName", StringArgumentType.word())
                .executes(ctx -> executeRemoveByName(ctx.getSource(), StringArgumentType.getString(ctx, "playerName")))
            )
        );
    }

    public static PlayerTeam getOrCreateGhostTeam(Scoreboard scoreboard) {
        PlayerTeam team = scoreboard.getPlayerTeam(GHOST_TEAM_NAME);
        if (team == null) {
            team = scoreboard.addPlayerTeam(GHOST_TEAM_NAME);
            team.setDisplayName(Component.literal("Ghost"));
            team.setColor(ChatFormatting.DARK_GRAY);
            team.setSeeFriendlyInvisibles(true);
        }
        return team;
    }

    public static boolean isGhost(ServerPlayer player) {
        if (player == null || player.getServer() == null) return false;
        Scoreboard scoreboard = player.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(GHOST_TEAM_NAME);
        if (team == null) return false;
        return team.getPlayers().contains(player.getScoreboardName());
    }

    public static int addPlayersToGhost(CommandSourceStack source, Collection<ServerPlayer> targets) {
        MinecraftServer server = source.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = getOrCreateGhostTeam(scoreboard);

        int count = 0;
        for (ServerPlayer player : targets) {
            String name = player.getScoreboardName();
            scoreboard.addPlayerToTeam(name, team);
            player.sendSystemMessage(Component.literal("§7[Ghost] §aKamu telah dimasukkan ke team §8Ghost§a!"));
            count++;
        }
        final int c = count;
        source.sendSuccess(() -> Component.literal("§a[Ghost] Berhasil memasukkan " + c + " pemain ke team Ghost!"), true);
        return count;
    }

    public static boolean addPlayerByName(MinecraftServer server, String playerName) {
        if (server == null || playerName == null || playerName.trim().isEmpty()) return false;
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = getOrCreateGhostTeam(scoreboard);
        String cleanName = playerName.trim();
        scoreboard.addPlayerToTeam(cleanName, team);

        ServerPlayer online = server.getPlayerList().getPlayerByName(cleanName);
        if (online != null) {
            online.sendSystemMessage(Component.literal("§7[Ghost] §aKamu telah dimasukkan ke team §8Ghost§a!"));
        }
        return true;
    }

    public static int removePlayersFromGhost(CommandSourceStack source, Collection<ServerPlayer> targets) {
        MinecraftServer server = source.getServer();
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(GHOST_TEAM_NAME);
        if (team == null) {
            source.sendFailure(Component.literal("§c[Ghost] Team Ghost belum pernah dibuat."));
            return 0;
        }

        int count = 0;
        for (ServerPlayer player : targets) {
            String name = player.getScoreboardName();
            if (team.getPlayers().contains(name)) {
                scoreboard.removePlayerFromTeam(name, team);
                player.sendSystemMessage(Component.literal("§7[Ghost] §eKamu telah dikeluarkan dari team §8Ghost§e."));
                count++;
            }
        }
        final int c = count;
        source.sendSuccess(() -> Component.literal("§e[Ghost] Mengeluarkan " + c + " pemain dari team Ghost."), true);
        return count;
    }

    public static boolean removePlayerByName(MinecraftServer server, String playerName) {
        if (server == null || playerName == null || playerName.trim().isEmpty()) return false;
        Scoreboard scoreboard = server.getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(GHOST_TEAM_NAME);
        if (team == null) return false;

        String cleanName = playerName.trim();
        if (team.getPlayers().contains(cleanName)) {
            scoreboard.removePlayerFromTeam(cleanName, team);
            ServerPlayer online = server.getPlayerList().getPlayerByName(cleanName);
            if (online != null) {
                online.sendSystemMessage(Component.literal("§7[Ghost] §eKamu telah dikeluarkan dari team §8Ghost§e."));
            }
            return true;
        }
        return false;
    }

    private static int executeAddSelf(CommandSourceStack source) {
        if (source.getPlayer() != null) {
            return addPlayersToGhost(source, Collections.singletonList(source.getPlayer()));
        } else {
            source.sendFailure(Component.literal("§c[Ghost] Command ini harus dijalankan oleh pemain in-game!"));
            return 0;
        }
    }

    private static int executeRemoveSelf(CommandSourceStack source) {
        if (source.getPlayer() != null) {
            return removePlayersFromGhost(source, Collections.singletonList(source.getPlayer()));
        } else {
            source.sendFailure(Component.literal("§c[Ghost] Command ini harus dijalankan oleh pemain in-game!"));
            return 0;
        }
    }

    private static int executeAddByName(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        if (server != null && addPlayerByName(server, playerName)) {
            source.sendSuccess(() -> Component.literal("§a[Ghost] Player '" + playerName + "' berhasil dimasukkan ke team Ghost!"), true);
            return 1;
        }
        source.sendFailure(Component.literal("§c[Ghost] Gagal memasukkan '" + playerName + "' ke team Ghost."));
        return 0;
    }

    private static int executeRemoveByName(CommandSourceStack source, String playerName) {
        MinecraftServer server = source.getServer();
        if (server != null && removePlayerByName(server, playerName)) {
            source.sendSuccess(() -> Component.literal("§e[Ghost] Player '" + playerName + "' berhasil dikeluarkan dari team Ghost!"), true);
            return 1;
        }
        source.sendFailure(Component.literal("§c[Ghost] Player '" + playerName + "' tidak ditemukan di team Ghost."));
        return 0;
    }

    public static int listGhostPlayers(CommandSourceStack source) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(GHOST_TEAM_NAME);
        if (team == null || team.getPlayers().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7[Ghost] §eSaat ini tidak ada pemain di team Ghost."), false);
            return 0;
        }
        Collection<String> players = team.getPlayers();
        source.sendSuccess(() -> Component.literal("§7[Ghost] §6Anggota Team Ghost (" + players.size() + "): §f" + String.join(", ", players)), false);
        return players.size();
    }

    public static int clearGhostTeam(CommandSourceStack source) {
        Scoreboard scoreboard = source.getServer().getScoreboard();
        PlayerTeam team = scoreboard.getPlayerTeam(GHOST_TEAM_NAME);
        if (team == null || team.getPlayers().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7[Ghost] §eTeam Ghost sudah kosong."), false);
            return 0;
        }
        List<String> toRemove = new ArrayList<>(team.getPlayers());
        for (String p : toRemove) {
            scoreboard.removePlayerFromTeam(p, team);
        }
        source.sendSuccess(() -> Component.literal("§a[Ghost] Seluruh anggota team Ghost (" + toRemove.size() + " pemain) telah dikosongkan!"), true);
        return toRemove.size();
    }

    public static int executeSetGhostAttack(CommandSourceStack source, String jumpscareId) {
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("§cCommand ini harus dijalankan oleh pemain!"));
            return 0;
        }
        String cleanId = jumpscareId.trim().toLowerCase();
        if (cleanId.equalsIgnoreCase("random")) {
            GhostAttackHandler.setGhostAttackJumpscare(source.getPlayer().getUUID(), "random");
            source.sendSuccess(() -> Component.literal("§a[Ghost] Jumpscare serangan Anda diatur ke mode: §eRANDOM §a(acak setiap hit)!"), false);
            return 1;
        }
        if (JumpscareServerConfig.getEntry(cleanId) == null && !cleanId.equalsIgnoreCase("spooky")) {
            source.sendFailure(Component.literal("§c[Ghost] Jumpscare ID '" + cleanId + "' belum terdaftar di server. Gunakan /jumpscare list untuk melihat daftar ID."));
            return 0;
        }
        GhostAttackHandler.setGhostAttackJumpscare(source.getPlayer().getUUID(), cleanId);
        source.sendSuccess(() -> Component.literal("§a[Ghost] Jumpscare serangan Anda berhasil diatur ke: §e" + cleanId + "§a!"), false);
        return 1;
    }

    public static int executeShowSelectedJumpscare(CommandSourceStack source) {
        if (source.getPlayer() == null) {
            source.sendFailure(Component.literal("§cCommand ini harus dijalankan oleh pemain!"));
            return 0;
        }
        String cur = GhostAttackHandler.getGhostAttackJumpscare(source.getPlayer().getUUID());
        if (cur == null || cur.isEmpty()) cur = "random (default)";
        final String selected = cur;
        source.sendSuccess(() -> Component.literal("§7[Ghost] Jumpscare serangan aktif Anda saat ini: §e" + selected), false);
        return 1;
    }
}
