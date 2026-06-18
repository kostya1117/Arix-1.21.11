package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viafabricplus.settings.impl.DebugSettings;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class IronBarsBlock extends CrossCollisionBlock {
    public static final MapCodec<IronBarsBlock> CODEC = simpleCodec(IronBarsBlock::new);
    private VoxelShape[] viaFabricPlus$shape_r1_12_2;

    private VoxelShape[] viaFabricPlus$shape_r1_8;

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return CODEC;
    }

    protected IronBarsBlock(BlockBehaviour.Properties p_54198_) {
        super(2.0F, 16.0F, 2.0F, 16.0F, 16.0F, p_54198_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
                .setValue(WATERLOGGED, false)
        );
        final float f = 7.0F;
        final float g = 9.0F;
        final float h = 7.0F;
        final float i = 9.0F;

        final VoxelShape baseShape = Block.box(f, 0.0, f, g, (float) 16.0, g);

        viaFabricPlus$shape_r1_12_2 = new VoxelShape[]{
                baseShape,
                Block.box(h, 0.0, h, i, 16.0, 16.0), // south
                Block.box(0.0, 0.0, h, i, 16.0, i), // west
                Block.box(0.0, 0.0, h, i, 16.0, 16.0), // south-west corner
                Block.box(h, 0.0, 0.0, i, 16.0, i), // north
                Block.box(h, 0.0, 0.0, i, 16.0, 16.0), // south-north line
                Block.box(0.0, 0.0, 0.0, i, 16.0, i), // west-north corner
                Block.box(0.0, 0.0, 0.0, i, 16.0, 16.0), // south-west-north T
                Block.box(h, 0.0, h, 16.0, 16.0, i), // east
                Block.box(h, 0.0, h, 16.0, 16.0, 16.0), // south-east corner
                Block.box(0.0, 0.0, h, 16.0, 16.0, i), // west-east line
                Block.box(0.0, 0.0, h, 16.0, 16.0, 16.0), // south-west-east T
                Block.box(h, 0.0, 0.0, 16.0, 16.0, i), // north-east corner
                Block.box(h, 0.0, 0.0, 16.0, 16.0, 16.0), // south-north-east T
                Block.box(0.0, 0.0, 0.0, 16.0, 16.0, i), // west-north-east T
                Shapes.block() // cross
        };

        final VoxelShape northShape = Block.box(h, (float) 0.0, 0.0, i, (float) 16.0, i - 1);
        final VoxelShape southShape = Block.box(h, (float) 0.0, h + 1, i, (float) 16.0, 16.0);
        final VoxelShape westShape = Block.box(0.0, (float) 0.0, h, i - 1, (float) 16.0, i);
        final VoxelShape eastShape = Block.box(h + 1, (float) 0.0, h, 16.0, (float) 16.0, i);

        final VoxelShape northEastCornerShape = Shapes.or(northShape, eastShape);
        final VoxelShape southWestCornerShape = Shapes.or(southShape, westShape);

        viaFabricPlus$shape_r1_8 = new VoxelShape[]{
                baseShape,
                southShape,
                westShape,
                southWestCornerShape,
                northShape,
                Shapes.or(southShape, northShape),
                Shapes.or(westShape, northShape),
                Shapes.or(southWestCornerShape, northShape),
                eastShape,
                Shapes.or(southShape, eastShape),
                Shapes.or(westShape, eastShape),
                Shapes.or(southWestCornerShape, eastShape),
                northEastCornerShape,
                Shapes.or(southShape, northEastCornerShape),
                Shapes.or(westShape, northEastCornerShape),
                Shapes.or(southWestCornerShape, northEastCornerShape)
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_54200_) {
        BlockGetter blockgetter = p_54200_.getLevel();
        BlockPos blockpos = p_54200_.getClickedPos();
        FluidState fluidstate = p_54200_.getLevel().getFluidState(p_54200_.getClickedPos());
        BlockPos blockpos1 = blockpos.north();
        BlockPos blockpos2 = blockpos.south();
        BlockPos blockpos3 = blockpos.west();
        BlockPos blockpos4 = blockpos.east();
        BlockState blockstate = blockgetter.getBlockState(blockpos1);
        BlockState blockstate1 = blockgetter.getBlockState(blockpos2);
        BlockState blockstate2 = blockgetter.getBlockState(blockpos3);
        BlockState blockstate3 = blockgetter.getBlockState(blockpos4);

        boolean north = this.attachsTo(blockstate, blockstate.isFaceSturdy(blockgetter, blockpos1, Direction.SOUTH));
        boolean south = this.attachsTo(blockstate1, blockstate1.isFaceSturdy(blockgetter, blockpos2, Direction.NORTH));
        boolean west = this.attachsTo(blockstate2, blockstate2.isFaceSturdy(blockgetter, blockpos3, Direction.EAST));
        boolean east = this.attachsTo(blockstate3, blockstate3.isFaceSturdy(blockgetter, blockpos4, Direction.WEST));

        int count = 0;
        if (north) count++;
        if (south) count++;
        if (west) count++;
        if (east) count++;

        BlockState result = this.defaultBlockState()
                .setValue(NORTH, north)
                .setValue(SOUTH, south)
                .setValue(WEST, west)
                .setValue(EAST, east)
                .setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);

        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8) && count == 0) {
            result = result
                    .setValue(NORTH, true)
                    .setValue(SOUTH, true)
                    .setValue(WEST, true)
                    .setValue(EAST, true);
        }

        return result;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_54211_,
        LevelReader p_367146_,
        ScheduledTickAccess p_367530_,
        BlockPos p_54215_,
        Direction p_54212_,
        BlockPos p_54216_,
        BlockState p_54213_,
        RandomSource p_369110_
    ) {
        if (p_54211_.getValue(WATERLOGGED)) {
            p_367530_.scheduleTick(p_54215_, Fluids.WATER, Fluids.WATER.getTickDelay(p_367146_));
        }

        return p_54212_.getAxis().isHorizontal()
            ? p_54211_.setValue(PROPERTY_BY_DIRECTION.get(p_54212_), this.attachsTo(p_54213_, p_54213_.isFaceSturdy(p_367146_, p_54216_, p_54212_.getOpposite())))
            : super.updateShape(p_54211_, p_367146_, p_367530_, p_54215_, p_54212_, p_54216_, p_54213_, p_369110_);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState p_54202_, BlockGetter p_54203_, BlockPos p_54204_, CollisionContext p_54205_) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return (this.getCollisionShape(p_54202_, p_54203_, p_54204_, p_54205_));
        }
        return Shapes.empty();
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (DebugSettings.INSTANCE.legacyPaneOutlines.isEnabled()) {
            return this.viaFabricPlus$shape_r1_12_2[this.viaFabricPlus$getShapeIndex(state)];
        } else {
            return super.getShape(state, world, pos, context);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_8)) {
            return this.viaFabricPlus$shape_r1_8[this.viaFabricPlus$getShapeIndex(state)];
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }

    @Override
    protected boolean skipRendering(BlockState p_54207_, BlockState p_54208_, Direction p_54209_) {
        if (p_54208_.is(this)
            || p_54208_.is(BlockTags.BARS) && p_54207_.is(BlockTags.BARS) && p_54208_.hasProperty(PROPERTY_BY_DIRECTION.get(p_54209_.getOpposite()))) {
            if (!p_54209_.getAxis().isHorizontal()) {
                return true;
            }

            if (p_54207_.getValue(PROPERTY_BY_DIRECTION.get(p_54209_)) && p_54208_.getValue(PROPERTY_BY_DIRECTION.get(p_54209_.getOpposite()))) {
                return true;
            }
        }

        return super.skipRendering(p_54207_, p_54208_, p_54209_);
    }

    public final boolean attachsTo(BlockState p_54218_, boolean p_54219_) {
        return !isExceptionForConnection(p_54218_) && p_54219_ || p_54218_.getBlock() instanceof IronBarsBlock || p_54218_.is(BlockTags.WALLS);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_54221_) {
        p_54221_.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }
}
