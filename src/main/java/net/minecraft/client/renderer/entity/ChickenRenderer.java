package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.animal.chicken.ColdChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.optifine.entity.model.ChickenType;
import net.optifine.entity.model.ModelPairUtils;

public class ChickenRenderer extends MobRenderer<Chicken, ChickenRenderState, ChickenModel> {
    private final Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> models;
    private AdultAndBabyModelPair<ChickenModel> warmModels;
    private boolean customModels;

    public ChickenRenderer(EntityRendererProvider.Context p_173952_) {
        super(p_173952_, new ChickenModel(p_173952_.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
        this.models = bakeModels(p_173952_);
        this.warmModels = this.models.get(ChickenVariant.ModelType.NORMAL);
    }

    private static Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> bakeModels(EntityRendererProvider.Context p_396360_) {
        return Maps.newEnumMap(
            Map.of(
                ChickenVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(
                    new ChickenModel(p_396360_.bakeLayer(ModelLayers.CHICKEN)), new ChickenModel(p_396360_.bakeLayer(ModelLayers.CHICKEN_BABY))
                ),
                ChickenVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new ColdChickenModel(p_396360_.bakeLayer(ModelLayers.COLD_CHICKEN)), new ColdChickenModel(p_396360_.bakeLayer(ModelLayers.COLD_CHICKEN_BABY))
                )
            )
        );
    }

    public void submit(ChickenRenderState p_425325_, PoseStack p_422426_, SubmitNodeCollector p_424654_, CameraRenderState p_422384_) {
        if (p_425325_.variant != null) {
            this.model = this.models.get(p_425325_.variant.modelAndTexture().model()).getModel(p_425325_.isBaby);
            if (this.customModels && p_425325_.variant.modelAndTexture().asset().id().getPath().endsWith("warm_chicken")) {
                this.model = this.warmModels.getModel(p_425325_.isBaby);
            }

            super.submit(p_425325_, p_422426_, p_424654_, p_422384_);
        }
    }

    public Identifier getTextureLocation(ChickenRenderState p_368820_) {
        return p_368820_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_368820_.variant.modelAndTexture().asset().texturePath();
    }

    public ChickenRenderState createRenderState() {
        return new ChickenRenderState();
    }

    public void extractRenderState(Chicken p_451495_, ChickenRenderState p_368780_, float p_370144_) {
        super.extractRenderState(p_451495_, p_368780_, p_370144_);
        p_368780_.flap = Mth.lerp(p_370144_, p_451495_.oFlap, p_451495_.flap);
        p_368780_.flapSpeed = Mth.lerp(p_370144_, p_451495_.oFlapSpeed, p_451495_.flapSpeed);
        p_368780_.variant = p_451495_.getVariant().value();
    }

    public void setModel(ChickenType typeIn, boolean babyIn, ChickenModel modelIn) {
        if (typeIn == ChickenType.NORMAL) {
            this.setModel(ChickenVariant.ModelType.NORMAL, babyIn, modelIn);
        } else if (typeIn == ChickenType.COLD) {
            this.setModel(ChickenVariant.ModelType.COLD, babyIn, modelIn);
        } else if (typeIn == ChickenType.WARM) {
            this.warmModels = ModelPairUtils.updateModel(this.warmModels, babyIn, modelIn);
        }

        this.customModels = true;
    }

    private void setModel(ChickenVariant.ModelType typeIn, boolean babyIn, ChickenModel modelIn) {
        AdultAndBabyModelPair<ChickenModel> adultandbabymodelpair = this.models.get(typeIn);
        AdultAndBabyModelPair<ChickenModel> adultandbabymodelpair1 = ModelPairUtils.updateModel(adultandbabymodelpair, babyIn, modelIn);
        this.models.put(typeIn, adultandbabymodelpair1);
    }
}
