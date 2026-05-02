package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.ghast.HappyGhastHarnessModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterHappyGhastHarness extends ModelAdapterHappyGhast {

    public ModelAdapterHappyGhastHarness() {
        super(EntityType.HAPPY_GHAST, "happy_ghast_harness", ModelLayers.HAPPY_GHAST_HARNESS);
    }

    protected ModelAdapterHappyGhastHarness(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        return null;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new HappyGhastHarnessModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "harness");
        map.put("goggles", "goggles");
        map.put("root", "root");
        return map;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        HappyGhastHarnessModel happyghastharnessmodel = (HappyGhastHarnessModel) modelBase;

        for (Object layer : renderer.layers) {
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer simpleequipmentlayer = (SimpleEquipmentLayer) layer;
                if (simpleequipmentlayer.getLayerType() == EquipmentClientInfo.LayerType.HAPPY_GHAST_BODY) {
                    if (this.isBaby()) {
                        simpleequipmentlayer.babyModel = happyghastharnessmodel;
                    } else {
                        simpleequipmentlayer.adultModel = happyghastharnessmodel;
                    }
                }
            }
        }
    }
}