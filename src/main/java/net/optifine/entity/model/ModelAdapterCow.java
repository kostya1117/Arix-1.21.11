package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterCow extends ModelAdapterMultiLiving {
    public ModelAdapterCow() {
        super(EntityType.COW, "cow", ModelLayers.COW, CowVariant.ModelType.NORMAL);
    }

    protected ModelAdapterCow(EntityType entityType, String name, ModelLayerLocation modelLayer, CowVariant.ModelType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCow modeladaptercow = new ModelAdapterCow(this.getEntityType(), "cow_baby", ModelLayers.COW_BABY, CowVariant.ModelType.NORMAL);
        modeladaptercow.setBaby(true);
        modeladaptercow.setAlias(this.getName());
        return modeladaptercow;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CowModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        return ModelAdapterQuadruped.makeMapPartsStatic();
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new CowRenderer(context);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase, Object type, boolean baby) {
        CowRenderer cowrenderer = (CowRenderer)renderer;
        CowModel cowmodel = (CowModel)modelBase;
        CowVariant.ModelType cowvariant$modeltype = (CowVariant.ModelType)type;
        Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> map = (Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>>)Reflector.CowRenderer_models
            .getValue(cowrenderer);
        if (map == null) {
            Config.warn("CowRenderer.models not found.");
        } else {
            AdultAndBabyModelPair<CowModel> adultandbabymodelpair = map.get(cowvariant$modeltype);
            if (adultandbabymodelpair == null) {
                Config.warn("CowRenderer.models.pair not found for type: " + cowvariant$modeltype);
            } else {
                AdultAndBabyModelPair<CowModel> adultandbabymodelpair1;
                if (baby) {
                    adultandbabymodelpair1 = new AdultAndBabyModelPair<>(adultandbabymodelpair.adultModel(), cowmodel);
                } else {
                    adultandbabymodelpair1 = new AdultAndBabyModelPair<>(cowmodel, adultandbabymodelpair.babyModel());
                }

                map.put(cowvariant$modeltype, adultandbabymodelpair1);
            }
        }
    }
}
