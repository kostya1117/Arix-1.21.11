package net.minecraft.client.resources.model;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.SimpleModelWrapper;
import net.minecraft.client.renderer.block.model.SingleVariant;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.thread.ParallelMapTransform;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import org.slf4j.Logger;

public class ModelBakery {
    public static final Material FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_0");
    public static final Material FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("fire_1");
    public static final Material LAVA_STILL = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_still");
    public static final Material LAVA_FLOW = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("lava_flow");
    public static final Material WATER_STILL = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_still");
    public static final Material WATER_FLOW = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_flow");
    public static final Material WATER_OVERLAY = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("water_overlay");
    public static final Material BANNER_BASE = new Material(Sheets.BANNER_SHEET, Identifier.withDefaultNamespace("entity/banner_base"));
    public static final Material SHIELD_BASE = new Material(Sheets.SHIELD_SHEET, Identifier.withDefaultNamespace("entity/shield_base"));
    public static final Material NO_PATTERN_SHIELD = new Material(Sheets.SHIELD_SHEET, Identifier.withDefaultNamespace("entity/shield_base_nopattern"));
    public static final int DESTROY_STAGE_COUNT = 10;
    public static final List<Identifier> DESTROY_STAGES = IntStream.range(0, 10)
        .mapToObj(indexIn -> Identifier.withDefaultNamespace("block/destroy_stage_" + indexIn))
        .collect(Collectors.toList());
    public static final List<Identifier> BREAKING_LOCATIONS = DESTROY_STAGES.stream()
        .map(locationIn -> locationIn.withPath(pathIn -> "textures/" + pathIn + ".png"))
        .collect(Collectors.toList());
    public static final List<RenderType> DESTROY_TYPES = BREAKING_LOCATIONS.stream().map(RenderTypes::crumbling).collect(Collectors.toList());
    static final Logger LOGGER = LogUtils.getLogger();
    private final EntityModelSet entityModelSet;
    private final MaterialSet materials;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final Map<BlockState, BlockStateModel.UnbakedRoot> unbakedBlockStateModels;
    private final Map<Identifier, ClientItem> clientInfos;
    final Map<Identifier, ResolvedModel> resolvedModels;
    final ResolvedModel missingModel;

    public ModelBakery(
        EntityModelSet p_376026_,
        MaterialSet p_423870_,
        PlayerSkinRenderCache p_430137_,
        Map<BlockState, BlockStateModel.UnbakedRoot> p_251087_,
        Map<Identifier, ClientItem> p_250416_,
        Map<Identifier, ResolvedModel> p_375852_,
        ResolvedModel p_393546_
    ) {
        this.entityModelSet = p_376026_;
        this.materials = p_423870_;
        this.playerSkinRenderCache = p_430137_;
        this.unbakedBlockStateModels = p_251087_;
        this.clientInfos = p_250416_;
        this.resolvedModels = p_375852_;
        this.missingModel = p_393546_;
    }

    public CompletableFuture<ModelBakery.BakingResult> bakeModels(SpriteGetter p_393789_, Executor p_392289_) {
        ModelBakery.PartCacheImpl modelbakery$partcacheimpl = new ModelBakery.PartCacheImpl();
        ModelBakery.MissingModels modelbakery$missingmodels = ModelBakery.MissingModels.bake(this.missingModel, p_393789_, modelbakery$partcacheimpl);
        ModelBakery.ModelBakerImpl modelbakery$modelbakerimpl = new ModelBakery.ModelBakerImpl(p_393789_, modelbakery$partcacheimpl, modelbakery$missingmodels);
        CompletableFuture<Map<BlockState, BlockStateModel>> completablefuture = ParallelMapTransform.schedule(this.unbakedBlockStateModels, (state2In, root2In) -> {
            try {
                return root2In.bake(state2In, modelbakery$modelbakerimpl);
            } catch (Exception exception) {
                LOGGER.warn("Unable to bake model: '{}': {}", state2In, exception);
                return null;
            }
        }, p_392289_);
        CompletableFuture<Map<Identifier, ItemModel>> completablefuture1 = ParallelMapTransform.schedule(
            this.clientInfos,
            (loc2In, item2In) -> {
                try {
                    return item2In.model()
                        .bake(
                            new ItemModel.BakingContext(
                                modelbakery$modelbakerimpl,
                                this.entityModelSet,
                                this.materials,
                                this.playerSkinRenderCache,
                                modelbakery$missingmodels.item,
                                item2In.registrySwapper()
                            )
                        );
                } catch (Exception exception) {
                    LOGGER.warn("Unable to bake item model: '{}'", loc2In, exception);
                    return null;
                }
            },
            p_392289_
        );
        Map<Identifier, ClientItem.Properties> map = new HashMap<>(this.clientInfos.size());
        this.clientInfos.forEach((loc3In, item3In) -> {
            ClientItem.Properties clientitem$properties = item3In.properties();
            if (!clientitem$properties.equals(ClientItem.Properties.DEFAULT)) {
                map.put(loc3In, clientitem$properties);
            }
        });
        return completablefuture.thenCombine(
            completablefuture1,
            (mapStateModelsIn, mapItemModelsIn) -> new ModelBakery.BakingResult(
                modelbakery$missingmodels, (Map<BlockState, BlockStateModel>)mapStateModelsIn, (Map<Identifier, ItemModel>)mapItemModelsIn, map
            )
        );
    }

