package net.minecraftforge.common.capabilities;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class CapabilityProvider<B> {
    protected CapabilityProvider() {
    }

    public final void gatherCapabilities() {
    }

    protected final CapabilityDispatcher getCapabilities() {
        return null;
    }

    protected final void deserializeCaps(HolderLookup.Provider registryAccess, CompoundTag tag) {
    }

    protected final CompoundTag serializeCaps(HolderLookup.Provider registryAccess) {
        return null;
    }

    protected final CompoundTag serializeCaps(ValueOutput output) {
        return null;
    }

    public void invalidateCaps() {
    }

    public static class BlockEntities extends CapabilityProvider<BlockEntity> {
        protected BlockEntities() {
        }
    }
}
