package net.minecraft.client.color.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;


public interface BlockColor {
    int getColor(BlockState p_92567_,  BlockAndTintGetter p_92568_,  BlockPos p_92569_, int p_92570_);
}
