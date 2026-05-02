package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.TropicalFishRenderer;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.optifine.reflect.Reflector;

public class ModelAdapterTropicalFishPatternA extends ModelAdapterTropicalFishA {
    public ModelAdapterTropicalFishPatternA() {
        super(EntityType.TROPICAL_FISH, "tropical_fish_pattern_a", ModelLayers.TROPICAL_FISH_SMALL_PATTERN);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        TropicalFishRenderer tropicalfishrenderer = (TropicalFishRenderer)renderer;

        for (TropicalFishPatternLayer tropicalfishpatternlayer : tropicalfishrenderer.getLayers(TropicalFishPatternLayer.class)) {
            this.setModel(tropicalfishpatternlayer, Reflector.TropicalFishPatternLayer_modelA, "TropicalFishPatternLayer.modelA", modelBase);
        }
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, Identifier textureLocation) {
        TropicalFishRenderer tropicalfishrenderer = (TropicalFishRenderer)er;

        for (TropicalFishPatternLayer tropicalfishpatternlayer : tropicalfishrenderer.getLayers(TropicalFishPatternLayer.class)) {
            TropicalFishSmallModel tropicalfishsmallmodel = (TropicalFishSmallModel)Reflector.TropicalFishPatternLayer_modelA
                .getValue(tropicalfishpatternlayer);
            if (tropicalfishsmallmodel != null) {
                tropicalfishsmallmodel.locationTextureCustom = textureLocation;
            }
        }

        return true;
    }
}
