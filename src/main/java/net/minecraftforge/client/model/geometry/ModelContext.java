package net.minecraftforge.client.model.geometry;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraftforge.client.model.ForgeBlockModelData;

public record ModelContext(ResolvedModel self, ResolvedModel parent, ForgeBlockModelData data, boolean gui3d) implements IGeometryBakingContext {
    public ModelContext(ResolvedModel self) {
        this(null, null, null, false);
    }
}
