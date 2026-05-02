package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.wolf.WolfModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterWolfCollar extends ModelAdapterWolf {
    public ModelAdapterWolfCollar() {
        this(EntityType.WOLF, "wolf_collar", ModelLayers.WOLF);
        this.setAlias("wolf");
    }

    protected ModelAdapterWolfCollar(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterAgeable modeladapterageable = new ModelAdapterWolfCollar(
                this.getEntityType(), "wolf_baby_collar", ModelLayers.WOLF_BABY
        );
        modeladapterageable.setBaby(true);
        modeladapterageable.setAliases(this.getName(), "wolf");
        return modeladapterageable;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model model) {
        WolfModel wolfmodel = (WolfModel) model;
        for (Object layer : renderer.layers) {
            if (layer instanceof WolfCollarLayer) {
                WolfCollarLayer wolfcollarlayer = (WolfCollarLayer) layer;

                if (this.isBaby()) {
                    wolfcollarlayer.babyModel = wolfmodel;
                } else {
                    wolfcollarlayer.adultModel = wolfmodel;
                }
            }
        }
    }
}