package net.minecraftforge.client.extensions;

import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

public interface IForgeBlockModelPart {
    default ChunkSectionLayer layer() {
        return null;
    }

    default ChunkSectionLayer layerFast() {
        return null;
    }
}
