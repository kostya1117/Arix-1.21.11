package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Function;

import com.viaversion.viafabricplus.features.block.interaction.Block1_14;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LeadItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.raphimc.vialegacy.api.LegacyProtocolVersion;

public class FenceBlock extends CrossCollisionBlock {
    public static final MapCodec<FenceBlock> CODEC = simpleCodec(FenceBlock::new);
    private final Function<BlockState, VoxelShape> occlusionShapes;
    private final VoxelShape[] viaFabricPlus$outline_shape_r1_12_2 = new VoxelShape[]{
            Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D),
            Shapes.box(0.375D, 0.0D, 0.375D, 0.625D, 1.0D, 1.0D),
            Shapes.box(0.0D, 0.0D, 0.375D, 0.625D, 1.0D, 0.625D),
            Shapes.box(0.0D, 0.0D, 0.375D, 0.625D, 1.0D, 1.0D),
            Shapes.box(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 0.625D),
            Shapes.box(0.375D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D),
            Shapes.box(0.0D, 0.0D, 0.0D, 0.625D, 1.0D, 0.625D),
            Shapes.box(0.0D, 0.0D, 0.0D, 0.625D, 1.0D, 1.0D),
            Shapes.box(0.375D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D),
            Shapes.box(0.375D, 0.0D, 0.375D, 1.0D, 1.0D, 1.0D),
            Shapes.box(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 0.625D),
            Shapes.box(0.0D, 0.0D, 0.375D, 1.0D, 1.0D, 1.0D),
            Shapes.box(0.375D, 0.0D, 0.0D, 1.0D, 1.0D, 0.625D),
            Shapes.box(0.375D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D),
            Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.625D),
            Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D)
    };

    private final VoxelShape viaFabricPlus$shape_b1_8_1 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 24.0D, 16.0D);

    private VoxelShape[] viaFabricPlus$collision_shape_r1_4_7;

    private VoxelShape[] viaFabricPlus$outline_shape_r1_4_7;
    @Override
    public MapCodec<FenceBlock> codec() {
        return CODEC;
    }

    public FenceBlock(BlockBehaviour.Properties p_53302_) {
        super(4.0F, 16.0F, 4.0F, 16.0F, 24.0F, p_53302_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false)
        );
        this.occlusionShapes = this.makeShapes(4.0F, 16.0F, 2.0F, 6.0F, 15.0F);
        this.viaFabricPlus$collision_shape_r1_4_7 = this.viaFabricPlus$createShapes1_4_7(24.0F);
        this.viaFabricPlus$outline_shape_r1_4_7 = this.viaFabricPlus$createShapes1_4_7(16.0F);
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState p_53338_) {
        return this.occlusionShapes.apply(p_53338_);
    }
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_21)) {
            return stack.is(Items.LEAD) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_10)) {
            return InteractionResult.SUCCESS;
        } else {
            return super.useItemOn(stack, state, world, pos, player, hand, hit);
        }
    }

    @Override
    protected VoxelShape getVisualShape(BlockState p_53311_, BlockGetter p_53312_, BlockPos p_53313_, CollisionContext p_53314_) {
        return this.getShape(p_53311_, p_53312_, p_53313_, p_53314_);
    }

    @Override
    protected boolean isPathfindable(BlockState p_53306_, PathComputationType p_53309_) {
        return false;
    }

    public boolean connectsTo(BlockState p_53330_, boolean p_53331_, Direction p_53332_) {
        Block block = p_53330_.getBlock();
        boolean flag = this.isSameFence(p_53330_);
        boolean flag1 = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(p_53330_, p_53332_);

        boolean result = !isExceptionForConnection(p_53330_) && p_53331_ || flag || flag1;

        // ViaFabricPlus - 1.14 connection logic
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_14)) {
            if (!Block1_14.isExceptBlockForAttachWithPiston(p_53330_.getBlock())) {
                result = false;
            }
        }

        return result;
    }

    private boolean isSameFence(BlockState p_153255_) {
        return p_153255_.is(BlockTags.FENCES) && p_153255_.is(BlockTags.WOODEN_FENCES) == this.defaultBlockState().is(BlockTags.WOODEN_FENCES);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_328142_, Level p_333097_, BlockPos p_335860_, Player p_334259_, BlockHitResult p_333666_) {
        return !p_333097_.isClientSide() ? LeadItem.bindPlayerMobs(p_334259_, p_333097_, p_335860_) : InteractionResult.PASS;
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            return Shapes.block();
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
            return this.viaFabricPlus$outline_shape_r1_4_7[this.viaFabricPlus$getShapeIndex(state)];
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_12_2)) {
            return this.viaFabricPlus$outline_shape_r1_12_2[this.viaFabricPlus$getShapeIndex(state)];
        } else {
            return super.getShape(state, world, pos, context);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.b1_8tob1_8_1)) {
            return viaFabricPlus$shape_b1_8_1;
        } else if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(LegacyProtocolVersion.r1_4_6tor1_4_7)) {
            return this.viaFabricPlus$collision_shape_r1_4_7[this.viaFabricPlus$getShapeIndex(state)];
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }

    private VoxelShape[] viaFabricPlus$createShapes1_4_7(final float height) {
        final float f = 6.0F;
        final float g = 10.0F;
        final float h = 6.0F;
        final float i = 10.0F;
        final VoxelShape baseShape = Block.box(f, 0.0, f, g, height, g);
        final VoxelShape northShape = Block.box(h, (float) 0.0, 0.0, i, height, i);
        final VoxelShape southShape = Block.box(h, (float) 0.0, h, i, height, 16.0);
        final VoxelShape westShape = Block.box(0.0, (float) 0.0, h, i, height, i);
        final VoxelShape eastShape = Block.box(h, (float) 0.0, h, 16.0, height, i);
        final VoxelShape[] voxelShapes = new VoxelShape[]{
                Shapes.empty(),
                Block.box(f, (float) 0.0, h, g, height, 16.0D),
                Block.box(0.0D, (float) 0.0, f, i, height, g),
                Block.box(f - 6, (float) 0.0, h, g, height, 16.0D),
                Block.box(f, (float) 0.0, 0.0D, g, height, i),

                Shapes.or(southShape, northShape),
                Block.box(f - 6, (float) 0.0, 0.0D, g, height, i),
                Block.box(f - 6, (float) 0.0, h - 5, g, height, 16.0D),
                Block.box(h, (float) 0.0, f, 16.0D, height, g),
                Block.box(h, (float) 0.0, f, 16.0D, height, g + 6),

                Shapes.or(westShape, eastShape),
                Block.box(h - 5, (float) 0.0, f, 16.0D, height, g + 6),
                Block.box(f, (float) 0.0, 0.0D, g + 6, height, i),
                Block.box(f, (float) 0.0, 0.0D, g + 6, height, i + 5),
                Block.box(h - 5, (float) 0.0, f - 6, 16.0D, height, g),
                Block.box(0, (float) 0.0, 0, 16.0D, height, 16.0D)
        };

        for (int j = 0; j < 16; ++j) {
            voxelShapes[j] = Shapes.or(baseShape, voxelShapes[j]);
        }

        return voxelShapes;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_53304_) {
        BlockGetter blockgetter = p_53304_.getLevel();
        BlockPos blockpos = p_53304_.getClickedPos();
        FluidState fluidstate = p_53304_.getLevel().getFluidState(p_53304_.getClickedPos());
        BlockPos blockpos1 = blockpos.north();
        BlockPos blockpos2 = blockpos.east();
        BlockPos blockpos3 = blockpos.south();
        BlockPos blockpos4 = blockpos.west();
        BlockState blockstate = blockgetter.getBlockState(blockpos1);
        BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
        BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
        BlockState blockstate3 = blockgetter.getBlockState(blockpos4);
        return super.getStateForPlacement(p_53304_)
            .setValue(NORTH, this.connectsTo(blockstate, blockstate.isFaceSturdy(blockgetter, blockpos1, Direction.SOUTH), Direction.SOUTH))
            .setValue(EAST, this.connectsTo(blockstate1, blockstate1.isFaceSturdy(blockgetter, blockpos2, Direction.WEST), Direction.WEST))
            .setValue(SOUTH, this.connectsTo(blockstate2, blockstate2.isFaceSturdy(blockgetter, blockpos3, Direction.NORTH), Direction.NORTH))
            .setValue(WEST, this.connectsTo(blockstate3, blockstate3.isFaceSturdy(blockgetter, blockpos4, Direction.EAST), Direction.EAST))
            .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_53323_,
        LevelReader p_367370_,
        ScheduledTickAccess p_364464_,
        BlockPos p_53327_,
        Direction p_53324_,
        BlockPos p_53328_,
        BlockState p_53325_,
        RandomSource p_368641_
    ) {
        if (p_53323_.getValue(WATERLOGGED)) {
            p_364464_.scheduleTick(p_53327_, Fluids.WATER, Fluids.WATER.getTickDelay(p_367370_));
        }

        return p_53324_.getAxis().isHorizontal()
            ? p_53323_.setValue(
                PROPERTY_BY_DIRECTION.get(p_53324_), this.connectsTo(p_53325_, p_53325_.isFaceSturdy(p_367370_, p_53328_, p_53324_.getOpposite()), p_53324_.getOpposite())
            )
            : super.updateShape(p_53323_, p_367370_, p_364464_, p_53327_, p_53324_, p_53328_, p_53325_, p_368641_);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_53334_) {
        p_53334_.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}
