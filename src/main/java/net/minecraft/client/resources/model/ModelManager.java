package net.minecraft.client.resources.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SpecialBlockModelRenderer;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.optifine.Config;
import net.optifine.CustomItems;
import net.optifine.reflect.Reflector;
import net.optifine.util.PathUtils;
import net.optifine.util.StrUtils;
import net.optifine.util.TextureUtils;
import org.slf4j.Logger;

public class ModelManager implements PreparableReloadListener {
    public static final Identifier BLOCK_OR_ITEM = Identifier.withDefaultNamespace("block_or_item");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter MODEL_LISTER = FileToIdConverter.json("models");
    private Map<Identifier, ItemModel> bakedItemStackModels = Map.of();
    private Map<Identifier, ClientItem.Properties> itemProperties = Map.of();
    private final AtlasManager atlasManager;
    private final PlayerSkinRenderCache playerSkinRenderCache;
    private final BlockModelShaper blockModelShaper;
    private final BlockColors blockColors;
    private EntityModelSet entityModelSet = EntityModelSet.EMPTY;
    private SpecialBlockModelRenderer specialBlockModelRenderer = SpecialBlockModelRenderer.EMPTY;
    private ModelBakery.MissingModels missingModels;
    private Object2IntMap<BlockState> modelGroups = Object2IntMaps.emptyMap();
    private Map<Identifier, ItemModel> bakedItemStackModelsView = Map.of();
    private ModelBakery modelBakery;
    private static ThreadLocal<Identifier> parsedModelLocation = new ThreadLocal<>();

    public ModelManager(BlockColors p_119407_, AtlasManager p_423190_, PlayerSkinRenderCache p_429874_) {
        this.blockColors = p_119407_;
        this.atlasManager = p_423190_;
        this.playerSkinRenderCache = p_429874_;
        this.blockModelShaper = new BlockModelShaper(this);
    }

    public BlockStateModel getMissingBlockStateModel() {
        return this.missingModels.block();
    }

    public ItemModel getItemModel(Identifier p_450917_) {
        return this.bakedItemStackModels.getOrDefault(p_450917_, this.missingModels.item());
    }

    public ClientItem.Properties getItemProperties(Identifier p_453916_) {
        return this.itemProperties.getOrDefault(p_453916_, ClientItem.Properties.DEFAULT);
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.SharedState p_427489_, Executor p_250550_, PreparableReloadListener.PreparationBarrier p_249079_, Executor p_249221_
    ) {
        Reflector.GeometryLoaderManager_init.call();
        ResourceManager resourcemanager = p_427489_.resourceManager();
        CompletableFuture<EntityModelSet> completablefuture = CompletableFuture.supplyAsync(EntityModelSet::vanilla, p_250550_);
        CompletableFuture<SpecialBlockModelRenderer> completablefuture1 = completablefuture.thenApplyAsync(
            models2In -> SpecialBlockModelRenderer.vanilla(new SpecialModelRenderer.BakingContext.Simple(models2In, this.atlasManager, this.playerSkinRenderCache)),
            p_250550_
        );
        CompletableFuture<Map<Identifier, UnbakedModel>> completablefuture2 = loadBlockModels(resourcemanager, p_250550_);
        CompletableFuture<BlockStateModelLoader.LoadedModels> completablefuture3 = BlockStateModelLoader.loadBlockStates(resourcemanager, p_250550_);
        CompletableFuture<ClientItemInfoLoader.LoadedClientInfos> completablefuture4 = ClientItemInfoLoader.scheduleLoad(resourcemanager, p_250550_);
        CompletableFuture<ModelManager.ResolvedModels> completablefuture5 = CompletableFuture.allOf(completablefuture2, completablefuture3, completablefuture4)
            .thenApplyAsync(voidIn -> discoverModelDependencies(completablefuture2.join(), completablefuture3.join(), completablefuture4.join()), p_250550_);
        CompletableFuture<ModelManager.ResolvedModels> completablefuture6 = completablefuture5.whenComplete(
            (resIn, excIn) -> TextureUtils.setModelLoadingException(excIn)
        );
        CompletableFuture<Object2IntMap<BlockState>> completablefuture7 = completablefuture3.thenApplyAsync(
            modelsIn -> buildModelGroups(this.blockColors, modelsIn), p_250550_
        );
        AtlasManager.PendingStitchResults atlasmanager$pendingstitchresults = p_427489_.get(AtlasManager.PENDING_STITCH);
        CompletableFuture<SpriteLoader.Preparations> completablefuture8 = atlasmanager$pendingstitchresults.get(AtlasIds.BLOCKS);
        CompletableFuture<SpriteLoader.Preparations> completablefuture9 = atlasmanager$pendingstitchresults.get(AtlasIds.ITEMS);
        return CompletableFuture.allOf(
                completablefuture8,
                completablefuture9,
                completablefuture6,
                completablefuture7,
                completablefuture3,
                completablefuture4,
                completablefuture,
                completablefuture1,
                completablefuture2
            )
            .thenComposeAsync(
                voidIn -> {
                    SpriteLoader.Preparations spriteloader$preparations = completablefuture8.join();
                    SpriteLoader.Preparations spriteloader$preparations1 = completablefuture9.join();
                    ModelManager.ResolvedModels modelmanager$resolvedmodels = completablefuture6.join();
                    Object2IntMap<BlockState> object2intmap = completablefuture7.join();
                    Set<Identifier> set = Sets.difference(completablefuture2.join().keySet(), modelmanager$resolvedmodels.models.keySet());
                    if (!set.isEmpty()) {
                        LOGGER.debug("Unreferenced models: \n{}", set.stream().sorted().map(locIn -> "\t" + locIn + "\n").collect(Collectors.joining()));
                    }

                    ModelBakery modelbakery = new ModelBakery(
                        completablefuture.join(),
                        this.atlasManager,
                        this.playerSkinRenderCache,
                        completablefuture3.join().models(),
                        completablefuture4.join().contents(),
                        modelmanager$resolvedmodels.models(),
                        modelmanager$resolvedmodels.missing()
                    );
                    return loadModels(
                        spriteloader$preparations,
                        spriteloader$preparations1,
                        modelbakery,
                        object2intmap,
                        completablefuture.join(),
                        completablefuture1.join(),
                        p_250550_
                    );
                },
                p_250550_
            )
            .thenCompose(p_249079_::wait)
            .thenAcceptAsync(this::apply, p_249221_);
    }

