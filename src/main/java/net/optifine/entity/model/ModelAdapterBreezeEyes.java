package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.BreezeEyesLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterBreezeEyes extends ModelAdapterBreeze {
    public ModelAdapterBreezeEyes() {
        super(EntityType.BREEZE, "breeze_eyes", ModelLayers.BREEZE_EYES);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new BreezeModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("head", "head");
        map.put("eyes", "eyes");
        map.put("root", "root");
        return map;
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        BreezeRenderer breezerenderer = (BreezeRenderer) renderer;
        Identifier identifier = modelBase.locationTextureCustom != null
                ? modelBase.locationTextureCustom
                : new Identifier("textures/entity/breeze/breeze_eyes.png");

        BreezeEyesLayer breezeeyeslayer = new BreezeEyesLayer(breezerenderer, this.getContext().getModelSet());
        breezeeyeslayer.setModel((BreezeModel) modelBase);
        breezeeyeslayer.setTextureLocation(identifier);

        breezerenderer.replaceLayer(BreezeEyesLayer.class, breezeeyeslayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, Identifier textureLocation) {
        BreezeRenderer breezerenderer = (BreezeRenderer) er;

        for (Object layer : breezerenderer.layers) {
            if (layer instanceof BreezeEyesLayer) {
                BreezeEyesLayer breezeeyeslayer = (BreezeEyesLayer) layer;
                breezeeyeslayer.setTextureLocation(textureLocation);
            }
        }

        return true;
    }
}