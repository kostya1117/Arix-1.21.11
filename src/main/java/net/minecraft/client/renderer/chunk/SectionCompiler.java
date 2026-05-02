package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomBlockLayers;
import net.optifine.override.ChunkCacheOF;
import net.optifine.reflect.Reflector;
import net.optifine.render.RegionLayerRenderer;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.Shaders;
import net.optifine.util.DummyRandomSource;
import net.optifine.util.SingleIterable;
import org.jspecify.annotations.Nullable;

public class SectionCompiler {
    private final BlockRenderDispatcher blockRenderer;
    private final BlockEntityRenderDispatcher blockEntityRenderer;
    protected SectionRenderDispatcher sectionRenderDispatcher;
    public static final boolean FORGE = Reflector.ForgeHooksClient.exists();

    public SectionCompiler(BlockRenderDispatcher p_344503_, BlockEntityRenderDispatcher p_345164_) {
        this.blockRenderer = p_344503_;
        this.blockEntityRenderer = p_345164_;
    }

    public SectionCompiler.Results compile(SectionPos p_344383_, RenderSectionRegion p_409909_, VertexSorting p_342522_, SectionBufferBuilderPack p_343546_) {
        ChunkCacheOF chunkcacheof = p_409909_.makeChunkCacheOF();
        return this.compile(p_344383_, chunkcacheof, p_342522_, p_343546_, 0, 0, 0);
    }

    public SectionCompiler.Results compile(
        SectionPos sectionPosIn, ChunkCacheOF regionIn, VertexSorting sortingIn, SectionBufferBuilderPack builderIn, int regionDX, int regionDY, int regionDZ
    ) {
        Map<BlockPos, ModelData> map = FORGE ? Minecraft.getInstance().level.getModelDataManager().getAt(sectionPosIn) : null;
        SectionCompiler.Results sectioncompiler$results = new SectionCompiler.Results();
        BlockPos blockpos = sectionPosIn.origin();
        BlockPos blockpos1 = blockpos.offset(15, 15, 15);
        VisGraph visgraph = new VisGraph();
        PoseStack posestack = new PoseStack();
        SectionRenderDispatcher.renderChunksUpdated++;
        regionIn.renderStart();
        SingleIterable<ChunkSectionLayer> singleiterable = new SingleIterable<>();
        boolean flag = Config.isMipmaps();
        boolean flag1 = Config.isShaders();
        boolean flag2 = flag1 && Shaders.useMidBlockAttrib;
        ModelBlockRenderer.enableCaching();
        Map<ChunkSectionLayer, BufferBuilder> map1 = new EnumMap<>(ChunkSectionLayer.class);
        RandomSource randomsource = !Config.isAlternateBlocks() ? DummyRandomSource.INSTANCE : RandomSource.create();
        List<BlockModelPart> list = new ObjectArrayList<>();
        BlockRenderDispatcher blockrenderdispatcher = Minecraft.getInstance().getBlockRenderer();

        for (BlockPosM blockposm : BlockPosM.getAllInBoxMutableM(blockpos, blockpos1)) {
            BlockState blockstate = regionIn.getBlockState(blockposm);
            if (!blockstate.isAir()) {
                if (blockstate.isSolidRender()) {
                    visgraph.setOpaque(blockposm);
                }

                if (blockstate.hasBlockEntity()) {
                    BlockEntity blockentity = regionIn.getBlockEntity(blockposm);
                    if (blockentity != null) {
                        this.handleBlockEntity(sectioncompiler$results, blockentity);
                    }
                }

                FluidState fluidstate = blockstate.getFluidState();
                if (!fluidstate.isEmpty()) {
                    ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getRenderLayer(fluidstate);
                    BufferBuilder bufferbuilder = this.getOrBeginLayer(map1, builderIn, chunksectionlayer);
                    RenderEnv renderenv = bufferbuilder.getRenderEnv(blockstate, blockposm);
                    renderenv.setCompileParams(this, map1, builderIn);
                    regionIn.setRenderEnv(renderenv);
                    this.blockRenderer.renderLiquid(blockposm, regionIn, bufferbuilder, blockstate, fluidstate);
                }

                if (blockstate.getRenderShape() == RenderShape.MODEL) {
                    BlockStateModel blockstatemodel = blockrenderdispatcher.getBlockModel(blockstate);
                    ModelData modeldata = FORGE
                        ? blockstatemodel.getModelData(regionIn, blockposm, blockstate, map.getOrDefault(blockposm, ModelData.EMPTY))
                        : null;

                    for (ChunkSectionLayer chunksectionlayer1 : getBlockRenderLayers(
                        blockstatemodel, blockstate, blockposm, randomsource, modeldata, singleiterable
                    )) {
                        ChunkSectionLayer chunksectionlayer2 = this.fixBlockLayer(regionIn, blockstate, blockposm, chunksectionlayer1, flag);
                        BufferBuilder bufferbuilder1 = this.getOrBeginLayer(map1, builderIn, chunksectionlayer2);
                        randomsource.setSeed(blockstate.getSeed(blockposm));
                        if (FORGE) {
                            blockstatemodel.collectParts(randomsource, list, modeldata, chunksectionlayer1);
                        } else {
                            this.blockRenderer.getBlockModel(blockstate).collectParts(randomsource, list);
                        }

                        RenderEnv renderenv1 = bufferbuilder1.getRenderEnv(blockstate, blockposm);
                        renderenv1.setCompileParams(this, map1, builderIn);
                        regionIn.setRenderEnv(renderenv1);
                        posestack.pushPose();
                        if (RegionLayerRenderer.canRenderLayer(chunksectionlayer2)) {
                            posestack.translate(
                                regionDX + SectionPos.sectionRelative(blockposm.getX()),
                                regionDY + SectionPos.sectionRelative(blockposm.getY()),
                                regionDZ + SectionPos.sectionRelative(blockposm.getZ())
                            );
                        } else {
                            posestack.translate(
                                SectionPos.sectionRelative(blockposm.getX()),
                                SectionPos.sectionRelative(blockposm.getY()),
                                SectionPos.sectionRelative(blockposm.getZ())
                            );
                        }

                        if (flag2) {
                            bufferbuilder1.setMidBlock(
                                0.5F + regionDX + SectionPos.sectionRelative(blockposm.getX()),
                                0.5F + regionDY + SectionPos.sectionRelative(blockposm.getY()),
                                0.5F + regionDZ + SectionPos.sectionRelative(blockposm.getZ())
                            );
                        }

                        this.blockRenderer.renderBatched(blockstate, blockposm, regionIn, posestack, bufferbuilder1, true, list);
                        posestack.popPose();
                        list.clear();
                    }
                }
            }
        }

        for (ChunkSectionLayer chunksectionlayer4 : SectionRenderDispatcher.BLOCK_RENDER_LAYERS) {
            sectioncompiler$results.setAnimatedSprites(chunksectionlayer4, null);
        }

        for (Entry<ChunkSectionLayer, BufferBuilder> entry : map1.entrySet()) {
            ChunkSectionLayer chunksectionlayer3 = entry.getKey();
            BufferBuilder bufferbuilder2 = entry.getValue();
            if (bufferbuilder2.animatedSprites != null && !bufferbuilder2.animatedSprites.isEmpty()) {
                sectioncompiler$results.setAnimatedSprites(chunksectionlayer3, (BitSet)bufferbuilder2.animatedSprites.clone());
            }

            MeshData meshdata = bufferbuilder2.build();
            if (meshdata != null) {
                if (chunksectionlayer3 == ChunkSectionLayer.TRANSLUCENT) {
                    sectioncompiler$results.transparencyState = meshdata.sortQuads(builderIn.buffer(chunksectionlayer3), sortingIn);
                }

                sectioncompiler$results.renderedLayers.put(chunksectionlayer3, meshdata);
            }
        }

        regionIn.renderFinish();
        ModelBlockRenderer.clearCache();
        sectioncompiler$results.visibilitySet = visgraph.resolve();
        return sectioncompiler$results;
    }

