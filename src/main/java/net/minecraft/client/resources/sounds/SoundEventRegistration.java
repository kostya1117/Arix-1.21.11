package net.minecraft.client.resources.sounds;

import java.util.List;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public class SoundEventRegistration {
    private final List<Sound> sounds;
    private final boolean replace;
    private final  String subtitle;

    public SoundEventRegistration(List<Sound> p_119819_, boolean p_119820_,  String p_119821_) {
        this.sounds = p_119819_;
        this.replace = p_119820_;
        this.subtitle = p_119821_;
    }

    public List<Sound> getSounds() {
        return this.sounds;
    }

    public boolean isReplace() {
        return this.replace;
    }

    public  String getSubtitle() {
        return this.subtitle;
    }
}
