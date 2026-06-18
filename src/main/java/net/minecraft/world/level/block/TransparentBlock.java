package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.viaversion.viafabricplus.protocoltranslator.ProtocolTranslator;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TransparentBlock extends HalfTransparentBlock {
    public static final MapCodec<TransparentBlock> CODEC = simpleCodec(TransparentBlock::new);

    protected TransparentBlock(BlockBehaviour.Properties p_312723_) {
        super(p_312723_);
    }

    @Override
    protected MapCodec<? extends TransparentBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getVisualShape(BlockState p_312193_, BlockGetter p_310654_, BlockPos p_310658_, CollisionContext p_311129_) {
        if (ProtocolTranslator.getTargetVersion().olderThanOrEqualTo(ProtocolVersion.v1_15_2)) {
            return (this.getCollisionShape(p_312193_, p_310654_, p_310658_, p_311129_));
        }
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState p_312407_, BlockGetter p_310193_, BlockPos p_311965_) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_312717_) {
        return true;
    }
}
