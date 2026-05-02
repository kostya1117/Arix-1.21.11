package net.minecraft.client.renderer.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.event.RenderHighlightEvent;
import org.jspecify.annotations.Nullable;

public record BlockOutlineRenderState(
    BlockPos pos,
    boolean isTranslucent,
    boolean highContrast,
    VoxelShape shape,
    VoxelShape collisionShape,
    VoxelShape occlusionShape,
    VoxelShape interactionShape,
    RenderHighlightEvent.Callback customRenderer,
    BlockState blockState
) {
    public BlockOutlineRenderState(
        BlockPos pos,
        boolean isTranslucent,
        boolean highContrast,
        VoxelShape shape,
        @Nullable VoxelShape collisionShape,
        @Nullable VoxelShape occlusionShape,
        @Nullable VoxelShape interactionShape
    ) {
        this(pos, isTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, null, null);
    }

    public BlockOutlineRenderState(
        BlockPos pos,
        boolean isTranslucent,
        boolean highContrast,
        VoxelShape shape,
        @Nullable VoxelShape collisionShape,
        @Nullable VoxelShape occlusionShape,
        @Nullable VoxelShape interactionShape,
        RenderHighlightEvent.Callback customRenderer
    ) {
        this(pos, isTranslucent, highContrast, shape, collisionShape, occlusionShape, interactionShape, customRenderer, null);
    }

    public BlockOutlineRenderState(BlockPos posIn, boolean translucentIn, boolean contrastIn, VoxelShape shapeIn, BlockState blockState) {
        this(posIn, translucentIn, contrastIn, shapeIn, null, null, null, null, blockState);
    }

    public BlockOutlineRenderState(BlockPos p_425529_, boolean p_426298_, boolean p_426881_, VoxelShape p_424013_) {
        this(p_425529_, p_426298_, p_426881_, p_424013_, null, null, null);
    }
}
