package com.elestial.jumpscare.sound;

import com.elestial.jumpscare.JumpscareMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, JumpscareMod.MOD_ID);

    public static final RegistryObject<SoundEvent> HEARTBEAT =
        SOUND_EVENTS.register("heartbeat", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(JumpscareMod.MOD_ID, "heartbeat")));

    public static final RegistryObject<SoundEvent> FAKESTEP =
        SOUND_EVENTS.register("fakestep", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(JumpscareMod.MOD_ID, "fakestep")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
