package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterChickenCold extends ModelAdapterChicken {
    public ModelAdapterChickenCold() {
        super(EntityType.CHICKEN, "cold_chicken", ModelLayers.COLD_CHICKEN, ChickenType.COLD);
    }

    protected ModelAdapterChickenCold(EntityType entityType, String name, ModelLayerLocation modelLayer, ChickenType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterChickenCold modeladapterchickencold = new ModelAdapterChickenCold(
            this.getEntityType(), "cold_chicken_baby", ModelLayers.COLD_CHICKEN_BABY, ChickenType.COLD
        );
        modeladapterchickencold.setBaby(true);
        modeladapterchickencold.setAlias(this.getName());
        return modeladapterchickencold;
    }
}
