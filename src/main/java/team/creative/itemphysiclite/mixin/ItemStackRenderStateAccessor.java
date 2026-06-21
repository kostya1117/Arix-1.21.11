package team.creative.itemphysiclite.mixin;

import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface ItemStackRenderStateAccessor {

    public ItemStackRenderState.LayerRenderState callFirstLayer();
    
}
