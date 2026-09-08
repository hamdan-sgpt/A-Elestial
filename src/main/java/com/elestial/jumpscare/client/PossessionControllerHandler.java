package com.elestial.jumpscare.client;

import com.elestial.jumpscare.network.JumpscareNetworking;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class PossessionControllerHandler {

    private static String activeTarget = null;
    private static Player targetEntity = null;

    public static void startPossession(String targetPlayerName) {
        activeTarget = targetPlayerName;
        targetEntity = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendSystemMessage(Component.literal("§d[★] Merasuki '" + targetPlayerName + "'! POV milik target. Tekan SNEAK (SHIFT) untuk keluar."));
        }
    }

    public static void stopPossession() {
        if (activeTarget != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.setCameraEntity(mc.player); // Reset camera to Dev's own body
                mc.player.sendSystemMessage(Component.literal("§e[★] Melepas '" + activeTarget + "'. Kamera kembali ke tubuhmu."));
            }
            JumpscareNetworking.sendPossessionInputPacket(activeTarget, 0, 0, 0, 0, false, false, false, true);
            activeTarget = null;
            targetEntity = null;
        }
    }

    public static boolean isPossessing() {
        return activeTarget != null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || activeTarget == null) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stopPossession();
            return;
        }

        // Attach Dev's POV camera to Victim's entity eyes
        if (targetEntity == null || !targetEntity.isAlive()) {
            for (Player p : mc.level.players()) {
                if (p.getScoreboardName().equalsIgnoreCase(activeTarget) && p != player) {
                    targetEntity = p;
                    mc.setCameraEntity(p);
                    break;
                }
            }
        } else if (mc.getCameraEntity() != targetEntity) {
            mc.setCameraEntity(targetEntity);
        }

        // Release control on Shift / Sneak key press
        if (InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT) ||
            player.isCrouching()) {
            stopPossession();
            return;
        }

        float yaw = player.getYRot();
        float pitch = player.getXRot();
        float forward = player.input.forwardImpulse;
        float strafe = player.input.leftImpulse;
        boolean jumping = player.input.jumping;
        boolean sprinting = player.isSprinting() || mc.options.keySprint.isDown();
        boolean attacking = mc.options.keyAttack.isDown();

        JumpscareNetworking.sendPossessionInputPacket(activeTarget, yaw, pitch, forward, strafe, jumping, sprinting, attacking, false);
    }
}