    public record BakingResult(
        ModelBakery.MissingModels missingModels,
        Map<BlockState, BlockStateModel> blockStateModels,
        Map<Identifier, ItemModel> itemStackModels,
        Map<Identifier, ClientItem.Properties> itemProperties
    ) {
    }

    public record MissingModels(BlockModelPart blockPart, BlockStateModel block, ItemModel item) {
        public static ModelBakery.MissingModels bake(ResolvedModel p_395506_, final SpriteGetter p_393671_, final ModelBaker.PartCache p_456418_) {
            ModelBaker modelbaker = new ModelBaker() {
                @Override
                public ResolvedModel getModel(Identifier p_460026_) {
                    throw new IllegalStateException("Missing model can't have dependencies, but asked for " + p_460026_);
                }

                @Override
                public BlockModelPart missingBlockModelPart() {
                    throw new IllegalStateException();
                }

                @Override
                public <T> T compute(ModelBaker.SharedOperationKey<T> p_396793_) {
                    return p_396793_.compute(this);
                }

                @Override
                public SpriteGetter sprites() {
                    return p_393671_;
                }

                @Override
                public ModelBaker.PartCache parts() {
                    return p_456418_;
                }
            };
            TextureSlots textureslots = p_395506_.getTopTextureSlots();
            boolean flag = p_395506_.getTopAmbientOcclusion();
            boolean flag1 = p_395506_.getTopGuiLight().lightLikeBlock();
            ItemTransforms itemtransforms = p_395506_.getTopTransforms();
            QuadCollection quadcollection = p_395506_.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.IDENTITY);
            TextureAtlasSprite textureatlassprite = p_395506_.resolveParticleSprite(textureslots, modelbaker);
            SimpleModelWrapper simplemodelwrapper = new SimpleModelWrapper(quadcollection, flag, textureatlassprite);
            BlockStateModel blockstatemodel = new SingleVariant(simplemodelwrapper);
            ItemModel itemmodel = new MissingItemModel(quadcollection.getAll(), new ModelRenderProperties(flag1, textureatlassprite, itemtransforms));
            return new ModelBakery.MissingModels(simplemodelwrapper, blockstatemodel, itemmodel);
        }
    }

    class ModelBakerImpl implements ModelBaker {
        private final SpriteGetter sprites;
        private final ModelBaker.PartCache parts;
        private final MissingModels missingModels;
        private final Map<ModelBaker.SharedOperationKey<?>, Object> operationCache = new ConcurrentHashMap<>();
        private final Function<ModelBaker.SharedOperationKey<?>, Object> cacheComputeFunction = this::computeSharedOperation;

        ModelBakerImpl(final SpriteGetter spritesIn, final ModelBaker.PartCache cacheIn, final MissingModels missingModelsIn) {
            this.sprites = spritesIn;
            this.parts = cacheIn;
            this.missingModels = missingModelsIn;
        }

        public BlockModelPart missingBlockModelPart() {
            return this.missingModels.blockPart();
        }

        public SpriteGetter sprites() {
            return this.sprites;
        }

        public ModelBaker.PartCache parts() {
            return this.parts;
        }

        public ResolvedModel getModel(Identifier locIn) {
            ResolvedModel resolvedmodel = ModelBakery.this.resolvedModels.get(locIn);
            if (resolvedmodel == null) {
                ModelBakery.LOGGER.warn("Requested a model that was not discovered previously: {}", locIn);
                return ModelBakery.this.missingModel;
            } else {
                return resolvedmodel;
            }
        }

        private Object computeSharedOperation(ModelBaker.SharedOperationKey<?> keyIn) {
            return keyIn.compute(this);
        }

        @SuppressWarnings("unchecked")
        public <T> T compute(ModelBaker.SharedOperationKey<T> keyIn) {
            return (T) this.operationCache.computeIfAbsent(keyIn, this.cacheComputeFunction);
        }
    }

    public static class PartCacheImpl implements ModelBaker.PartCache {
        private final Interner<Vector3fc> vectors = Interners.newStrongInterner();

        @Override
        public Vector3fc vector(Vector3fc p_460813_) {
            return this.vectors.intern(p_460813_);
        }
    }
}
