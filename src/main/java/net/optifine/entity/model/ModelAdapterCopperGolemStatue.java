package net.optifine.entity.model;

import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.statue.CopperGolemStatueModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.CopperGolemStatueBlockRenderer;
import net.minecraft.world.level.block.CopperGolemStatueBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.optifine.Config;
import net.optifine.reflect.Reflector;

public class ModelAdapterCopperGolemStatue extends ModelAdapterBlockEntity {
    private CopperGolemStatueBlock.Pose pose;
    private ModelLayerLocation modelLayer;
    private static Map<String, String> mapParts = makeStaticMapParts();

    public ModelAdapterCopperGolemStatue(CopperGolemStatueBlock.Pose poseIn) {
        super(BlockEntityType.COPPER_GOLEM_STATUE, "copper_golem_statue" + getSuffix(poseIn));
        this.pose = poseIn;
        this.modelLayer = getModelLayer(poseIn);
    }

    private static String getSuffix(CopperGolemStatueBlock.Pose poseIn) {
        switch (poseIn) {
            case STANDING:
                return "";
            case SITTING:
                return "_sitting";
            case RUNNING:
                return "_running";
            case STAR:
                return "_star";
            default:
                Config.warn("Unknown copper golem statue pose: " + poseIn);
                return "";
        }
    }

    private static ModelLayerLocation getModelLayer(CopperGolemStatueBlock.Pose poseIn) {
        switch (poseIn) {
            case STANDING:
                return ModelLayers.COPPER_GOLEM;
            case SITTING:
                return ModelLayers.COPPER_GOLEM_SITTING;
            case RUNNING:
                return ModelLayers.COPPER_GOLEM_RUNNING;
            case STAR:
                return ModelLayers.COPPER_GOLEM_STAR;
            default:
                Config.warn("Unknown copper golem statue pose: " + poseIn);
                return ModelLayers.COPPER_GOLEM;
        }
    }

    @Override
    public Model makeModel() {
        return new CopperGolemStatueModel(bakeModelLayer(this.modelLayer));
    }

    @Override
    public ModelPart getModelRenderer(Model model, String modelPart) {
        return this.getModelRenderer(model.root(), mapParts.get(modelPart));
    }

    @Override
    public String[] getModelRendererNames() {
        return toArray(mapParts.keySet());
    }

    public static Map<String, String> makeStaticMapParts() {
        return ModelAdapterCopperGolem.makeStaticMapParts();
    }

    @Override
    public IEntityRenderer makeEntityRender(Model modelBase, RendererCache rendererCache, int index) {
        BlockEntityRenderer blockentityrenderer = rendererCache.get(
            BlockEntityType.COPPER_GOLEM_STATUE, index, () -> new CopperGolemStatueBlockRenderer(this.getContext())
        );
        if (!Reflector.CopperGolemStatueBlockRenderer_models.exists()) {
            throw new IllegalArgumentException("Field not found: CopperGolemStatueBlockRenderer.models");
        } else {
            Object object = Reflector.CopperGolemStatueBlockRenderer_models.getValue(blockentityrenderer);
            if (!(object instanceof Map map)) {
                throw new IllegalArgumentException("CopperGolemStatueBlockRenderer.models is not a map: " + object);
            } else {
                map.put(this.pose, modelBase);
                return blockentityrenderer;
            }
        }
    }
}