    public BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> p_344204_, SectionBufferBuilderPack p_344936_, ChunkSectionLayer p_408915_) {
        BufferBuilder bufferbuilder = p_344204_.get(p_408915_);
        if (bufferbuilder == null) {
            ByteBufferBuilder bytebufferbuilder = p_344936_.buffer(p_408915_);
            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK, null, p_408915_);
            p_344204_.put(p_408915_, bufferbuilder);
        }

        return bufferbuilder;
    }

    private <E extends BlockEntity> void handleBlockEntity(SectionCompiler.Results p_343713_, E p_343478_) {
        BlockEntityRenderer<E, ?> blockentityrenderer = this.blockEntityRenderer.getRenderer(p_343478_);
        if (blockentityrenderer != null && !blockentityrenderer.shouldRenderOffScreen()) {
            p_343713_.blockEntities.add(p_343478_);
        }
    }

    public static Iterable<ChunkSectionLayer> getBlockRenderLayers(
        BlockStateModel model,
        BlockState blockState,
        BlockPos blockPos,
        RandomSource randomsource,
        ModelData modelData,
        SingleIterable<ChunkSectionLayer> singleLayer
    ) {
        if (FORGE) {
            randomsource.setSeed(blockState.getSeed(blockPos));
            return model.getRenderTypes(blockState, randomsource, modelData);
        } else {
            singleLayer.setValue(ItemBlockRenderTypes.getChunkRenderType(blockState));
            return singleLayer;
        }
    }

    private ChunkSectionLayer fixBlockLayer(BlockGetter worldReader, BlockState blockState, BlockPos blockPos, ChunkSectionLayer layer, boolean isMipmaps) {
        if (CustomBlockLayers.isActive()) {
            ChunkSectionLayer chunksectionlayer = CustomBlockLayers.getRenderLayer(worldReader, blockState, blockPos);
            if (chunksectionlayer != null) {
                return chunksectionlayer;
            }
        }

        return layer;
    }

    public static final class Results {
        public final List<BlockEntity> blockEntities = new ArrayList<>();
        public final Map<ChunkSectionLayer, MeshData> renderedLayers = new EnumMap<>(ChunkSectionLayer.class);
        public VisibilitySet visibilitySet = new VisibilitySet();
        public MeshData. SortState transparencyState;
        public BitSet[] animatedSprites = new BitSet[ChunkSectionLayer.VALUES.length];

        public void setAnimatedSprites(ChunkSectionLayer layer, BitSet animatedSprites) {
            this.animatedSprites[layer.ordinal()] = animatedSprites;
        }

        public void release() {
            this.renderedLayers.values().forEach(MeshData::close);
        }
    }
}
