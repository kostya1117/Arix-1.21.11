package net.minecraft.client.renderer.chunk;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

public interface SectionMesh extends AutoCloseable {
    default boolean isDifferentPointOfView(TranslucencyPointOfView p_406250_) {
        return false;
    }

    default boolean hasRenderableLayers() {
        return false;
    }

    default boolean hasTranslucentGeometry() {
        return false;
    }

    default boolean isEmpty(ChunkSectionLayer p_410400_) {
        return true;
    }

    default List<BlockEntity> getRenderableBlockEntities() {
        return Collections.emptyList();
    }

    boolean facesCanSeeEachother(Direction p_407864_, Direction p_408147_);

    default @Nullable SectionBuffers getBuffers(ChunkSectionLayer p_409041_) {
        return null;
    }

    @Override
    default void close() {
    }

    default BitSet getAnimatedSprites(ChunkSectionLayer layerIn) {
        return null;
    }

    default void setAnimatedSprites(BitSet[] animatedSprites) {
    }

    default boolean isLayerUsed(ChunkSectionLayer layerIn) {
        return false;
    }

    default boolean hasTerrainBlockEntities() {
        return false;
    }

    default boolean isEmpty() {
        return true;
    }

    default Set<ChunkSectionLayer> getLayersUsed() {
        return Set.of();
    }
}
