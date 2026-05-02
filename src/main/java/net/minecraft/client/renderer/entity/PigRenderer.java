package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.pig.ColdPigModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.optifine.entity.model.ModelPairUtils;
import net.optifine.entity.model.PigType;

public class PigRenderer extends MobRenderer<Pig, PigRenderState, PigModel> {
    private final Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> models;
    private AdultAndBabyModelPair<PigModel> warmModels;
    private boolean customModels;

    public PigRenderer(EntityRendererProvider.Context p_174340_) {
        super(p_174340_, new PigModel(p_174340_.bakeLayer(ModelLayers.PIG)), 0.7F);
        this.models = bakeModels(p_174340_);
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_174340_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.PIG_SADDLE,
                stateIn -> stateIn.saddle,
                new PigModel(p_174340_.bakeLayer(ModelLayers.PIG_SADDLE)),
                new PigModel(p_174340_.bakeLayer(ModelLayers.PIG_BABY_SADDLE))
            )
        );
        this.warmModels = this.models.get(PigVariant.ModelType.NORMAL);
    }

    private static Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> bakeModels(EntityRendererProvider.Context p_396029_) {
        return Maps.newEnumMap(
            Map.of(
                PigVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(new PigModel(p_396029_.bakeLayer(ModelLayers.PIG)), new PigModel(p_396029_.bakeLayer(ModelLayers.PIG_BABY))),
                PigVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new ColdPigModel(p_396029_.bakeLayer(ModelLayers.COLD_PIG)), new ColdPigModel(p_396029_.bakeLayer(ModelLayers.COLD_PIG_BABY))
                )
            )
        );
    }

    public void submit(PigRenderState p_422871_, PoseStack p_427883_, SubmitNodeCollector p_429250_, CameraRenderState p_429157_) {
        if (p_422871_.variant != null) {
            this.model = this.models.get(p_422871_.variant.modelAndTexture().model()).getModel(p_422871_.isBaby);
            if (this.customModels && p_422871_.variant.modelAndTexture().asset().id().getPath().endsWith("warm_pig")) {
                this.model = this.warmModels.getModel(p_422871_.isBaby);
            }

            super.submit(p_422871_, p_427883_, p_429250_, p_429157_);
        }
    }

    public Identifier getTextureLocation(PigRenderState p_452903_) {
        return p_452903_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_452903_.variant.modelAndTexture().asset().texturePath();
    }

    public PigRenderState createRenderState() {
        return new PigRenderState();
    }

    public void extractRenderState(Pig p_452787_, PigRenderState p_370177_, float p_367094_) {
        super.extractRenderState(p_452787_, p_370177_, p_367094_);
        p_370177_.saddle = p_452787_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_370177_.variant = p_452787_.getVariant().value();
    }

    public void setModel(PigType typeIn, boolean babyIn, PigModel modelIn) {
        if (typeIn == PigType.NORMAL) {
            this.setModel(PigVariant.ModelType.NORMAL, babyIn, modelIn);
        } else if (typeIn == PigType.COLD) {
            this.setModel(PigVariant.ModelType.COLD, babyIn, modelIn);
        } else if (typeIn == PigType.WARM) {
            this.warmModels = ModelPairUtils.updateModel(this.warmModels, babyIn, modelIn);
        }

        this.customModels = true;
    }

    private void setModel(PigVariant.ModelType typeIn, boolean babyIn, PigModel modelIn) {
        AdultAndBabyModelPair<PigModel> adultandbabymodelpair = this.models.get(typeIn);
        AdultAndBabyModelPair<PigModel> adultandbabymodelpair1 = ModelPairUtils.updateModel(adultandbabymodelpair, babyIn, modelIn);
        this.models.put(typeIn, adultandbabymodelpair1);
    }
}
