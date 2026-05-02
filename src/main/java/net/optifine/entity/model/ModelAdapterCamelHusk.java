package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.CamelHuskRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCamelHusk extends ModelAdapterCamel {
    public ModelAdapterCamelHusk() {
        super(EntityType.CAMEL_HUSK, "camel_husk", ModelLayers.CAMEL);
    }

    protected ModelAdapterCamelHusk(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCamelHusk modeladaptercamelhusk = new ModelAdapterCamelHusk(this.getEntityType(), "camel_husk_baby", ModelLayers.CAMEL_BABY);
        modeladaptercamelhusk.setBaby(true);
        modeladaptercamelhusk.setAlias(this.getName());
        return modeladaptercamelhusk;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CamelModel(root);
    }

    @Override
    protected AgeableMobRenderer makeAgeableRenderer(EntityRendererProvider.Context context) {
        return new CamelHuskRenderer(context);
    }
}
