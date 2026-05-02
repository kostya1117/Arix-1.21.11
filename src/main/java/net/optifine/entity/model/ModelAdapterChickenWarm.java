package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterChickenWarm extends ModelAdapterChicken {
    public ModelAdapterChickenWarm() {
        super(EntityType.CHICKEN, "warm_chicken", ModelLayers.CHICKEN, ChickenType.WARM);
    }

    protected ModelAdapterChickenWarm(EntityType entityType, String name, ModelLayerLocation modelLayer, ChickenType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterChickenWarm modeladapterchickenwarm = new ModelAdapterChickenWarm(
            this.getEntityType(), "warm_chicken_baby", ModelLayers.CHICKEN_BABY, ChickenType.WARM
        );
        modeladapterchickenwarm.setBaby(true);
        modeladapterchickenwarm.setAlias(this.getName());
        return modeladapterchickenwarm;
    }
}
