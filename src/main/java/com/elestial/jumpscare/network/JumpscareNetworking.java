package com.elestial.jumpscare.network;

import com.elestial.jumpscare.JumpscareMod;
import com.elestial.jumpscare.asset.JumpscareAssetManager;
import com.elestial.jumpscare.client.ClientConfigSync;
import com.elestial.jumpscare.client.ClientGuiHelper;
import com.elestial.jumpscare.config.JumpscareServerConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class JumpscareNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(JumpscareMod.MOD_ID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(JumpscarePacket.class, packetId++)
            .encoder(JumpscarePacket::encode)
            .decoder(JumpscarePacket::decode)
            .consumerMainThread(JumpscarePacket::handle)
            .add();

        CHANNEL.messageBuilder(ReloadPacket.class, packetId++)
            .encoder(ReloadPacket::encode)
            .decoder(ReloadPacket::decode)
            .consumerMainThread(ReloadPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenGuiPacket.class, packetId++)
            .encoder(OpenGuiPacket::encode)
            .decoder(OpenGuiPacket::decode)
            .consumerMainThread(OpenGuiPacket::handle)
            .add();

        CHANNEL.messageBuilder(OpenFolderPacket.class, packetId++)
            .encoder(OpenFolderPacket::encode)
            .decoder(OpenFolderPacket::decode)
            .consumerMainThread(OpenFolderPacket::handle)
            .add();

        CHANNEL.messageBuilder(AddJumpscareC2SPacket.class, packetId++)
            .encoder(AddJumpscareC2SPacket::encode)
            .decoder(AddJumpscareC2SPacket::decode)
            .consumerMainThread(AddJumpscareC2SPacket::handle)
            .add();

        CHANNEL.messageBuilder(RemoveJumpscareC2SPacket.class, packetId++)
            .encoder(RemoveJumpscareC2SPacket::encode)
            .decoder(RemoveJumpscareC2SPacket::decode)
            .consumerMainThread(RemoveJumpscareC2SPacket::handle)
            .add();

        CHANNEL.messageBuilder(SyncConfigPacket.class, packetId++)
            .encoder(SyncConfigPacket::encode)
            .decoder(SyncConfigPacket::decode)
            .consumerMainThread(SyncConfigPacket::handle)
            .add();

        CHANNEL.messageBuilder(MindControlPacket.class, packetId++)
            .encoder(MindControlPacket::encode)
            .decoder(MindControlPacket::decode)
            .consumerMainThread(MindControlPacket::handle)
            .add();

        CHANNEL.messageBuilder(PossessionInputPacket.class, packetId++)
            .encoder(PossessionInputPacket::encode)
            .decoder(PossessionInputPacket::decode)
            .consumerMainThread(PossessionInputPacket::handle)
            .add();

        CHANNEL.messageBuilder(PossessionStatusPacket.class, packetId++)
            .encoder(PossessionStatusPacket::encode)
            .decoder(PossessionStatusPacket::decode)
            .consumerMainThread(PossessionStatusPacket::handle)
            .add();

        CHANNEL.messageBuilder(ClientPossessionMovePacket.class, packetId++)
            .encoder(ClientPossessionMovePacket::encode)
            .decoder(ClientPossessionMovePacket::decode)
            .consumerMainThread(ClientPossessionMovePacket::handle)
            .add();

        CHANNEL.messageBuilder(BatchJumpscareC2SPacket.class, packetId++)
            .encoder(BatchJumpscareC2SPacket::encode)
            .decoder(BatchJumpscareC2SPacket::decode)
            .consumerMainThread(BatchJumpscareC2SPacket::handle)
            .add();

        CHANNEL.messageBuilder(AudioScarePacket.class, packetId++)
            .encoder(AudioScarePacket::encode)
            .decoder(AudioScarePacket::decode)
            .consumerMainThread(AudioScarePacket::handle)
            .add();

        CHANNEL.messageBuilder(TriggerAudioC2SPacket.class, packetId++)
            .encoder(TriggerAudioC2SPacket::encode)
            .decoder(TriggerAudioC2SPacket::decode)
            .consumerMainThread(TriggerAudioC2SPacket::handle)
            .add();
    }

    public static boolean sendAudioScarePacket(ServerPlayer targetPlayer, String soundId, String soundUrl, float volume, float pitch, boolean behindPlayer) {
        if (targetPlayer == null) return false;
        String id = (soundId != null && !soundId.isEmpty()) ? soundId.trim() : "fakestep";
        String url = (soundUrl != null) ? soundUrl.trim() : "";
        if (url.isEmpty()) {
            JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(id);
            if (entry != null && entry.soundUrl != null && !entry.soundUrl.isEmpty()) {
                url = entry.soundUrl;
            }
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer), new AudioScarePacket(id, url, volume, pitch, behindPlayer));
        return true;
    }

    public static void sendTriggerAudioPacket(String targetName, String soundId, String soundUrl, float volume, float pitch, boolean behindPlayer) {
        CHANNEL.sendToServer(new TriggerAudioC2SPacket(targetName, soundId, soundUrl, volume, pitch, behindPlayer));
    }

    public static void sendBatchJumpscarePacket(Map<String, String> assignments) {
        CHANNEL.sendToServer(new BatchJumpscareC2SPacket(assignments));
    }

    public static void sendPossessionInputPacket(String targetName, float yaw, float pitch, float forward, float strafe, boolean jumping, boolean sprinting, boolean attacking, boolean stop) {
        CHANNEL.sendToServer(new PossessionInputPacket(targetName, yaw, pitch, forward, strafe, jumping, sprinting, attacking, stop));
    }

    public static void sendRemoveJumpscarePacket(String id) {
        CHANNEL.sendToServer(new RemoveJumpscareC2SPacket(id));
    }

    public static class RemoveJumpscareC2SPacket {
        private final String id;

        public RemoveJumpscareC2SPacket(String id) {
            this.id = id;
        }

        public static void encode(RemoveJumpscareC2SPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.id, 256);
        }

        public static RemoveJumpscareC2SPacket decode(FriendlyByteBuf buf) {
            return new RemoveJumpscareC2SPacket(buf.readUtf(256));
        }

        public static void handle(RemoveJumpscareC2SPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    boolean removed = JumpscareServerConfig.removeEntry(pkt.id);
                    if (removed) {
                        syncConfigToAll(sender.getServer());
                        sender.sendSystemMessage(Component.literal("§a[Jumpscare] Successfully deleted entry '" + pkt.id + "' from server!"));
                    } else {
                        sender.sendSystemMessage(Component.literal("§c[Jumpscare] Entry '" + pkt.id + "' not found on server."));
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static void syncConfigToPlayer(ServerPlayer player) {
        if (player == null) return;
        List<SyncConfigPacket.EntryPayload> list = new ArrayList<>();
        for (Map.Entry<String, JumpscareServerConfig.JumpscareEntry> e : JumpscareServerConfig.getAllEntries().entrySet()) {
            list.add(new SyncConfigPacket.EntryPayload(e.getKey(), e.getValue().imageUrl, e.getValue().soundUrl, e.getValue().durationMs, e.getValue().intensity));
        }
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigPacket(list));
    }

    public static void syncConfigToAll(MinecraftServer server) {
        if (server == null) return;
        List<SyncConfigPacket.EntryPayload> list = new ArrayList<>();
        for (Map.Entry<String, JumpscareServerConfig.JumpscareEntry> e : JumpscareServerConfig.getAllEntries().entrySet()) {
            list.add(new SyncConfigPacket.EntryPayload(e.getKey(), e.getValue().imageUrl, e.getValue().soundUrl, e.getValue().durationMs, e.getValue().intensity));
        }
        CHANNEL.send(PacketDistributor.ALL.noArg(), new SyncConfigPacket(list));
    }

    public static boolean sendJumpscarePacket(ServerPlayer targetPlayer, String jumpscareId, int defaultDurationMs, float defaultIntensity) {
        if (targetPlayer == null) return false;

        String id = (jumpscareId != null && !jumpscareId.isEmpty()) ? jumpscareId.toLowerCase() : "spooky";

        String imageUrl = "";
        String soundUrl = "";
        int durationMs = defaultDurationMs;
        float intensity = defaultIntensity;

        JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(id);
        if (entry != null) {
            if (entry.imageUrl != null) imageUrl = entry.imageUrl;
            if (entry.soundUrl != null) soundUrl = entry.soundUrl;
            if (entry.durationMs > 0) durationMs = entry.durationMs;
            if (entry.intensity > 0) intensity = entry.intensity;
        }

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> targetPlayer), new JumpscarePacket(id, imageUrl, soundUrl, durationMs, intensity));
        return true;
    }

    public static void sendReloadPacket(ServerPlayer player) {
        if (player == null) return;
        JumpscareServerConfig.loadConfig();
        syncConfigToPlayer(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ReloadPacket());
    }

    public static void sendOpenGuiPacket(ServerPlayer player) {
        if (player == null) return;
        syncConfigToPlayer(player);
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenGuiPacket());
    }

    public static void sendOpenFolderPacket(ServerPlayer player) {
        if (player == null) return;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new OpenFolderPacket());
    }

    public static void sendAddJumpscarePacket(String id, String imageUrl, String soundUrl, int durationMs, float intensity) {
        CHANNEL.sendToServer(new AddJumpscareC2SPacket(id, imageUrl, soundUrl, durationMs, intensity));
    }

    public static class JumpscarePacket {
        private final String jumpscareId;
        private final String imageUrl;
        private final String soundUrl;
        private final int durationMs;
        private final float intensity;

        public JumpscarePacket(String jumpscareId, String imageUrl, String soundUrl, int durationMs, float intensity) {
            this.jumpscareId = jumpscareId;
            this.imageUrl = imageUrl;
            this.soundUrl = soundUrl;
            this.durationMs = durationMs;
            this.intensity = intensity;
        }

        public static void encode(JumpscarePacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.jumpscareId, 256);
            buf.writeUtf(pkt.imageUrl, 32767);
            buf.writeUtf(pkt.soundUrl, 32767);
            buf.writeInt(pkt.durationMs);
            buf.writeFloat(pkt.intensity);
        }

        public static JumpscarePacket decode(FriendlyByteBuf buf) {
            return new JumpscarePacket(buf.readUtf(256), buf.readUtf(32767), buf.readUtf(32767), buf.readInt(), buf.readFloat());
        }

        public static void handle(JumpscarePacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    JumpscareAssetManager.getInstance().fetchAndTrigger(pkt.jumpscareId, pkt.imageUrl, pkt.soundUrl, pkt.durationMs, pkt.intensity);
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class ReloadPacket {
        public ReloadPacket() {}

        public static void encode(ReloadPacket pkt, FriendlyByteBuf buf) {}

        public static ReloadPacket decode(FriendlyByteBuf buf) {
            return new ReloadPacket();
        }

        public static void handle(ReloadPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    JumpscareAssetManager.getInstance().reloadAssets();
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class OpenGuiPacket {
        public OpenGuiPacket() {}

        public static void encode(OpenGuiPacket pkt, FriendlyByteBuf buf) {}

        public static OpenGuiPacket decode(FriendlyByteBuf buf) {
            return new OpenGuiPacket();
        }

        public static void handle(OpenGuiPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientGuiHelper.openScreen();
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class OpenFolderPacket {
        public OpenFolderPacket() {}

        public static void encode(OpenFolderPacket pkt, FriendlyByteBuf buf) {}

        public static OpenFolderPacket decode(FriendlyByteBuf buf) {
            return new OpenFolderPacket();
        }

        public static void handle(OpenFolderPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientGuiHelper.openFolder();
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static void sendReloadPacketToAll(MinecraftServer server) {
        if (server == null) return;
        CHANNEL.send(PacketDistributor.ALL.noArg(), new ReloadPacket());
    }

    public static class AddJumpscareC2SPacket {
        private final String id;
        private final String imageUrl;
        private final String soundUrl;
        private final int durationMs;
        private final float intensity;

        public AddJumpscareC2SPacket(String id, String imageUrl, String soundUrl, int durationMs, float intensity) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.soundUrl = soundUrl;
            this.durationMs = durationMs;
            this.intensity = intensity;
        }

        public static void encode(AddJumpscareC2SPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.id, 256);
            buf.writeUtf(pkt.imageUrl, 32767);
            buf.writeUtf(pkt.soundUrl, 32767);
            buf.writeInt(pkt.durationMs);
            buf.writeFloat(pkt.intensity);
        }

        public static AddJumpscareC2SPacket decode(FriendlyByteBuf buf) {
            return new AddJumpscareC2SPacket(buf.readUtf(256), buf.readUtf(32767), buf.readUtf(32767), buf.readInt(), buf.readFloat());
        }

        public static void handle(AddJumpscareC2SPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null) {
                    JumpscareServerConfig.addEntry(pkt.id, pkt.imageUrl, pkt.soundUrl, pkt.durationMs, pkt.intensity);
                    syncConfigToAll(sender.getServer());
                    sendReloadPacketToAll(sender.getServer());
                    sender.sendSystemMessage(Component.literal("§a[Jumpscare] Successfully saved entry '" + pkt.id + "' to server!"));
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class SyncConfigPacket {
        public static class EntryPayload {
            public final String id;
            public final String imageUrl;
            public final String soundUrl;
            public final int durationMs;
            public final float intensity;

            public EntryPayload(String id, String imageUrl, String soundUrl, int durationMs, float intensity) {
                this.id = id != null ? id : "";
                this.imageUrl = imageUrl != null ? imageUrl : "";
                this.soundUrl = soundUrl != null ? soundUrl : "";
                this.durationMs = durationMs;
                this.intensity = intensity;
            }
        }

        private final List<EntryPayload> entries;

        public SyncConfigPacket(List<EntryPayload> entries) {
            this.entries = (entries != null) ? entries : new ArrayList<>();
        }

        public static void encode(SyncConfigPacket pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.entries.size());
            for (EntryPayload entry : pkt.entries) {
                buf.writeUtf(entry.id, 256);
                buf.writeUtf(entry.imageUrl, 32767);
                buf.writeUtf(entry.soundUrl, 32767);
                buf.writeInt(entry.durationMs);
                buf.writeFloat(entry.intensity);
            }
        }

        public static SyncConfigPacket decode(FriendlyByteBuf buf) {
            int count = buf.readInt();
            List<EntryPayload> list = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                String id = buf.readUtf(256);
                String imageUrl = buf.readUtf(32767);
                String soundUrl = buf.readUtf(32767);
                int duration = buf.readInt();
                float intensity = buf.readFloat();
                list.add(new EntryPayload(id, imageUrl, soundUrl, duration, intensity));
            }
            return new SyncConfigPacket(list);
        }

        public static void handle(SyncConfigPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    Map<String, ClientConfigSync.EntryData> map = new LinkedHashMap<>();
                    for (EntryPayload p : pkt.entries) {
                        map.put(p.id, new ClientConfigSync.EntryData(p.imageUrl, p.soundUrl, p.durationMs, p.intensity));
                    }
                    ClientConfigSync.updateEntries(map);
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static boolean sendMindControlPacket(ServerPlayer target, String action, int durationMs) {
        if (target == null) return false;
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new MindControlPacket(action, durationMs));
        return true;
    }

    public static class MindControlPacket {
        private final String action;
        private final int durationMs;

        public MindControlPacket(String action, int durationMs) {
            this.action = action;
            this.durationMs = durationMs;
        }

        public static void encode(MindControlPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.action, 64);
            buf.writeInt(pkt.durationMs);
        }

        public static MindControlPacket decode(FriendlyByteBuf buf) {
            return new MindControlPacket(buf.readUtf(64), buf.readInt());
        }

        public static void handle(MindControlPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.elestial.jumpscare.client.MindControlHandler.applyMindControl(pkt.action, pkt.durationMs);
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class PossessionInputPacket {
        private final String targetName;
        private final float yaw;
        private final float pitch;
        private final float forward;
        private final float strafe;
        private final boolean jumping;
        private final boolean sprinting;
        private final boolean attacking;
        private final boolean stop;

        public PossessionInputPacket(String targetName, float yaw, float pitch, float forward, float strafe, boolean jumping, boolean sprinting, boolean attacking, boolean stop) {
            this.targetName = targetName;
            this.yaw = yaw;
            this.pitch = pitch;
            this.forward = forward;
            this.strafe = strafe;
            this.jumping = jumping;
            this.sprinting = sprinting;
            this.attacking = attacking;
            this.stop = stop;
        }

        public static void encode(PossessionInputPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.targetName, 64);
            buf.writeFloat(pkt.yaw);
            buf.writeFloat(pkt.pitch);
            buf.writeFloat(pkt.forward);
            buf.writeFloat(pkt.strafe);
            buf.writeBoolean(pkt.jumping);
            buf.writeBoolean(pkt.sprinting);
            buf.writeBoolean(pkt.attacking);
            buf.writeBoolean(pkt.stop);
        }

        public static PossessionInputPacket decode(FriendlyByteBuf buf) {
            return new PossessionInputPacket(
                buf.readUtf(64),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean(),
                buf.readBoolean()
            );
        }

        public static void handle(PossessionInputPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null && sender.getServer() != null) {
                    ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(pkt.targetName);
                    if (target != null) {
                        if (pkt.stop) {
                            CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new PossessionStatusPacket(false));
                            return;
                        }

                        // Forward real-time movement overrides to target's client
                        CHANNEL.send(PacketDistributor.PLAYER.with(() -> target), new ClientPossessionMovePacket(
                            pkt.yaw, pkt.pitch, pkt.forward, pkt.strafe, pkt.jumping, pkt.sprinting, pkt.attacking
                        ));

                        // Mirror rotation on server
                        target.setYRot(pkt.yaw);
                        target.setXRot(pkt.pitch);
                        target.setYHeadRot(pkt.yaw);

                        // Calculate motion direction from Controller's yaw/pitch and WASD input
                        double speed = pkt.sprinting ? 0.42 : 0.28;
                        double radYaw = Math.toRadians(pkt.yaw);
                        double sin = Math.sin(radYaw);
                        double cos = Math.cos(radYaw);

                        double motionX = (-sin * pkt.forward + cos * pkt.strafe) * speed;
                        double motionZ = (cos * pkt.forward + sin * pkt.strafe) * speed;

                        target.setDeltaMovement(motionX, target.getDeltaMovement().y, motionZ);
                        target.hasImpulse = true;

                        if (pkt.sprinting) {
                            target.setSprinting(true);
                        }

                        if (pkt.jumping && target.onGround()) {
                            target.jumpFromGround();
                        }

                        if (pkt.attacking) {
                            target.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
                        }
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class ClientPossessionMovePacket {
        private final float yaw;
        private final float pitch;
        private final float forward;
        private final float strafe;
        private final boolean jumping;
        private final boolean sprinting;
        private final boolean attacking;

        public ClientPossessionMovePacket(float yaw, float pitch, float forward, float strafe, boolean jumping, boolean sprinting, boolean attacking) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.forward = forward;
            this.strafe = strafe;
            this.jumping = jumping;
            this.sprinting = sprinting;
            this.attacking = attacking;
        }

        public static void encode(ClientPossessionMovePacket pkt, FriendlyByteBuf buf) {
            buf.writeFloat(pkt.yaw);
            buf.writeFloat(pkt.pitch);
            buf.writeFloat(pkt.forward);
            buf.writeFloat(pkt.strafe);
            buf.writeBoolean(pkt.jumping);
            buf.writeBoolean(pkt.sprinting);
            buf.writeBoolean(pkt.attacking);
        }

        public static ClientPossessionMovePacket decode(FriendlyByteBuf buf) {
            return new ClientPossessionMovePacket(
                buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                buf.readBoolean(), buf.readBoolean(), buf.readBoolean()
            );
        }

        public static void handle(ClientPossessionMovePacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.elestial.jumpscare.client.MindControlHandler.applyRemoteInput(
                        pkt.yaw, pkt.pitch, pkt.forward, pkt.strafe, pkt.jumping, pkt.sprinting, pkt.attacking
                    );
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class PossessionStatusPacket {
        private final boolean possessed;

        public PossessionStatusPacket(boolean possessed) {
            this.possessed = possessed;
        }

        public static void encode(PossessionStatusPacket pkt, FriendlyByteBuf buf) {
            buf.writeBoolean(pkt.possessed);
        }

        public static PossessionStatusPacket decode(FriendlyByteBuf buf) {
            return new PossessionStatusPacket(buf.readBoolean());
        }

        public static void handle(PossessionStatusPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.elestial.jumpscare.client.MindControlHandler.setBeingPossessed(pkt.possessed);
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class BatchJumpscareC2SPacket {
        private final Map<String, String> assignments;

        public BatchJumpscareC2SPacket(Map<String, String> assignments) {
            this.assignments = assignments;
        }

        public static void encode(BatchJumpscareC2SPacket pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.assignments.size());
            for (Map.Entry<String, String> entry : pkt.assignments.entrySet()) {
                buf.writeUtf(entry.getKey(), 64);
                buf.writeUtf(entry.getValue(), 64);
            }
        }

        public static BatchJumpscareC2SPacket decode(FriendlyByteBuf buf) {
            int count = buf.readInt();
            Map<String, String> map = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                map.put(buf.readUtf(64), buf.readUtf(64));
            }
            return new BatchJumpscareC2SPacket(map);
        }

        public static void handle(BatchJumpscareC2SPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null && sender.getServer() != null) {
                    for (Map.Entry<String, String> entry : pkt.assignments.entrySet()) {
                        String targetName = entry.getKey();
                        String jumpscareId = entry.getValue();

                        if (targetName.equalsIgnoreCase("@a") || targetName.equalsIgnoreCase("ALL")) {
                            for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                                triggerSingle(player, jumpscareId);
                            }
                        } else {
                            ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(targetName);
                            if (target != null) {
                                triggerSingle(target, jumpscareId);
                            }
                        }
                    }
                }
            });
            ctx.setPacketHandled(true);
        }

        private static void triggerSingle(ServerPlayer target, String id) {
            String checkId = (id != null && !id.isEmpty()) ? id.toLowerCase() : "spooky";
            if (!checkId.equals("spooky") && JumpscareServerConfig.getEntry(checkId) == null) {
                // Ignore trigger if ID was deleted from server config
                return;
            }
            sendJumpscarePacket(target, checkId, 2500, 1.2f);
        }
    }

    public static class AudioScarePacket {
        private final String soundId;
        private final String soundUrl;
        private final float volume;
        private final float pitch;
        private final boolean behindPlayer;

        public AudioScarePacket(String soundId, String soundUrl, float volume, float pitch, boolean behindPlayer) {
            this.soundId = soundId;
            this.soundUrl = soundUrl;
            this.volume = volume;
            this.pitch = pitch;
            this.behindPlayer = behindPlayer;
        }

        public static void encode(AudioScarePacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.soundId, 256);
            buf.writeUtf(pkt.soundUrl, 32767);
            buf.writeFloat(pkt.volume);
            buf.writeFloat(pkt.pitch);
            buf.writeBoolean(pkt.behindPlayer);
        }

        public static AudioScarePacket decode(FriendlyByteBuf buf) {
            return new AudioScarePacket(
                buf.readUtf(256),
                buf.readUtf(32767),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean()
            );
        }

        public static void handle(AudioScarePacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    com.elestial.jumpscare.client.JumpscareAudioManager.playAudioScare(
                        pkt.soundId, pkt.soundUrl, pkt.volume, pkt.pitch, pkt.behindPlayer
                    );
                });
            });
            ctx.setPacketHandled(true);
        }
    }

    public static class TriggerAudioC2SPacket {
        private final String targetName;
        private final String soundId;
        private final String soundUrl;
        private final float volume;
        private final float pitch;
        private final boolean behindPlayer;

        public TriggerAudioC2SPacket(String targetName, String soundId, String soundUrl, float volume, float pitch, boolean behindPlayer) {
            this.targetName = targetName;
            this.soundId = soundId;
            this.soundUrl = soundUrl;
            this.volume = volume;
            this.pitch = pitch;
            this.behindPlayer = behindPlayer;
        }

        public static void encode(TriggerAudioC2SPacket pkt, FriendlyByteBuf buf) {
            buf.writeUtf(pkt.targetName, 64);
            buf.writeUtf(pkt.soundId, 256);
            buf.writeUtf(pkt.soundUrl, 32767);
            buf.writeFloat(pkt.volume);
            buf.writeFloat(pkt.pitch);
            buf.writeBoolean(pkt.behindPlayer);
        }

        public static TriggerAudioC2SPacket decode(FriendlyByteBuf buf) {
            return new TriggerAudioC2SPacket(
                buf.readUtf(64),
                buf.readUtf(256),
                buf.readUtf(32767),
                buf.readFloat(),
                buf.readFloat(),
                buf.readBoolean()
            );
        }

        public static void handle(TriggerAudioC2SPacket pkt, Supplier<NetworkEvent.Context> ctxGetter) {
            NetworkEvent.Context ctx = ctxGetter.get();
            ctx.enqueueWork(() -> {
                ServerPlayer sender = ctx.getSender();
                if (sender != null && sender.getServer() != null) {
                    String soundUrl = pkt.soundUrl != null ? pkt.soundUrl.trim() : "";
                    if (soundUrl.isEmpty()) {
                        JumpscareServerConfig.JumpscareEntry entry = JumpscareServerConfig.getEntry(pkt.soundId);
                        if (entry != null && entry.soundUrl != null && !entry.soundUrl.isEmpty()) {
                            soundUrl = entry.soundUrl;
                        }
                    }
                    if (pkt.targetName.equalsIgnoreCase("@a") || pkt.targetName.equalsIgnoreCase("ALL")) {
                        for (ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                            sendAudioScarePacket(player, pkt.soundId, soundUrl, pkt.volume, pkt.pitch, pkt.behindPlayer);
                        }
                    } else {
                        ServerPlayer target = sender.getServer().getPlayerList().getPlayerByName(pkt.targetName);
                        if (target != null) {
                            sendAudioScarePacket(target, pkt.soundId, soundUrl, pkt.volume, pkt.pitch, pkt.behindPlayer);
                        }
                    }
                }
            });
            ctx.setPacketHandled(true);
        }
    }
}

