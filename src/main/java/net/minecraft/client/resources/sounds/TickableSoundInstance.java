package net.minecraft.client.resources.sounds;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public interface TickableSoundInstance extends SoundInstance {
    boolean isStopped();

    void tick();
}
