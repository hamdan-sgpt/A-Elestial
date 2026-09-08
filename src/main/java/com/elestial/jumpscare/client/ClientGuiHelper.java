package com.elestial.jumpscare.client;

import com.elestial.jumpscare.asset.JumpscareAssetManager;
import com.elestial.jumpscare.client.gui.JumpscareScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientGuiHelper {

    public static void openScreen() {
        Minecraft.getInstance().execute(() -> {
            Minecraft.getInstance().setScreen(new JumpscareScreen());
        });
    }

    public static void openFolder() {
        Minecraft.getInstance().execute(() -> {
            JumpscareAssetManager.getInstance().openAssetFolder();
        });
    }
}
