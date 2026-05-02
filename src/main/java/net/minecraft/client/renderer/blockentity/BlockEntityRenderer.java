package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.optifine.entity.model.IEntityRenderer;
import net.optifine.util.Either;
import org.jspecify.annotations.Nullable;

public interface BlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends IEntityRenderer {
    IdentityHashMap<BlockEntityRenderer, BlockEntityType> CACHED_TYPES = new IdentityHashMap<>();

    S createRenderState();

    default void extractRenderState(T p_426293_, S p_424933_, float p_423635_, Vec3 p_431515_, ModelFeatureRenderer. CrumblingOverlay p_426172_) {
        BlockEntityRenderState.extractBase(p_426293_, p_424933_, p_426172_);
    }

    void submit(S p_422918_, PoseStack p_112309_, SubmitNodeCollector p_424306_, CameraRenderState p_422688_);

    default boolean shouldRenderOffScreen() {
        return false;
    }

    default int getViewDistance() {
        return 64;
    }

    default boolean shouldRender(T p_173568_, Vec3 p_173569_) {
        return Vec3.atCenterOf(p_173568_.getBlockPos()).closerThan(p_173569_, this.getViewDistance());
    }

    @Override
    default Either<EntityType, BlockEntityType> getType() {
        BlockEntityType blockentitytype = CACHED_TYPES.get(this);
        return blockentitytype == null ? null : Either.makeRight(blockentitytype);
    }

    @Override
    default void setType(Either<EntityType, BlockEntityType> type) {
        CACHED_TYPES.put(this, type.getRight().get());
    }

    @Override
    default void setShadowSize(float shadowSize) {
    }

    @Override
    default Identifier getLocationTextureCustom() {
        return null;
    }

    @Override
    default void setLocationTextureCustom(Identifier locationTextureCustom) {
    }
}
