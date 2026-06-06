package net.conczin.immersive_optimization.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.entity.Entity;

public interface EntityTickListAccessor {
    Int2ObjectMap<Entity> getActive();
}
