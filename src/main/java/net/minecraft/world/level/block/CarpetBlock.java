package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CarpetBlock extends Block {
    public static final MapCodec<CarpetBlock> CODEC = simpleCodec(CarpetBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 1.0);
    private static final VoxelShape viaFabricPlus$shape_r1_7_10 = Block.box(0.0D, -0.00001D /* 0.0D */, 0.0D, 16.0D, 0.0D, 16.0D);


    @Override
    public MapCodec<? extends CarpetBlock> codec() {
        return CODEC;
    }

    public CarpetBlock(BlockBehaviour.Properties p_152915_) {
        super(p_152915_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_152917_, BlockGetter p_152918_, BlockPos p_152919_, CollisionContext p_152920_) {
        return SHAPE;
    }
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_7_6)) {
            return viaFabricPlus$shape_r1_7_10;
        } else {
            return super.getCollisionShape(state, world, pos, context);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_152926_,
        LevelReader p_367863_,
        ScheduledTickAccess p_362101_,
        BlockPos p_152930_,
        Direction p_152927_,
        BlockPos p_152931_,
        BlockState p_152928_,
        RandomSource p_362637_
    ) {
        return !p_152926_.canSurvive(p_367863_, p_152930_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_152926_, p_367863_, p_362101_, p_152930_, p_152927_, p_152931_, p_152928_, p_362637_);
    }

    @Override
    protected boolean canSurvive(BlockState p_152922_, LevelReader p_152923_, BlockPos p_152924_) {
        return !p_152923_.isEmptyBlock(p_152924_.below());
    }
}
