package net.minecraft.client.renderer.block.model;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.WeightedVariants;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBlockStateModel;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.reflect.Reflector;

public interface BlockStateModel extends IForgeBlockStateModel {
    void collectParts(RandomSource p_397689_, List<BlockModelPart> p_394710_);

    default List<BlockModelPart> collectParts(RandomSource p_392713_) {
        List<BlockModelPart> list = new ObjectArrayList<>();
        if (Reflector.ForgeHooksClient.exists()) {
            this.collectParts(p_392713_, list, ModelData.EMPTY, null);
        } else {
            this.collectParts(p_392713_, list);
        }

        return list;
    }

    TextureAtlasSprite particleIcon();

    class SimpleCachedUnbakedRoot implements BlockStateModel.UnbakedRoot {
        final BlockStateModel.Unbaked contents;
        private final ModelBaker.SharedOperationKey<BlockStateModel> bakingKey = new ModelBaker.SharedOperationKey<BlockStateModel>() {
            public BlockStateModel compute(ModelBaker p_396245_) {
                return SimpleCachedUnbakedRoot.this.contents.bake(p_396245_);
            }
        };

        public SimpleCachedUnbakedRoot(BlockStateModel.Unbaked p_394126_) {
            this.contents = p_394126_;
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_396058_) {
            this.contents.resolveDependencies(p_396058_);
        }

        @Override
        public BlockStateModel bake(BlockState p_394850_, ModelBaker p_396441_) {
            return p_396441_.compute(this.bakingKey);
        }

        @Override
        public Object visualEqualityGroup(BlockState p_395333_) {
            return this;
        }
    }

    interface Unbaked extends ResolvableModel {
        Codec<Weighted<Variant>> ELEMENT_CODEC = RecordCodecBuilder.create(
            instIn -> instIn.group(
                    Variant.MAP_CODEC.forGetter(Weighted::value), ExtraCodecs.POSITIVE_INT.optionalFieldOf("weight", 1).forGetter(Weighted::weight)
                )
                .apply(instIn, Weighted::new)
        );
        Codec<WeightedVariants.Unbaked> HARDCODED_WEIGHTED_CODEC = ExtraCodecs.nonEmptyList(ELEMENT_CODEC.listOf())
            .flatComapMap(
                listIn -> new WeightedVariants.Unbaked(
                    WeightedList.of(Lists.transform((List<Weighted<Variant>>)listIn, weightedIn -> weightedIn.map(SingleVariant.Unbaked::new)))
                ),
                unbakedIn -> {
                    List<Weighted<BlockStateModel.Unbaked>> list = unbakedIn.entries().unwrap();
                    List<Weighted<Variant>> list1 = new ArrayList<>(list.size());

                    for (Weighted<BlockStateModel.Unbaked> weighted : list) {
                        if (!(weighted.value() instanceof SingleVariant.Unbaked singlevariant$unbaked)) {
                            return DataResult.error(() -> "Only single variants are supported");
                        }

                        list1.add(new Weighted<>(singlevariant$unbaked.variant(), weighted.weight()));
                    }

                    return DataResult.success(list1);
                }
            );
        Codec<BlockStateModel.Unbaked> CODEC = Codec.either(HARDCODED_WEIGHTED_CODEC, SingleVariant.Unbaked.CODEC)
            .flatComapMap(eitherIn -> eitherIn.map(unbakedIn -> unbakedIn, unbaked2In -> unbaked2In), unbaked3In -> {
                return switch (unbaked3In) {
                    case SingleVariant.Unbaked singlevariant$unbaked -> DataResult.success(Either.right(singlevariant$unbaked));
                    case WeightedVariants.Unbaked weightedvariants$unbaked -> DataResult.success(Either.left(weightedvariants$unbaked));
                    default -> DataResult.error(() -> "Only a single variant or a list of variants are supported");
                };
            });

        BlockStateModel bake(ModelBaker p_391198_);

        default BlockStateModel.UnbakedRoot asRoot() {
            return new BlockStateModel.SimpleCachedUnbakedRoot(this);
        }
    }

    interface UnbakedRoot extends ResolvableModel {
        BlockStateModel bake(BlockState p_392403_, ModelBaker p_396586_);

        Object visualEqualityGroup(BlockState p_391557_);
    }
}
