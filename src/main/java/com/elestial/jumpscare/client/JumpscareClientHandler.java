package com.elestial.jumpscare.client;

import com.elestial.jumpscare.JumpscareMod;
import com.elestial.jumpscare.asset.JumpscareAssetManager;
import com.elestial.jumpscare.client.gui.JumpscareScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = JumpscareMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class JumpscareClientHandler {

    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping(
        "key.elestialjumpscare.open_gui",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_J,
        "key.categories.elestialjumpscare"
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            JumpscareAssetManager.getInstance().init();
        });
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("jumpscare_overlay", JumpscareOverlayRenderer.getInstance());
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_GUI_KEY);
    }

    @Mod.EventBusSubscriber(modid = JumpscareMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (MindControlHandler.isBeingPossessed() && !PossessionControllerHandler.isPossessing()) {
                com.elestial.jumpscare.client.compat.VoiceChatCompat.tickPossessionMute();
            }
            if (OPEN_GUI_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new JumpscareScreen());
                }
            }
        }

        @SubscribeEvent
        public static void onRenderGuiPost(RenderGuiEvent.Post event) {
            JumpscareOverlayRenderer.getInstance().renderOverlayInternal(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight()
            );
            MindControlHandler.renderRedPossessionVignette(
                event.getGuiGraphics(),
                event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight()
            );
        }

        @SubscribeEvent
        public static void onRenderScreenPost(ScreenEvent.Render.Post event) {
            JumpscareOverlayRenderer.getInstance().renderOverlayInternal(
                event.getGuiGraphics(),
                event.getScreen().width,
                event.getScreen().height
            );
            MindControlHandler.renderRedPossessionVignette(
                event.getGuiGraphics(),
                event.getScreen().width,
                event.getScreen().height
            );
        }
    }
}

