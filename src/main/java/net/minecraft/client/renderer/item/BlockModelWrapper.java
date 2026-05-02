package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomItems;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public class BlockModelWrapper implements ItemModel {
    private static final Function<ItemStack, RenderType> ITEM_RENDER_TYPE_GETTER = stack2In -> Sheets.translucentItemSheet();
    private static final Function<ItemStack, RenderType> BLOCK_RENDER_TYPE_GETTER = stack2In -> {
        if (stack2In.getItem() instanceof BlockItem blockitem) {
            ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getChunkRenderType(blockitem.getBlock().defaultBlockState());
            if (chunksectionlayer != ChunkSectionLayer.TRANSLUCENT) {
                return Sheets.cutoutBlockSheet();
            }
        }

        return Sheets.translucentBlockItemSheet();
    };
    private final List<ItemTintSource> tints;
    private final List<BakedQuad> quads;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final boolean animated;
    private final Function<ItemStack, RenderType> renderType;
    private Identifier modelLocation;
    private boolean builtinGenerated;

    BlockModelWrapper(List<ItemTintSource> p_377381_, List<BakedQuad> p_396453_, ModelRenderProperties p_395664_, Function<ItemStack, RenderType> p_460789_) {
        this(p_377381_, p_396453_, p_395664_, p_460789_, null, false);
    }

    public BlockModelWrapper(
        List<ItemTintSource> tintSourcesIn,
        List<BakedQuad> bakedQuadsIn,
        ModelRenderProperties propsIn,
        Function<ItemStack, RenderType> renderTypeIn,
        Identifier modelLocation,
        boolean builtinGenerated
    ) {
        this.tints = tintSourcesIn;
        this.quads = bakedQuadsIn;
        this.properties = propsIn;
        this.renderType = renderTypeIn;
        this.extents = Suppliers.memoize(() -> computeExtents(this.quads));
        boolean flag = false;

        for (BakedQuad bakedquad : bakedQuadsIn) {
            if (bakedquad.sprite().contents().isAnimated()) {
                flag = true;
                break;
            }
        }

        this.animated = flag;
        this.modelLocation = modelLocation;
        this.builtinGenerated = builtinGenerated;
    }

    public static Vector3fc[] computeExtents(List<BakedQuad> p_397460_) {
        Set<Vector3fc> set = new HashSet<>();

        for (BakedQuad bakedquad : p_397460_) {
            for (int i = 0; i < 4; i++) {
                set.add(bakedquad.position(i));
            }
        }

        return set.toArray(Vector3fc[]::new);
    }

    @Override
    public void update(
        ItemStackRenderState p_377049_,
        ItemStack p_378482_,
        ItemModelResolver p_377214_,
        ItemDisplayContext p_375691_,
         ClientLevel p_376532_,
         ItemOwner p_425592_,
        int p_377340_
    ) {
        this.update(p_377049_, p_378482_, p_377214_, p_375691_, p_376532_, p_425592_, p_377340_, false, null);
    }

    public void update(
        ItemStackRenderState stateIn,
        ItemStack stackIn,
        ItemModelResolver resolverIn,
        ItemDisplayContext contextIn,
         ClientLevel worldIn,
         ItemOwner entityIn,
        int seedIn,
        boolean customIn,
        ModelRenderProperties propertiesIn
    ) {
        if (Config.isCustomItems() && !customIn) {
            BlockModelWrapper blockmodelwrapper = CustomItems.getCustomItemModel(stackIn, this, this.modelLocation, true) instanceof BlockModelWrapper blockmodelwrapper1
                ? blockmodelwrapper1
                : this;
            ItemModel itemmodel = CustomItems.getCustomItemModel(stackIn, blockmodelwrapper, this.modelLocation, false);
            if (itemmodel != this && itemmodel instanceof BlockModelWrapper blockmodelwrapper2) {
                blockmodelwrapper2.update(stateIn, stackIn, resolverIn, contextIn, worldIn, entityIn, seedIn, true, blockmodelwrapper.properties);
                return;
            }
        }

        stateIn.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = stateIn.newLayer();
        itemstackrenderstate$layerrenderstate.setItemStack(stackIn);
        if (stackIn.hasFoil()) {
            ItemStackRenderState.FoilType itemstackrenderstate$foiltype = hasSpecialAnimatedTexture(stackIn)
                ? ItemStackRenderState.FoilType.SPECIAL
                : ItemStackRenderState.FoilType.STANDARD;
            itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
            stateIn.setAnimated();
            stateIn.appendModelIdentityElement(itemstackrenderstate$foiltype);
        }

        int k = this.tints.size();
        int[] aint = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

        for (int i = 0; i < k; i++) {
            int j = this.tints.get(i).calculate(stackIn, worldIn, entityIn == null ? null : entityIn.asLivingEntity());
            if (Config.isCustomColors()) {
                j = CustomColors.getFullColorFromItemStack(stackIn, i, j);
            }

            aint[i] = j;
            stateIn.appendModelIdentityElement(j);
        }

        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.setRenderType(this.renderType.apply(stackIn));
        if (propertiesIn != null) {
            propertiesIn.applyToLayer(itemstackrenderstate$layerrenderstate, contextIn);
        } else {
            this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, contextIn);
        }

        itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
        if (this.animated) {
            stateIn.setAnimated();
        }
    }

    public static Function<ItemStack, RenderType> detectRenderType(List<BakedQuad> p_457890_) {
        Iterator<BakedQuad> iterator = p_457890_.iterator();
        if (!iterator.hasNext()) {
            return ITEM_RENDER_TYPE_GETTER;
        }

        Identifier identifier = iterator.next().sprite().atlasLocation();

        while (iterator.hasNext()) {
            BakedQuad bakedquad = iterator.next();
            Identifier identifier1 = bakedquad.sprite().atlasLocation();
            if (!identifier1.equals(identifier)) {
                Identifier identifier2 = p_457890_.get(0).sprite().getName();
                Identifier identifier3 = bakedquad.sprite().getName();
                throw new IllegalStateException(
                    "Multiple atlases used in model, expected " + identifier + " (" + identifier2 + "), but also got " + identifier1 + " (" + identifier3 + ")"
                );
            }
        }

        if (identifier.equals(TextureAtlas.LOCATION_ITEMS)) {
            return ITEM_RENDER_TYPE_GETTER;
        } else if (identifier.equals(TextureAtlas.LOCATION_BLOCKS)) {
            return BLOCK_RENDER_TYPE_GETTER;
        } else {
            throw new IllegalArgumentException("Atlas " + identifier + " can't be usef for item models");
        }
    }

    private static boolean hasSpecialAnimatedTexture(ItemStack p_377482_) {
        return p_377482_.is(ItemTags.COMPASSES) || p_377482_.is(Items.CLOCK);
    }

    public boolean isBuiltinGenerated() {
        return this.builtinGenerated;
    }

    @Override
    public String toString() {
        return this.modelLocation + "";
    }

    public record Unbaked(Identifier model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<BlockModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            model2In -> model2In.group(
                    Identifier.CODEC.fieldOf("model").forGetter(BlockModelWrapper.Unbaked::model),
                    ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BlockModelWrapper.Unbaked::tints)
                )
                .apply(model2In, BlockModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_375708_) {
            p_375708_.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_375857_) {
            ModelBaker modelbaker = p_375857_.blockModelBaker();
            ResolvedModel resolvedmodel = modelbaker.getModel(this.model);
            TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
            QuadCollection quadcollection = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.IDENTITY);
            List<BakedQuad> list = quadcollection.getAll();
            ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
            Function<ItemStack, RenderType> function = BlockModelWrapper.detectRenderType(list);
            return new BlockModelWrapper(this.tints, list, modelrenderproperties, function, this.model, quadcollection.isBuiltinGenerated());
        }

        @Override
        public MapCodec<BlockModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
