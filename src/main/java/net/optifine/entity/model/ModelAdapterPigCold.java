package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPigCold extends ModelAdapterPig {
    public ModelAdapterPigCold() {
        super(EntityType.PIG, "cold_pig", ModelLayers.COLD_PIG, PigType.COLD);
    }

    protected ModelAdapterPigCold(EntityType entityType, String name, ModelLayerLocation modelLayer, PigType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPigCold modeladapterpigcold = new ModelAdapterPigCold(this.getEntityType(), "cold_pig_baby", ModelLayers.COLD_PIG_BABY, PigType.COLD);
        modeladapterpigcold.setBaby(true);
        modeladapterpigcold.setAlias(this.getName());
        return modeladapterpigcold;
    }
}
