package net.optifine.entity.model;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.equine.HorseModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterZombieHorseArmor extends ModelAdapterZombieHorse {
    public ModelAdapterZombieHorseArmor() {
        super(EntityType.ZOMBIE_HORSE, "zombie_horse_armor", ModelLayers.UNDEAD_HORSE_ARMOR);
    }

    protected ModelAdapterZombieHorseArmor(EntityType entityType, String name, ModelLayerLocation modelLayer) {
        super(entityType, name, modelLayer);
    }

    public ModelAdapter makeBaby() {
        ModelAdapterZombieHorseArmor ma = new ModelAdapterZombieHorseArmor(this.getEntityType(), "zombie_horse_baby_armor", ModelLayers.UNDEAD_HORSE_BABY_ARMOR);
        ma.setBaby(true);
        return ma;
    }

    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        HorseModel horseModel = (HorseModel)modelBase;
        List<RenderLayer> layers = this.getRenderLayers(renderer, SimpleEquipmentLayer.class);

        for (RenderLayer renderLayer : layers) {
            SimpleEquipmentLayer layer = (SimpleEquipmentLayer) renderLayer;
            if (layer.getLayerType() == LayerType.HORSE_BODY) {
                if (this.isBaby()) {
                    layer.babyModel = horseModel;
                } else {
                    layer.adultModel = horseModel;
                }
            }
        }

    }
}
