package net.optifine.entity.model;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ParchedRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterParched extends ModelAdapterSkeleton {
    public ModelAdapterParched() {
        super(EntityType.PARCHED, "parched", ModelLayers.PARCHED);
    }

    public ModelAdapterParched(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new ParchedRenderer(context);
    }
}
