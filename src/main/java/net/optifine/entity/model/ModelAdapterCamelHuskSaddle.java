package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCamelHuskSaddle extends ModelAdapterCamelHusk {
    public ModelAdapterCamelHuskSaddle() {
        super(EntityType.CAMEL_HUSK, "camel_husk_saddle", ModelLayers.CAMEL_HUSK_SADDLE);
    }

    protected ModelAdapterCamelHuskSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterCamelHuskSaddle modeladaptercamelhusksaddle = new ModelAdapterCamelHuskSaddle(
                this.getEntityType(), "camel_husk_baby_saddle", ModelLayers.CAMEL_HUSK_BABY_SADDLE
        );
        modeladaptercamelhusksaddle.setBaby(true);
        modeladaptercamelhusksaddle.setAlias(this.getName());
        return modeladaptercamelhusksaddle;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new CamelSaddleModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        return ModelAdapterCamelSaddle.appendSaddleParts(map);
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        CamelSaddleModel camelsaddlemodel = (CamelSaddleModel) modelBase;
        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;
                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.CAMEL_HUSK_SADDLE) {
                    if (this.isBaby()) {
                        simpleequipmentlayer.babyModel = camelsaddlemodel;
                    } else {
                        simpleequipmentlayer.adultModel = camelsaddlemodel;
                    }
                }
            }
        }
    }
}