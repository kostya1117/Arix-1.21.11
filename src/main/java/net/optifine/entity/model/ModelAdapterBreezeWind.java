package net.optifine.entity.model;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.entity.BreezeRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.BreezeWindLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

public class ModelAdapterBreezeWind extends ModelAdapterBreeze {
    public ModelAdapterBreezeWind() {
        super(EntityType.BREEZE, "breeze_wind", ModelLayers.BREEZE_WIND);
    }

    @Override
    protected Model makeModel(ModelPart root) {
        return new BreezeModel(root);
    }

    @Override
    public Map<String, String> makeMapParts() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("body", "body");
        map.put("wind_body", "wind_body");
        map.put("wind_middle", "wind_mid");
        map.put("wind_bottom", "wind_bottom");
        map.put("wind_top", "wind_top");
        map.put("root", "root");
        return map;
    }

    @Override
    protected void modifyLivingRenderer(LivingEntityRenderer renderer, Model modelBase) {
        BreezeRenderer breezerenderer = (BreezeRenderer) renderer;
        Identifier identifier = modelBase.locationTextureCustom != null
                ? modelBase.locationTextureCustom
                : new Identifier("textures/entity/breeze/breeze_wind.png");

        BreezeWindLayer breezewindlayer = new BreezeWindLayer(breezerenderer, this.getContext().getModelSet());
        breezewindlayer.setModel((BreezeModel) modelBase);
        breezewindlayer.setTextureLocation(identifier);
        breezerenderer.replaceLayer(BreezeWindLayer.class, breezewindlayer);
    }

    @Override
    public boolean setTextureLocation(IEntityRenderer er, Identifier textureLocation) {
        BreezeRenderer breezerenderer = (BreezeRenderer) er;

        for (Object layer : breezerenderer.layers) {
            if (layer instanceof BreezeWindLayer) {
                BreezeWindLayer breezewindlayer = (BreezeWindLayer) layer;
                breezewindlayer.setTextureLocation(textureLocation);
            }
        }

        return true;
    }
}