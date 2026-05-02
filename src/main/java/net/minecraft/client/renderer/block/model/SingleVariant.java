package net.minecraft.client.renderer.block.model;

import com.mojang.serialization.Codec;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.reflect.Reflector;

public class SingleVariant implements BlockStateModel {
    private final BlockModelPart model;
    private final Collection<ChunkSectionLayer> layer;
    private final Collection<ChunkSectionLayer> layerFast;

    public SingleVariant(BlockModelPart p_394592_) {
        this.model = p_394592_;
        this.layer = this.model.layer() == null ? null : EnumSet.of(this.model.layer());
        this.layerFast = this.model.layerFast() == null ? null : EnumSet.of(this.model.layerFast());
    }

    @Override
    public void collectParts(RandomSource p_397567_, List<BlockModelPart> p_396765_) {
        p_396765_.add(this.model);
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return this.model.particleIcon();
    }

    public BlockModelPart getModel() {
        return this.model;
    }

    @Override
    public Collection<ChunkSectionLayer> getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        boolean flag = Reflector.ForgeItemBlockRenderTypes_isFancy.callBoolean();
        Collection<ChunkSectionLayer> collection = flag ? this.layer : this.layerFast;
        return collection != null ? collection : BlockStateModel.super.getRenderTypes(state, rand, data);
    }

    public record Unbaked(Variant variant) implements BlockStateModel.Unbaked {
        public static final Codec<SingleVariant.Unbaked> CODEC = Variant.CODEC.xmap(SingleVariant.Unbaked::new, SingleVariant.Unbaked::variant);

        @Override
        public BlockStateModel bake(ModelBaker p_397283_) {
            return new SingleVariant(this.variant.bake(p_397283_));
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_395676_) {
            this.variant.resolveDependencies(p_395676_);
        }
    }
}
