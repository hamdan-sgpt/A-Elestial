package com.elestial.jumpscare.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class MindControlHandler {

    private static String activeAction = null;
    private static long endTimeMs = 0;

    private static boolean isBeingPossessed = false;
    private static long lastPossessedTimeMs = 0;
    private static com.elestial.jumpscare.client.sound.HeartbeatSoundInstance activeHeartbeatSound = null;

    public static void stopHeartbeat() {
        if (activeHeartbeatSound != null) {
            activeHeartbeatSound.stopPlaying();
            try {
                Minecraft.getInstance().getSoundManager().stop(activeHeartbeatSound);
            } catch (Exception ignored) {}
            activeHeartbeatSound = null;
        }
    }

    public static void stopPossessionEffects() {
        stopHeartbeat();
        com.elestial.jumpscare.client.compat.VoiceChatCompat.onPossessionEnd();
    }

    public static void setBeingPossessed(boolean possessed) {
        isBeingPossessed = possessed;
        if (possessed) {
            lastPossessedTimeMs = System.currentTimeMillis();
        } else {
            stopPossessionEffects();
        }
    }

    public static boolean isBeingPossessed() {
        if (isBeingPossessed && (System.currentTimeMillis() - lastPossessedTimeMs > 1500)) {
            isBeingPossessed = false; // Auto timeout if packet stops arriving
            stopPossessionEffects();
        }
        return isBeingPossessed || (activeAction != null);
    }

    private static float remoteForward = 0;
    private static float remoteStrafe = 0;
    private static boolean remoteJumping = false;
    private static boolean remoteActive = false;

    public static void applyRemoteInput(float yaw, float pitch, float forward, float strafe, boolean jumping, boolean sprinting, boolean attacking) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        setBeingPossessed(true);

        player.setYRot(yaw);
        player.setXRot(pitch);
        player.setYHeadRot(yaw);

        remoteForward = forward;
        remoteStrafe = strafe;
        remoteJumping = jumping;
        remoteActive = true;

        if (player.input != null) {
            player.input.forwardImpulse = forward;
            player.input.leftImpulse = strafe;
            player.input.jumping = jumping;
        }

        if (sprinting && player.onGround() && forward > 0) {
            player.setSprinting(true);
        }

        if (jumping && player.onGround()) {
            player.jumpFromGround();
        }

        if (attacking) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, false);
        }
    }

    public static void applyMindControl(String action, int durationMs) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        activeAction = action.toLowerCase();
        endTimeMs = System.currentTimeMillis() + durationMs;

        // One-time instant actions
        if (activeAction.equals("drop")) {
            player.drop(false);
            activeAction = null;
        } else if (activeAction.equals("look_down")) {
            player.setXRot(90.0f);
        } else if (activeAction.equals("look_up")) {
            player.setXRot(-90.0f);
        }
    }

    @SubscribeEvent
    public static void onMovementInput(net.minecraftforge.client.event.MovementInputUpdateEvent event) {
        if (isBeingPossessed() && !PossessionControllerHandler.isPossessing()) {
            if (remoteActive) {
                event.getInput().forwardImpulse = remoteForward;
                event.getInput().leftImpulse = remoteStrafe;
                event.getInput().jumping = remoteJumping;
            } else {
                event.getInput().forwardImpulse = 0;
                event.getInput().leftImpulse = 0;
                event.getInput().jumping = false;
                event.getInput().shiftKeyDown = false;
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            activeAction = null;
            remoteActive = false;
            stopPossessionEffects();
            return;
        }

        boolean possessed = isBeingPossessed() && !PossessionControllerHandler.isPossessing();
        if (possessed) {
            if (activeHeartbeatSound == null || activeHeartbeatSound.isStopped() || !mc.getSoundManager().isActive(activeHeartbeatSound)) {
                activeHeartbeatSound = new com.elestial.jumpscare.client.sound.HeartbeatSoundInstance();
                mc.getSoundManager().play(activeHeartbeatSound);
            }
            com.elestial.jumpscare.client.compat.VoiceChatCompat.onPossessionStart();
            com.elestial.jumpscare.client.compat.VoiceChatCompat.tickPossessionMute();
        } else {
            stopPossessionEffects();
        }

        if (isBeingPossessed() && remoteActive && player.input != null) {
            player.input.forwardImpulse = remoteForward;
            player.input.leftImpulse = remoteStrafe;
            player.input.jumping = remoteJumping;
        } else if (!isBeingPossessed()) {
            remoteActive = false;
        }

        if (activeAction == null) return;

        if (System.currentTimeMillis() > endTimeMs) {
            activeAction = null; // Expired
            return;
        }

        switch (activeAction) {
            case "spin":
                player.setYRot(player.getYRot() + 25.0f);
                break;
            case "jump":
                if (player.onGround()) {
                    player.jumpFromGround();
                }
                break;
            case "freeze":
                player.setDeltaMovement(0, Math.min(0, player.getDeltaMovement().y), 0);
                break;
            case "look_down":
                player.setXRot(90.0f);
                break;
            case "look_up":
                player.setXRot(-90.0f);
                break;
            case "forward":
                if (player.onGround()) {
                    player.setSprinting(true);
                }
                break;
        }
    }

    public static void renderRedPossessionVignette(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (!isBeingPossessed()) return;

        long time = System.currentTimeMillis();
        float pulse = (float) (Math.sin(time * 0.006) * 0.25 + 0.75); // Sinusoidal pulse
        int alphaEdge = (int) (190 * pulse);
        int alphaCorner = (int) (220 * pulse);

        int colorEdge = (alphaEdge << 24) | 0x880000;      // Dark Crimson Red
        int colorCorner = (alphaCorner << 24) | 0xAA0000;  // Bright Blood Red
        int colorCenter = 0x00880000;                     // Transparent

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 1200.0f); // Render above HUD

        int bandSize = Math.min(screenWidth, screenHeight) / 3;

        // Top Gradient
        guiGraphics.fillGradient(0, 0, screenWidth, bandSize, colorEdge, colorCenter);
        // Bottom Gradient
        guiGraphics.fillGradient(0, screenHeight - bandSize, screenWidth, screenHeight, colorCenter, colorEdge);

        // Subtly overlay corner danger tint
        guiGraphics.fill(0, 0, 30, 30, colorCorner);
        guiGraphics.fill(screenWidth - 30, 0, screenWidth, 30, colorCorner);
        guiGraphics.fill(0, screenHeight - 30, 30, screenHeight, colorCorner);
        guiGraphics.fill(screenWidth - 30, screenHeight - 30, screenWidth, screenHeight, colorCorner);

        guiGraphics.pose().popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
