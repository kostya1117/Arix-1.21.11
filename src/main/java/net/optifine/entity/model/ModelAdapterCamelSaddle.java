package net.optifine.entity.model;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.camel.CamelModel;
import net.minecraft.client.model.animal.camel.CamelSaddleModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterCamelSaddle extends ModelAdapterCamel {
    public ModelAdapterCamelSaddle() {
        super(EntityType.CAMEL, "camel_saddle", ModelLayers.CAMEL_SADDLE);
    }

    protected ModelAdapterCamelSaddle(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    public ModelAdapter makeBaby() {
        ModelAdapterCamelSaddle ma = new ModelAdapterCamelSaddle(this.getEntityType(), "camel_baby_saddle", ModelLayers.CAMEL_BABY_SADDLE);
        ma.setBaby(true);
        ma.setAlias(this.getName());
        return ma;
    }

    protected Model makeModel(ModelPart root) {
        return new CamelSaddleModel(root);
    }

    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        map = appendSaddleParts(map);
        return map;
    }

    public static Map<String, String> appendSaddleParts(Map<String, String> map) {
        map.put("saddle", "saddle");
        map.put("reins", "reins");
        map.put("bridle", "bridle");
        return map;
    }

    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        CamelModel camelModel = (CamelModel)modelBase;
        List<RenderLayer> layers = this.getRenderLayers(renderer, SimpleEquipmentLayer.class);

        for (RenderLayer renderLayer : layers) {
            SimpleEquipmentLayer layer = (SimpleEquipmentLayer) renderLayer;
            if (layer.getLayerType() == LayerType.CAMEL_SADDLE) {
                if (this.isBaby()) {
                    layer.babyModel = camelModel;
                } else {
                    layer.adultModel = camelModel;
                }
            }
        }

    }
}
