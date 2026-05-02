package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSheepWool extends ModelAdapterSheep {

    public ModelAdapterSheepWool() {
        super(EntityType.SHEEP, "sheep_wool", ModelLayers.SHEEP_WOOL);
    }

    protected ModelAdapterSheepWool(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSheepWool modeladaptersheepwool = new ModelAdapterSheepWool(this.getEntityType(), "sheep_baby_wool", ModelLayers.SHEEP_BABY_WOOL);
        modeladaptersheepwool.setBaby(true);
        modeladaptersheepwool.setAlias(this.getName());
        return modeladaptersheepwool;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SheepFurModel(root);
    }

    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        SheepFurModel sheepfurmodel = (SheepFurModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof SheepWoolLayer) {
                SheepWoolLayer sheepwoollayer = (SheepWoolLayer) layer;

                if (this.isBaby()) {
                    sheepwoollayer.babyModel = sheepfurmodel;
                } else {
                    sheepwoollayer.adultModel = sheepfurmodel;
                }
            }
        }
    }
}