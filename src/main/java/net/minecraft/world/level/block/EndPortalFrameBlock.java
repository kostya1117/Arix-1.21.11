package net.minecraft.world.level.block;

import com.google.common.base.Predicates;
import com.mojang.serialization.MapCodec;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.block.state.pattern.BlockPattern;
import net.minecraft.world.level.block.state.pattern.BlockPatternBuilder;
import net.minecraft.world.level.block.state.predicate.BlockStatePredicate;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.raphimc.viabedrock.api.BedrockProtocolVersion;
import org.jspecify.annotations.Nullable;

public class EndPortalFrameBlock extends Block {
    public static final MapCodec<EndPortalFrameBlock> CODEC = simpleCodec(EndPortalFrameBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty HAS_EYE = BlockStateProperties.EYE;
    private static final VoxelShape SHAPE_EMPTY = Block.column(16.0, 0.0, 13.0);
    private static final VoxelShape SHAPE_FULL = Shapes.or(SHAPE_EMPTY, Block.column(8.0, 13.0, 16.0));
    private static @Nullable BlockPattern portalShape;
    private static final VoxelShape viaFabricPlus$eye_shape_r1_12_2 = Block.box(5.0D, 13.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape viaFabricPlus$frame_with_eye_shape_r1_12_2 = Shapes.or(SHAPE_EMPTY, viaFabricPlus$eye_shape_r1_12_2);
    private static final VoxelShape viaFabricPlus$shape_frame_bedrock = Shapes.box(0, 0, 0, 1, 0.8125, 1);

    @Override
    public MapCodec<EndPortalFrameBlock> codec() {
        return CODEC;
    }

    public EndPortalFrameBlock(BlockBehaviour.Properties p_53050_) {
        super(p_53050_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HAS_EYE, false));
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState p_53079_) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState p_53073_, BlockGetter p_53074_, BlockPos p_53075_, CollisionContext p_53076_) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            return (SHAPE_EMPTY);
        } else if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            // The eye doesn't have a different shape on bedrock
            return (viaFabricPlus$shape_frame_bedrock);
        }

        return p_53073_.getValue(HAS_EYE) ? SHAPE_FULL : SHAPE_EMPTY;
    }
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            return state.getValue(HAS_EYE) ? viaFabricPlus$frame_with_eye_shape_r1_12_2 : SHAPE_EMPTY;
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state) {
        if (ProtocolTranslator.getTargetVersion().equals(BedrockProtocolVersion.bedrockLatest)) {
            return SHAPE_EMPTY;
        } else {
            return super.getOcclusionShape(state);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_53052_) {
        return this.defaultBlockState().setValue(FACING, p_53052_.getHorizontalDirection().getOpposite()).setValue(HAS_EYE, false);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState p_53054_) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState p_53061_, Level p_53062_, BlockPos p_53063_, Direction p_425251_) {
        return p_53061_.getValue(HAS_EYE) ? 15 : 0;
    }

    @Override
    protected BlockState rotate(BlockState p_53068_, Rotation p_53069_) {
        return p_53068_.setValue(FACING, p_53069_.rotate(p_53068_.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState p_53065_, Mirror p_53066_) {
        return p_53065_.rotate(p_53066_.getRotation(p_53065_.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53071_) {
        p_53071_.add(FACING, HAS_EYE);
    }

    public static BlockPattern getOrCreatePortalShape() {
        if (portalShape == null) {
            portalShape = BlockPatternBuilder.start()
                .aisle("?vvv?", ">???<", ">???<", ">???<", "?^^^?")
                .where('?', BlockInWorld.hasState(BlockStatePredicate.ANY))
                .where(
                    '^',
                    BlockInWorld.hasState(
                        BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                            .where(HAS_EYE, Predicates.equalTo(true))
                            .where(FACING, Predicates.equalTo(Direction.SOUTH))
                    )
                )
                .where(
                    '>',
                    BlockInWorld.hasState(
                        BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                            .where(HAS_EYE, Predicates.equalTo(true))
                            .where(FACING, Predicates.equalTo(Direction.WEST))
                    )
                )
                .where(
                    'v',
                    BlockInWorld.hasState(
                        BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                            .where(HAS_EYE, Predicates.equalTo(true))
                            .where(FACING, Predicates.equalTo(Direction.NORTH))
                    )
                )
                .where(
                    '<',
                    BlockInWorld.hasState(
                        BlockStatePredicate.forBlock(Blocks.END_PORTAL_FRAME)
                            .where(HAS_EYE, Predicates.equalTo(true))
                            .where(FACING, Predicates.equalTo(Direction.EAST))
                    )
                )
                .build();
        }

        return portalShape;
    }

    @Override
    protected boolean isPathfindable(BlockState p_53056_, PathComputationType p_53059_) {
        return false;
    }
}
