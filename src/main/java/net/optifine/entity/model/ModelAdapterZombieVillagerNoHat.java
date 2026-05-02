package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.zombie.ZombieVillagerModel;
import net.minecraft.client.renderer.entity.AgeableMobRenderer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.world.entity.EntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterZombieVillagerNoHat extends ModelAdapterZombieVillager {
    public ModelAdapterZombieVillagerNoHat() {
        super(EntityType.ZOMBIE_VILLAGER, "zombie_villager_no_hat", ModelLayers.ZOMBIE_VILLAGER_NO_HAT);
        this.setAlias("zombie_villager");
    }

    protected ModelAdapterZombieVillagerNoHat(EntityType type, String name, ModelLayerLocation modelLayer) {
        super(type, name, modelLayer);
    }

    @Override
    public ModelAdapter makeBaby() {
        ModelAdapterZombieVillagerNoHat modeladapterzombievillagernohat = new ModelAdapterZombieVillagerNoHat(
                this.getEntityType(), "zombie_villager_baby_no_hat", ModelLayers.ZOMBIE_VILLAGER_BABY_NO_HAT
        );
        modeladapterzombievillagernohat.setBaby(true);
        modeladapterzombievillagernohat.setAlias(this.getName());
        return modeladapterzombievillagernohat;
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new ZombieVillagerModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = super.makeMapParts();
        map.remove("headwear");
        return map;
    }

    @Override
    public String[] getIgnoredPartNames() {
        return new String[]{"headwear"};
    }

    @Override
    public void finalizeModel(Model model) {
        ModelPart modelpart = this.getModelRenderer(model, "head");
        if (modelpart != null) {
            modelpart.removeCubesDeep();
        }
    }

    @Override
    protected void modifyAgeableRenderer(AgeableMobRenderer renderer, Model modelBase) {
        boolean hasAdultModel = Reflector.VillagerProfessionLayer_adultModel.exists();
        boolean hasBabyModel = Reflector.VillagerProfessionLayer_babyModel.exists();

        if (!hasAdultModel) {
            Config.warn("Field not found: VillagerProfessionLayer.adultModel");
        }

        if (!hasBabyModel) {
            Config.warn("Field not found: VillagerProfessionLayer.babyModel");
        }

        for (Object layer : renderer.layers) {
            if (layer instanceof VillagerProfessionLayer) {
                VillagerProfessionLayer villagerprofessionlayer = (VillagerProfessionLayer) layer;

                if (this.isBaby()) {
                    if (hasBabyModel) {
                        Reflector.VillagerProfessionLayer_babyModel.setValue(villagerprofessionlayer, modelBase);
                    }
                } else {
                    if (hasAdultModel) {
                        Reflector.VillagerProfessionLayer_adultModel.setValue(villagerprofessionlayer, modelBase);
                    }
                }
            }
        }
    }
}