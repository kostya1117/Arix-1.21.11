package net.minecraftforge.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;

public class RenderHighlightEvent {
    public interface Callback {
        void render(MultiBufferSource.BufferSource var1, PoseStack var2, boolean var3, LevelRenderState var4);
    }
}
