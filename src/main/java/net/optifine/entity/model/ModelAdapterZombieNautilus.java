package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.animal.nautilus.NautilusModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.ZombieNautilusRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.nautilus.ZombieNautilusVariant;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterZombieNautilus extends ModelAdapterMultiLiving {
    public ModelAdapterZombieNautilus() {
        super(EntityType.ZOMBIE_NAUTILUS, "zombie_nautilus", ModelLayers.ZOMBIE_NAUTILUS, ZombieNautilusVariant.ModelType.NORMAL);
    }

    protected ModelAdapterZombieNautilus(EntityType entityType, String name, ModelLayerLocation modelLayer, ZombieNautilusVariant.ModelType type) {
        super(entityType, name, modelLayer, type);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new NautilusModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        return ModelAdapterNautilus.makeStaticMapParts();
    }

    @Override
    protected LivingEntityRenderer makeLivingRenderer(EntityRendererProvider.Context context) {
        return new ZombieNautilusRenderer(context);
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase, Object type, boolean baby) {
        ZombieNautilusRenderer zombienautilusrenderer = (ZombieNautilusRenderer)renderer;
        NautilusModel nautilusmodel = (NautilusModel)modelBase;
        ZombieNautilusVariant.ModelType zombienautilusvariant$modeltype = (ZombieNautilusVariant.ModelType)type;
        Map<ZombieNautilusVariant.ModelType, NautilusModel> map = (Map<ZombieNautilusVariant.ModelType, NautilusModel>)Reflector.ZombieNautilusRenderer_models
            .getValue(zombienautilusrenderer);
        if (map == null) {
            Config.warn("ZombieNautilusRenderer.models not found.");
        } else {
            map.put(zombienautilusvariant$modeltype, nautilusmodel);
        }
    }
}
