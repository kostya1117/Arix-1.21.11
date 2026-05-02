package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterSkeletonHorseArmor extends ModelAdapterSkeletonHorse {
    public ModelAdapterSkeletonHorseArmor() {
        super(EntityType.SKELETON_HORSE, "skeleton_horse_armor", ModelLayers.UNDEAD_HORSE_ARMOR);
    }

    protected ModelAdapterSkeletonHorseArmor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterSkeletonHorseArmor modeladapterskeletonhorsearmor = new ModelAdapterSkeletonHorseArmor(
                this.getEntityType(), "skeleton_horse_baby_armor", ModelLayers.UNDEAD_HORSE_BABY_ARMOR
        );
        modeladapterskeletonhorsearmor.setBaby(true);
        return modeladapterskeletonhorsearmor;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        HorseModel horsemodel = (HorseModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;

                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.HORSE_BODY) {
                    if (this.isBaby()) {
                        simpleequipmentlayer.babyModel = horsemodel;
                    } else {
                        simpleequipmentlayer.adultModel = horsemodel;
                    }
                }
            }
        }
    }
}