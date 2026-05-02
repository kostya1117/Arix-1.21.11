package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.renderer.entity.CreeperRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CreeperPowerLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCreeperCharge extends ModelAdapterCreeper {
    public ModelAdapterCreeperCharge() {
        super(EntityType.CREEPER, "creeper_charge", ModelLayers.CREEPER_ARMOR);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        CreeperRenderer creeperrenderer = (CreeperRenderer) renderer;
        CreeperPowerLayer creeperpowerlayer = new CreeperPowerLayer(creeperrenderer, this.getContext().getModelSet());
        creeperpowerlayer.model = (CreeperModel) modelBase;
        creeperrenderer.replaceLayer(CreeperPowerLayer.class, creeperpowerlayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, Identifier textureLocation) {
        LivingEntityRenderer livingentityrenderer = (LivingEntityRenderer) er;

        for (Object layer : livingentityrenderer.layers) {
            if (layer instanceof CreeperPowerLayer) {
                CreeperPowerLayer creeperpowerlayer = (CreeperPowerLayer) layer;
                creeperpowerlayer.customTextureLocation = textureLocation;
            }
        }

        return true;
    }
}