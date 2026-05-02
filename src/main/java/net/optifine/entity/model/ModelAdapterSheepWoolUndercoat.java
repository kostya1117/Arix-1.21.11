package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SheepWoolUndercoatLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterSheepWoolUndercoat extends ModelAdapterSheep {
    public ModelAdapterSheepWoolUndercoat() {
        super(EntityType.SHEEP, "sheep_wool_undercoat", ModelLayers.SHEEP_WOOL_UNDERCOAT);
        this.setAlias("sheep");
    }

    protected ModelAdapterSheepWoolUndercoat(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSheepWoolUndercoat modeladaptersheepwoolundercoat = new ModelAdapterSheepWoolUndercoat(
                this.getEntityType(), "sheep_baby_wool_undercoat", ModelLayers.SHEEP_BABY_WOOL_UNDERCOAT
        );
        modeladaptersheepwoolundercoat.setBaby(true);
        modeladaptersheepwoolundercoat.setAlias(this.getName());
        return modeladaptersheepwoolundercoat;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new SheepFurModel(root);
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        SheepFurModel sheepfurmodel = (SheepFurModel) modelBase;

        boolean hasAdultModel = Reflector.SheepWoolUndercoatLayer_adultModel.exists();
        boolean hasBabyModel = Reflector.SheepWoolUndercoatLayer_babyModel.exists();

        if (!hasAdultModel) {
            Config.warn("Field not found: SheepWoolUndercoatLayer.adultModel");
        }

        if (!hasBabyModel) {
            Config.warn("Field not found: SheepWoolUndercoatLayer.babyModel");
        }

        for (Object layer : renderer.layers) {
            if (layer instanceof SheepWoolUndercoatLayer) {
                SheepWoolUndercoatLayer sheepwoolundercoatlayer = (SheepWoolUndercoatLayer) layer;

                if (this.isBaby()) {
                    if (hasBabyModel) {
                        Reflector.SheepWoolUndercoatLayer_babyModel.setValue(sheepwoolundercoatlayer, sheepfurmodel);
                    }
                } else {
                    if (hasAdultModel) {
                        Reflector.SheepWoolUndercoatLayer_adultModel.setValue(sheepwoolundercoatlayer, sheepfurmodel);
                    }
                }
            }
        }
    }
}