package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.itemphysiclite.ClientPhysic;
import team.creative.itemphysiclite.ItemEntityRenderStateExtender;
import team.creative.itemphysiclite.ItemPhysicLite;
import team.creative.itemphysiclite.mixin.ItemStackRenderStateAccessor;
import team.creative.itemphysiclite.mixin.LayerRenderStateAccessor;
import team.creative.itemphysiclite.mixin.RenderTypeAccessor;


public class ItemEntityRenderState extends ItemClusterRenderState implements ItemEntityRenderStateExtender {
    public float bobOffset;

    public float rotX;
    public float rotY;
    public boolean skipRendering;
    public boolean additionalOffset;
    public boolean isBlock;

    @Override
    public float getXRot() {
        return rotX;
    }

    @Override
    public float getYRot() {
        return rotY;
    }

    @Override
    public boolean hasAdditionalOffset() {
        return additionalOffset;
    }

    @Override
    public boolean isBlock() {
        return isBlock;
    }

    @Override
    public void extractPhysic(ItemEntity entity) {
        ItemEntityRenderState state = this;
        var renderType = ((LayerRenderStateAccessor) ((ItemStackRenderStateAccessor) state.item).callFirstLayer()).getRenderType();
        isBlock = state.item.usesBlockLight() && (renderType == null || ((RenderTypeAccessor) renderType).getName().equals("item_entity_translucent_cull"));
        ClientPhysic.calculateRotation(entity, state);
        additionalOffset = ItemPhysicLite.CONFIG.blockRequireOffset.is(entity.level().getBlockState(entity.blockPosition()));
        rotX = entity.getXRot();
        rotY = entity.getYRot();
    }
}
