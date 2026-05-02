package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPig extends ModelAdapterMultiLiving {
    public ModelAdapterPig() {
        super(EntityType.PIG, "pig", ModelLayers.PIG, PigType.NORMAL);
    }

    protected ModelAdapterPig(EntityType entityType, String name, ModelLayerLocation modelLayer, PigType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPig modeladapterpig = new ModelAdapterPig(this.getEntityType(), "pig_baby", ModelLayers.PIG_BABY, PigType.NORMAL);
        modeladapterpig.setBaby(true);
        modeladapterpig.setAlias(this.getName());
        return modeladapterpig;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new PigModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        return ModelAdapterQuadruped.makeMapPartsStatic();
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new PigRenderer(context);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase, Object type, boolean baby) {
        PigRenderer pigrenderer = (PigRenderer)renderer;
        PigModel pigmodel = (PigModel)modelBase;
        PigType pigtype = (PigType)type;
        pigrenderer.setModel(pigtype, baby, pigmodel);
    }
}
