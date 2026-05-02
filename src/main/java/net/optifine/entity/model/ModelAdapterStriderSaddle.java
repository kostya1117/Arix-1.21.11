package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.strider.StriderModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterStriderSaddle extends ModelAdapterStrider {
    public ModelAdapterStriderSaddle() {
        super(EntityType.STRIDER, "strider_saddle", ModelLayers.STRIDER_SADDLE);
    }

    private ModelAdapterStriderSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterStriderSaddle modeladapterstridersaddle =
                new ModelAdapterStriderSaddle(this.getEntityType(), "strider_baby_saddle", ModelLayers.STRIDER_BABY_SADDLE);
        modeladapterstridersaddle.setBaby(true);
        modeladapterstridersaddle.setAlias(this.getName());
        return modeladapterstridersaddle;
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        StriderModel stridermodel = (StriderModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;

                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.STRIDER_SADDLE) {
                    if (this.isBaby()) {
                        simpleequipmentlayer.babyModel = stridermodel;
                    } else {
                        simpleequipmentlayer.adultModel = stridermodel;
                    }
                }
            }
        }
    }
}