package net.conczin.immersive_optimization.mixin;

import net.minecraft.world.level.entity.EntityTickList;

public interface ServerLevelAccessor {
    EntityTickList getEntityTickList();
}
