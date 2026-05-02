package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.Display;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public class TextDisplayEntityRenderState extends DisplayEntityRenderState {
    public Display.TextDisplay. TextRenderState textRenderState;
    public Display.TextDisplay. CachedInfo cachedInfo;

    @Override
    public boolean hasSubState() {
        return this.textRenderState != null;
    }
}
