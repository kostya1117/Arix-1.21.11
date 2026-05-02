package net.minecraft.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public interface DispensibleContainerItem {
    default void checkExtraContent( LivingEntity p_391486_, Level p_150818_, ItemStack p_150819_, BlockPos p_150820_) {
    }

    boolean emptyContents( LivingEntity p_396492_, Level p_150822_, BlockPos p_150823_,  BlockHitResult p_150824_);
}
