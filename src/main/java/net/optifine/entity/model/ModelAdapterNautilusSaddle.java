package net.optifine.entity.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.nautilus.NautilusSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

import java.util.LinkedHashMap;
import java.util.Map;


public class ModelAdapterNautilusSaddle extends ModelAdapterNautilus {

    public ModelAdapterNautilusSaddle() {
        super(EntityType.NAUTILUS, "nautilus_saddle", ModelLayers.NAUTILUS_SADDLE);
    }

    protected ModelAdapterNautilusSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        return null;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new NautilusSaddleModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("shell", "shell");
        map.put("root", "root");
        return map;
    }

    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        NautilusSaddleModel nautilussaddlemodel = (NautilusSaddleModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;
                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.NAUTILUS_SADDLE) {
                    if (this.isBaby()) {
                        simpleequipmentlayer.babyModel = nautilussaddlemodel;
                    } else {
                        simpleequipmentlayer.adultModel = nautilussaddlemodel;
                    }
                }
            }
        }
    }
}
