package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.equine.EquineSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterDonkeySaddle extends ModelAdapterDonkey {
    public ModelAdapterDonkeySaddle() {
        super(EntityType.DONKEY, "donkey_saddle", ModelLayers.DONKEY_SADDLE);
    }

    protected ModelAdapterDonkeySaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterDonkeySaddle modeladapterdonkeysaddle = new ModelAdapterDonkeySaddle(
                this.getEntityType(), "donkey_baby_saddle", ModelLayers.DONKEY_BABY_SADDLE
        );
        modeladapterdonkeysaddle.setBaby(true);
        return modeladapterdonkeysaddle;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new EquineSaddleModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        return ModelAdapterHorseSaddle.appendSaddleParts(map);
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        EquineSaddleModel equinesaddlemodel = (EquineSaddleModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;

                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.DONKEY_SADDLE) {
                    if (this.isBaby()) {
                        simpleequipmentlayer.babyModel = equinesaddlemodel;
                    } else {
                        simpleequipmentlayer.adultModel = equinesaddlemodel;
                    }
                }
            }
        }
    }
}