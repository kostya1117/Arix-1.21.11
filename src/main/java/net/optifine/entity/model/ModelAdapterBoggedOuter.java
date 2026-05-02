package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.BoggedRenderer;
import net.minecraft.client.renderer.entity.layers.SkeletonClothingLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterBoggedOuter extends ModelAdapterSkeleton {
    public ModelAdapterBoggedOuter() {
        super(EntityType.BOGGED, "bogged_outer", ModelLayers.BOGGED_OUTER_LAYER);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SkeletonModel(root);
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        BoggedRenderer boggedrenderer = (BoggedRenderer)renderer;
        Identifier identifier = Identifier.withDefaultNamespace("textures/entity/skeleton/bogged_overlay.png");
        SkeletonClothingLayer skeletonclothinglayer = new SkeletonClothingLayer<>(
            boggedrenderer, this.getContext().getModelSet(), ModelLayers.BOGGED_OUTER_LAYER, identifier
        );
        skeletonclothinglayer.layerModel = (SkeletonModel)modelBase;
        boggedrenderer.replaceLayer(SkeletonClothingLayer.class, skeletonclothinglayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, Identifier textureLocation) {
        BoggedRenderer boggedrenderer = (BoggedRenderer)er;

        for (SkeletonClothingLayer skeletonclothinglayer : boggedrenderer.getLayers(SkeletonClothingLayer.class)) {
            skeletonclothinglayer.clothesLocation = textureLocation;
        }

        return true;
    }
}