    private static CompletableFuture<Map<Identifier, UnbakedModel>> loadBlockModels(ResourceManager p_251361_, Executor p_252189_) {
        return CompletableFuture.<Map<Identifier, Resource>>supplyAsync(() -> MODEL_LISTER.listMatchingResources(p_251361_), p_252189_)
            .thenCompose(
                resourcesIn -> {
                    List<CompletableFuture<Pair<Identifier, BlockModel>>> list = new ArrayList<>(resourcesIn.size());
                    TextureUtils.registerCustomModels((Map<Identifier, Resource>)resourcesIn);

                    for (Entry<Identifier, Resource> entry : resourcesIn.entrySet()) {
                        list.add(CompletableFuture.supplyAsync(() -> {
                            Identifier identifier = MODEL_LISTER.fileToId(entry.getKey());

                            try (Reader reader = entry.getValue().openAsReader()) {
                                parsedModelLocation.set(identifier);
                                Pair pair = fixBlockModel(identifier, BlockModel.fromStream(reader));
                                parsedModelLocation.set(null);
                                return pair;
                            } catch (Exception exception) {
                                LOGGER.error("Failed to load model {}", entry.getKey(), exception);
                                return null;
                            }
                        }, p_252189_));
                    }

                    return Util.sequence(list)
                        .thenApply(
                            modelsIn -> modelsIn.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond))
                        );
                }
            );
    }

    private static ModelManager.ResolvedModels discoverModelDependencies(
        Map<Identifier, UnbakedModel> p_360749_, BlockStateModelLoader.LoadedModels p_366446_, ClientItemInfoLoader.LoadedClientInfos p_378505_
    ) {
        ModelManager.ResolvedModels modelmanager$resolvedmodels;
        try (Zone zone = Profiler.get().zone("dependencies")) {
            ModelDiscovery modeldiscovery = new ModelDiscovery(p_360749_, MissingBlockModel.missingModel());
            modeldiscovery.addSpecialModel(ItemModelGenerator.GENERATED_ITEM_MODEL_ID, new ItemModelGenerator());
            p_366446_.models().values().forEach(modeldiscovery::addRoot);
            p_378505_.contents().values().forEach(itemIn -> modeldiscovery.addRoot(itemIn.model()));
            modelmanager$resolvedmodels = new ModelManager.ResolvedModels(modeldiscovery.missingModel(), modeldiscovery.resolve());
            if (Config.isCustomItems()) {
                CustomItems.collectModelSprites(modelmanager$resolvedmodels.models);
            }
        }

        return modelmanager$resolvedmodels;
    }

    private static CompletableFuture<ModelManager.ReloadState> loadModels(
        final SpriteLoader.Preparations p_422832_,
        final SpriteLoader.Preparations p_460168_,
        ModelBakery p_248945_,
        Object2IntMap<BlockState> p_361513_,
        EntityModelSet p_378097_,
        SpecialBlockModelRenderer p_377275_,
        Executor p_394729_
    ) {
        final Multimap<String, Material> multimap = Multimaps.synchronizedMultimap(HashMultimap.create());
        final Multimap<String, String> multimap1 = Multimaps.synchronizedMultimap(HashMultimap.create());
        return p_248945_.bakeModels(new SpriteGetter() {
                private final TextureAtlasSprite blockMissing = p_422832_.missing();
                private final TextureAtlasSprite itemMissing = p_460168_.missing();

                @Override
                public TextureAtlasSprite get(Material p_375858_, ModelDebugName p_375833_) {
                    Identifier identifier = p_375858_.atlasLocation();
                    boolean flag = identifier.equals(ModelManager.BLOCK_OR_ITEM);
                    boolean flag1 = identifier.equals(TextureAtlas.LOCATION_ITEMS);
                    boolean flag2 = identifier.equals(TextureAtlas.LOCATION_BLOCKS);
                    if (flag || flag1) {
                        TextureAtlasSprite textureatlassprite = p_460168_.getSprite(p_375858_.texture());
                        if (textureatlassprite != null) {
                            return textureatlassprite;
                        }
                    }

                    if (flag || flag2) {
                        TextureAtlasSprite textureatlassprite1 = p_422832_.getSprite(p_375858_.texture());
                        if (textureatlassprite1 != null) {
                            return textureatlassprite1;
                        }
                    }

                    multimap.put(p_375833_.debugName(), p_375858_);
                    return flag1 ? this.itemMissing : this.blockMissing;
                }

                @Override
                public TextureAtlasSprite reportMissingReference(String p_378821_, ModelDebugName p_377684_) {
                    multimap1.put(p_377684_.debugName(), p_378821_);
                    return this.blockMissing;
                }
            }, p_394729_)
            .thenApply(
                resultIn -> {
                    Reflector.ForgeHooksClient_onModifyBakingResult.call(p_248945_, resultIn);
                    multimap.asMap()
                        .forEach(
                            (locIn, materialsIn) -> LOGGER.warn(
                                "Missing textures in model {}: {}",
                                locIn,
                                materialsIn.stream()
                                    .sorted(Material.COMPARATOR)
                                    .map(materialIn -> materialIn.atlasLocation() + ":" + materialIn.texture())
                                    .collect(Collectors.joining(", "))
                            )
                        );
                    multimap1.asMap()
                        .forEach(
                            (nameIn, refsIn) -> LOGGER.warn(
                                "Missing texture references in model {}: {}",
                                nameIn,
                                refsIn.stream().sorted().map(strIn -> strIn + "").collect(Collectors.joining(", "))
                            )
                        );
                    Map<BlockState, BlockStateModel> map = createBlockStateToModelDispatch(resultIn.blockStateModels(), resultIn.missingModels().block());
                    return new ModelManager.ReloadState(resultIn, p_361513_, map, p_378097_, p_377275_, p_248945_);
                }
            );
    }

    private static Map<BlockState, BlockStateModel> createBlockStateToModelDispatch(Map<BlockState, BlockStateModel> p_377857_, BlockStateModel p_396223_) {
        Object object;
        try (Zone zone = Profiler.get().zone("block state dispatch")) {
            Map<BlockState, BlockStateModel> map = new IdentityHashMap<>(p_377857_);

            for (Block block : BuiltInRegistries.BLOCK) {
                block.getStateDefinition().getPossibleStates().forEach(state2In -> {
                    if (p_377857_.putIfAbsent(state2In, p_396223_) == null) {
                        LOGGER.warn("Missing model for variant: '{}'", state2In);
                    }
                });
            }

            object = map;
        }

        return (Map<BlockState, BlockStateModel>)object;
    }

    private static Object2IntMap<BlockState> buildModelGroups(BlockColors p_369941_, BlockStateModelLoader.LoadedModels p_360724_) {
        try (Zone zone = Profiler.get().zone("block groups")) {
            return ModelGroupCollector.build(p_369941_, p_360724_);
        }
    }

    private void apply(ModelManager.ReloadState p_248996_) {
        ModelBakery.BakingResult modelbakery$bakingresult = p_248996_.bakedModels;
        this.bakedItemStackModels = modelbakery$bakingresult.itemStackModels();
        this.itemProperties = modelbakery$bakingresult.itemProperties();
        this.modelGroups = p_248996_.modelGroups;
        this.missingModels = modelbakery$bakingresult.missingModels();
        this.bakedItemStackModelsView = Collections.unmodifiableMap(this.bakedItemStackModels);
        this.modelBakery = p_248996_.modelBakery();
        Reflector.ForgeHooksClient_onModelBake.call(this, this.modelBakery);
        this.blockModelShaper.replaceCache(p_248996_.modelCache);
        this.specialBlockModelRenderer = p_248996_.specialBlockModelRenderer;
        this.entityModelSet = p_248996_.entityModelSet;
    }

    public boolean requiresRender(BlockState p_119416_, BlockState p_119417_) {
        if (p_119416_ == p_119417_) {
            return false;
        }

        int i = this.modelGroups.getInt(p_119416_);
        if (i != -1) {
            int j = this.modelGroups.getInt(p_119417_);
            if (i == j) {
                FluidState fluidstate = p_119416_.getFluidState();
                FluidState fluidstate1 = p_119417_.getFluidState();
                return fluidstate != fluidstate1;
            }
        }

        return true;
    }

    public ItemModel getMissingItemModel() {
        return this.missingModels.item();
    }

    public ModelBakery.MissingModels getMissingModels() {
        return this.missingModels;
    }

    public AtlasManager getAtlasManager() {
        return this.atlasManager;
    }

    private static Pair<Identifier, BlockModel> fixBlockModel(Identifier locationModelIn, BlockModel modelIn) {
        Identifier identifier = resolveRelative(locationModelIn, modelIn.parent());
        if (identifier != modelIn.parent()) {
            modelIn = new BlockModel(modelIn.geometry(), modelIn.guiLight(), modelIn.ambientOcclusion(), modelIn.transforms(), modelIn.textureSlots(), identifier);
        }

        return new Pair<>(locationModelIn, modelIn);
    }

    public static Identifier resolveRelative(Identifier locationModelIn, Identifier locationIn) {
        if (locationModelIn != null && locationIn != null) {
            if (locationIn.getPath().startsWith("./")) {
                String s = PathUtils.removeLast(locationModelIn.getPath());
                String s1 = StrUtils.replacePrefix(locationIn.getPath(), "./", s + "/");
                locationIn = locationIn.withPath(s1);
            }

            return locationIn;
        } else {
            return locationIn;
        }
    }

    public static Material fixMaterial(Material materialIn) {
        Identifier identifier = parsedModelLocation.get();
        if (identifier != null && identifier.isDefaultNamespace() && identifier.getPath().startsWith("optifine/cit/")) {
            Identifier identifier1 = TextureAtlas.LOCATION_ITEMS;
            Identifier identifier2 = resolveRelative(identifier, materialIn.texture());
            if (identifier2.getPath().startsWith("block/")) {
                identifier2 = identifier2.withPrefix("_virtual_/");
            }

            return new Material(identifier1, identifier2);
        } else {
            return materialIn;
        }
    }

    public Map<Identifier, ItemModel> getItemModels() {
        return this.bakedItemStackModelsView;
    }

    public ModelBakery getModelBakery() {
        return Preconditions.checkNotNull(this.modelBakery, "Attempted to query model bakery before it has been initialized.");
    }

    public SpecialBlockModelRenderer specialBlockModelRenderer() {
        return this.specialBlockModelRenderer;
    }

    public Supplier<EntityModelSet> entityModels() {
        return () -> this.entityModelSet;
    }

    record ReloadState(
        ModelBakery.BakingResult bakedModels,
        Object2IntMap<BlockState> modelGroups,
        Map<BlockState, BlockStateModel> modelCache,
        EntityModelSet entityModelSet,
        SpecialBlockModelRenderer specialBlockModelRenderer,
        ModelBakery modelBakery
    ) {
        public ReloadState(
            ModelBakery.BakingResult bakedModels,
            Object2IntMap<BlockState> modelGroups,
            Map<BlockState, BlockStateModel> modelCache,
            EntityModelSet entityModelSet,
            SpecialBlockModelRenderer specialBlockModelRenderer
        ) {
            this(bakedModels, modelGroups, modelCache, entityModelSet, specialBlockModelRenderer, null);
        }
    }

    record ResolvedModels(ResolvedModel missing, Map<Identifier, ResolvedModel> models) {
    }
}
