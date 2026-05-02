package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.feline.CatModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCatCollar extends ModelAdapterCat {
    public ModelAdapterCatCollar() {
        super(EntityType.CAT, "cat_collar", ModelLayers.CAT_COLLAR);
        this.setAlias("cat");
    }

    protected ModelAdapterCatCollar(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCatCollar modeladaptercatcollar =
                new ModelAdapterCatCollar(this.getEntityType(), "cat_baby_collar", ModelLayers.CAT_BABY_COLLAR);
        modeladaptercatcollar.setBaby(true);
        modeladaptercatcollar.setAliases(this.getName(), "cat");
        return modeladaptercatcollar;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        CatModel catmodel = (CatModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof CatCollarLayer) {
                CatCollarLayer catcollarlayer = (CatCollarLayer) layer;

                if (this.isBaby()) {
                    catcollarlayer.babyModel = catmodel;
                } else {
                    catcollarlayer.adultModel = catmodel;
                }
            }
        }
    }
}