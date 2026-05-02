package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterPigSaddle extends ModelAdapterPig {
    public ModelAdapterPigSaddle() {
        super(EntityType.PIG, "pig_saddle", ModelLayers.PIG_SADDLE, null);
    }

    protected ModelAdapterPigSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer, PigType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterPigSaddle modeladapterpigsaddle = new ModelAdapterPigSaddle(
                this.getEntityType(), "pig_baby_saddle", ModelLayers.PIG_BABY_SADDLE, null
        );
        modeladapterpigsaddle.setBaby(true);
        modeladapterpigsaddle.setAlias(this.getName());
        return modeladapterpigsaddle;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model model, Object type, boolean baby) {
        PigModel pigmodel = (PigModel) model;

        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;

                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.PIG_SADDLE) {
                    if (baby) {
                        simpleequipmentlayer.babyModel = pigmodel;
                    } else {
                        simpleequipmentlayer.adultModel = pigmodel;
                    }
                }
            }
        }
    }
}