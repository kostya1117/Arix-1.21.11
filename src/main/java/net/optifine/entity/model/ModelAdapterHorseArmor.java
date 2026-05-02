package net.optifine.entity.model;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterHorseArmor extends ModelAdapterHorse {
    public ModelAdapterHorseArmor() {
        super(EntityType.HORSE, "horse_armor", ModelLayers.HORSE_ARMOR);
    }

    protected ModelAdapterHorseArmor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    public ModelAdapter makeBaby() {
        ModelAdapterHorseArmor ma = new ModelAdapterHorseArmor(this.getEntityType(), "horse_baby_armor", ModelLayers.HORSE_BABY_ARMOR);
        ma.setBaby(true);
        return ma;
    }

    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        HorseModel horseModel = (HorseModel) modelBase;

        // Прямой перебор слоев рендерера
        for (Object layer : renderer.layers) { // В некоторых маппингах renderer.getLayers()
            if (layer instanceof SimpleEquipmentLayer) {
                SimpleEquipmentLayer eqLayer = (SimpleEquipmentLayer) layer;
                if (eqLayer.getLayerType() == LayerType.HORSE_BODY) {
                    if (this.isBaby()) {
                        eqLayer.babyModel = horseModel;
                    } else {
                        eqLayer.adultModel = horseModel;
                    }
                }
            }
        }
    }
}
