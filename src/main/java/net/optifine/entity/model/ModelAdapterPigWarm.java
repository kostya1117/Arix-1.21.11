package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPigWarm extends ModelAdapterPig {
    public ModelAdapterPigWarm() {
        super(EntityType.PIG, "warm_pig", ModelLayers.PIG, PigType.WARM);
    }

    protected ModelAdapterPigWarm(EntityType entityType, String name, ModelLayerLocation modelLayer, PigType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPigWarm modeladapterpigwarm = new ModelAdapterPigWarm(this.getEntityType(), "warm_pig_baby", ModelLayers.PIG_BABY, PigType.WARM);
        modeladapterpigwarm.setBaby(true);
        modeladapterpigwarm.setAlias(this.getName());
        return modeladapterpigwarm;
    }
}
