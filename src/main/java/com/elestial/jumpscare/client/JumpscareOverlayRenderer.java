package com.elestial.jumpscare.client;

import com.elestial.jumpscare.asset.JumpscareAssetManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import java.util.Random;

public class JumpscareOverlayRenderer implements IGuiOverlay {
    private static JumpscareOverlayRenderer INSTANCE;

    private String activeJumpscareId = null;
    private long startTimeMs = 0;
    private int durationMs = 2000;
    private float intensity = 1.0f;
    private final Random random = new Random();

    public static JumpscareOverlayRenderer getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JumpscareOverlayRenderer();
        }
        return INSTANCE;
    }

    public void triggerJumpscare(String jumpscareId, int durationMs, float intensity) {
        this.activeJumpscareId = jumpscareId;
        this.startTimeMs = System.currentTimeMillis();
        this.durationMs = durationMs;
        this.intensity = intensity;

        // Trigger sound effect
        JumpscareAudioManager.playJumpscareSound(jumpscareId);
    }

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        renderOverlayInternal(guiGraphics, screenWidth, screenHeight);
    }

    public void renderOverlayInternal(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        if (activeJumpscareId == null) return;

        long elapsed = System.currentTimeMillis() - startTimeMs;
        if (elapsed > durationMs) {
            activeJumpscareId = null; // Expired
            return;
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0f, 0.0f, 1500.0f); // Render at top Z layer to cover Chat & GUI

        // 1. Solid pitch black background behind jumpscare gif/image
        guiGraphics.fill(0, 0, screenWidth, screenHeight, 0xFF000000);

        // 2. Dynamic Texture Render with Matrix Translation for Camera Shake
        ResourceLocation textureId = JumpscareAssetManager.getInstance().getTextureId(activeJumpscareId);
        if (textureId != null) {
            float shakeX = (random.nextFloat() - 0.5f) * 30.0f * intensity;
            float shakeY = (random.nextFloat() - 0.5f) * 30.0f * intensity;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(shakeX, shakeY, 0.0f);

            // Clean blit with exact screen dimensions
            guiGraphics.blit(textureId, 0, 0, 0, 0, screenWidth, screenHeight, screenWidth, screenHeight);

            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().popPose();

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }
}
