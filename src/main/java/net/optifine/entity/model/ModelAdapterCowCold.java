package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.CowVariant;

public class ModelAdapterCowCold extends ModelAdapterCow {
    public ModelAdapterCowCold() {
        super(EntityType.COW, "cold_cow", ModelLayers.COLD_COW, CowVariant.ModelType.COLD);
    }

    protected ModelAdapterCowCold(EntityType entityType, String name, ModelLayerLocation modelLayer, CowVariant.ModelType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCowCold modeladaptercowcold = new ModelAdapterCowCold(
            this.getEntityType(), "cold_cow_baby", ModelLayers.COLD_COW_BABY, CowVariant.ModelType.COLD
        );
        modeladaptercowcold.setBaby(true);
        modeladaptercowcold.setAlias(this.getName());
        return modeladaptercowcold;
    }
}
