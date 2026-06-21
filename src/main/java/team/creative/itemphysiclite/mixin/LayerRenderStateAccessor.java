package team.creative.itemphysiclite.mixin;

import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.rendertype.RenderType;

public interface LayerRenderStateAccessor {

    public ItemTransform getTransform();

    public RenderType getRenderType();
}
