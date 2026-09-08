package com.elestial.jumpscare.client.sound;

import com.elestial.jumpscare.client.MindControlHandler;
import com.elestial.jumpscare.client.PossessionControllerHandler;
import com.elestial.jumpscare.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HeartbeatSoundInstance extends AbstractTickableSoundInstance {

    public HeartbeatSoundInstance() {
        super(ModSounds.HEARTBEAT.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0F;
        this.pitch = 1.0F;
        this.relative = true; // Plays centered in player's head/ears
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isAlive()) {
            this.stop();
            return;
        }

        // Stop playing immediately if no longer possessed or if currently controlling someone else
        if (!MindControlHandler.isBeingPossessed() || PossessionControllerHandler.isPossessing()) {
            this.stop();
        }
    }

    public void stopPlaying() {
        this.stop();
    }
}
