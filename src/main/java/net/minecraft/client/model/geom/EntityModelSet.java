package net.minecraft.client.model.geom;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import net.minecraft.client.model.geom.builders.LayerDefinition;

public class EntityModelSet {
    public static final EntityModelSet EMPTY = new EntityModelSet(Map.of());
    private final Map<ModelLayerLocation, LayerDefinition> roots;

    public EntityModelSet(Map<ModelLayerLocation, LayerDefinition> p_378805_) {
        this.roots = p_378805_;
    }

    public ModelPart bakeLayer(ModelLayerLocation p_171104_) {
        LayerDefinition layerdefinition = this.roots.get(p_171104_);
        if (layerdefinition == null) {
            throw new IllegalArgumentException("No model for layer " + p_171104_);
        }

        ModelPart modelpart = layerdefinition.bakeRoot();
        modelpart.setModelLayerLocation(p_171104_);
        return modelpart;
    }

    public static EntityModelSet vanilla() {
        return new EntityModelSet(ImmutableMap.copyOf(LayerDefinitions.createRoots()));
    }
}
