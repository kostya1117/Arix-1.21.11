package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.HappyGhastRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.RopesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterHappyGhastRopes extends ModelAdapterHappyGhast {
    public ModelAdapterHappyGhastRopes() {
        super(EntityType.HAPPY_GHAST, "happy_ghast_ropes", ModelLayers.HAPPY_GHAST_ROPES);
    }

    protected ModelAdapterHappyGhastRopes(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        return null;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        HappyGhastModel happyghastmodel = (HappyGhastModel)modelBase;

        for (RenderLayer ropeslayer : this.getRenderLayers(renderer, RopesLayer.class)) {
            if (!Reflector.RopesLayer_adultModel.exists()) {
                Config.warn("RopesLayer.adultModel not found");
                return;
            }

            Reflector.setFieldValue(ropeslayer, Reflector.RopesLayer_adultModel, happyghastmodel);
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, Identifier textureLocation) {
        HappyGhastRenderer happyghastrenderer = (HappyGhastRenderer)er;

        for (RenderLayer ropeslayer : this.getRenderLayers(happyghastrenderer, RopesLayer.class)) {
            if (!Reflector.RopesLayer_ropes.exists()) {
                Config.warn("Field not found: RopesLayer.ropes");
            }

            RenderType rendertype = RenderTypes.entityCutoutNoCull(textureLocation);
            Reflector.RopesLayer_ropes.setValue(ropeslayer, rendertype);
        }

        return true;
    }
}
