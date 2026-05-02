package net.minecraftforge.client.extensions;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

public interface IForgeBlockStateModel {
    private BlockStateModel self() {
        return (BlockStateModel)this;
    }

    private BlockStateModel getBakedModel() {
        return (BlockStateModel)this;
    }

    default ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        throw new UnsupportedOperationException();
    }

    default TextureAtlasSprite getParticleIcon(ModelData data) {
        throw new UnsupportedOperationException();
    }

    default Collection<ChunkSectionLayer> getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        throw new UnsupportedOperationException();
    }

    default List<BlockModelPart> collectParts(RandomSource random, ModelData data, ChunkSectionLayer renderType) {
        List<BlockModelPart> list = new ObjectArrayList<>();
        this.collectParts(random, list, data, renderType);
        return list;
    }

    default void collectParts(RandomSource random, List<BlockModelPart> dest, ModelData data, ChunkSectionLayer renderType) {
        this.self().collectParts(random, dest);
    }
}
