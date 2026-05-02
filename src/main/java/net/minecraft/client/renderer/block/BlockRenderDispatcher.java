package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.reflect.Reflector;
import org.jspecify.annotations.Nullable;

public class BlockRenderDispatcher implements ResourceManagerReloadListener {
    private final BlockModelShaper blockModelShaper;
    private final MaterialSet materials;
    private final ModelBlockRenderer modelRenderer;
    private  LiquidBlockRenderer liquidBlockRenderer;
    private final RandomSource singleThreadRandom = RandomSource.create();
    private final List<BlockModelPart> singleThreadPartList = new ArrayList<>();
    private final BlockColors blockColors;

    public BlockRenderDispatcher(BlockModelShaper p_173399_, MaterialSet p_424299_, BlockColors p_173401_) {
        this.blockModelShaper = p_173399_;
        this.materials = p_424299_;
        this.blockColors = p_173401_;
        this.modelRenderer = new ModelBlockRenderer(this.blockColors);
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    public void renderBreakingTexture(BlockState p_110919_, BlockPos p_110920_, BlockAndTintGetter p_110921_, PoseStack p_110922_, VertexConsumer p_110923_) {
        this.renderBreakingTexture(p_110919_, p_110920_, p_110921_, p_110922_, p_110923_, ModelData.EMPTY);
    }

    public void renderBreakingTexture(
        BlockState blockStateIn, BlockPos posIn, BlockAndTintGetter lightReaderIn, PoseStack matrixStackIn, VertexConsumer vertexBuilderIn, ModelData modelData
    ) {
        if (blockStateIn.getRenderShape() == RenderShape.MODEL) {
            BlockStateModel blockstatemodel = this.blockModelShaper.getBlockModel(blockStateIn);
            this.singleThreadRandom.setSeed(blockStateIn.getSeed(posIn));
            this.singleThreadPartList.clear();
            blockstatemodel.collectParts(this.singleThreadRandom, this.singleThreadPartList, modelData, null);
            this.modelRenderer.tesselateBlock(lightReaderIn, this.singleThreadPartList, blockStateIn, posIn, matrixStackIn, vertexBuilderIn, true, OverlayTexture.NO_OVERLAY);
        }
    }

    public void renderBatched(
        BlockState p_234356_,
        BlockPos p_234357_,
        BlockAndTintGetter p_234358_,
        PoseStack p_234359_,
        VertexConsumer p_234360_,
        boolean p_234361_,
        List<BlockModelPart> p_393078_
    ) {
        try {
            this.modelRenderer.tesselateBlock(p_234358_, p_393078_, p_234356_, p_234357_, p_234359_, p_234360_, p_234361_, OverlayTexture.NO_OVERLAY);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234358_, p_234357_, p_234356_);
            throw new ReportedException(crashreport);
        }
    }

    public void renderLiquid(BlockPos p_234364_, BlockAndTintGetter p_234365_, VertexConsumer p_234366_, BlockState p_234367_, FluidState p_234368_) {
        try {
            Objects.requireNonNull(this.liquidBlockRenderer).tesselate(p_234365_, p_234364_, p_234366_, p_234367_, p_234368_);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating liquid in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234365_, p_234364_, p_234367_);
            throw new ReportedException(crashreport);
        }
    }

    public ModelBlockRenderer getModelRenderer() {
        return this.modelRenderer;
    }

    public BlockStateModel getBlockModel(BlockState p_110911_) {
        return this.blockModelShaper.getBlockModel(p_110911_);
    }

    public void renderSingleBlock(BlockState p_110913_, PoseStack p_110914_, MultiBufferSource p_110915_, int p_110916_, int p_110917_) {
        this.renderSingleBlock(p_110913_, p_110914_, p_110915_, p_110916_, p_110917_, ModelData.EMPTY, null);
    }

    public void renderSingleBlock(
        BlockState blockStateIn,
        PoseStack matrixStackIn,
        MultiBufferSource bufferTypeIn,
        int combinedLightIn,
        int combinedOverlayIn,
        ModelData modelData,
        RenderType renderType
    ) {
        RenderShape rendershape = blockStateIn.getRenderShape();
        if (rendershape != RenderShape.INVISIBLE) {
            BlockStateModel blockstatemodel = this.getBlockModel(blockStateIn);
            int i = this.blockColors.getColor(blockStateIn, null, null, 0);
            float f = (i >> 16 & 0xFF) / 255.0F;
            float f1 = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i & 0xFF) / 255.0F;
            if (Reflector.ForgeHooksClient.exists()) {
                for (ChunkSectionLayer chunksectionlayer : blockstatemodel.getRenderTypes(blockStateIn, RandomSource.create(42L), modelData)) {
                    ModelBlockRenderer.renderModel(
                        matrixStackIn.last(),
                        bufferTypeIn.getBuffer(renderType != null ? renderType : RenderTypeHelper.getEntityRenderType(chunksectionlayer)),
                        blockstatemodel,
                        f,
                        f1,
                        f2,
                        combinedLightIn,
                        combinedOverlayIn,
                        modelData,
                        chunksectionlayer
                    );
                }
            } else {
                ModelBlockRenderer.renderModel(
                    matrixStackIn.last(),
                    bufferTypeIn.getBuffer(ItemBlockRenderTypes.getRenderType(blockStateIn)),
                    blockstatemodel,
                    f,
                    f1,
                    f2,
                    combinedLightIn,
                    combinedOverlayIn
                );
            }
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_110909_) {
        this.liquidBlockRenderer = new LiquidBlockRenderer(this.materials);
    }
}
