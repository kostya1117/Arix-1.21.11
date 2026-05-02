package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.longs.Long2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntLinkedOpenHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.optifine.BetterSnow;
import net.optifine.BlockPosM;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.EmissiveTextures;
import net.optifine.model.BlockModelCustomizer;
import net.optifine.model.ListQuadsOverlay;
import net.optifine.reflect.Reflector;
import net.optifine.render.LightCacheOF;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class ModelBlockRenderer {
    private static final Direction[] DIRECTIONS = Direction.values();
    private final BlockColors blockColors;
    private static final int CACHE_SIZE = 100;
    static final ThreadLocal<ModelBlockRenderer.Cache> CACHE = ThreadLocal.withInitial(ModelBlockRenderer.Cache::new);
    private static float aoLightValueOpaque = 0.2F;
    private static boolean separateAoLightValue = false;
    private static final LightCacheOF LIGHT_CACHE_OF = new LightCacheOF();
    private static final ChunkSectionLayer[] OVERLAY_LAYERS = new ChunkSectionLayer[]{ChunkSectionLayer.CUTOUT, ChunkSectionLayer.TRANSLUCENT};
    private static boolean forge = Reflector.ForgeHooksClient.exists();

    public ModelBlockRenderer(BlockColors p_110999_) {
        this.blockColors = p_110999_;
    }

    public void tesselateBlock(
        BlockAndTintGetter p_234380_,
        List<BlockModelPart> p_393688_,
        BlockState p_234382_,
        BlockPos p_234383_,
        PoseStack p_234384_,
        VertexConsumer p_234385_,
        boolean p_234386_,
        int p_234389_
    ) {
        if (!p_393688_.isEmpty()) {
            boolean flag = Minecraft.useAmbientOcclusion() && p_234382_.getLightEmission(p_234380_, p_234383_) == 0 && ((BlockModelPart)p_393688_.getFirst()).useAmbientOcclusion();
            Vec3 vec3 = p_234382_.getOffset(p_234383_);
            p_234384_.translate(vec3);

            try {
                if (Config.isShaders()) {
                    SVertexBuilder.pushEntity(p_234382_, p_234385_);
                }

                RenderEnv renderenv = p_234385_.getRenderEnv(p_234382_, p_234383_);
                p_393688_ = BlockModelCustomizer.getRenderModel(p_393688_, p_234382_, renderenv);
                int i = p_234385_.getVertexCount();
                if (flag) {
                    this.tesselateWithAO(p_234380_, p_393688_, p_234382_, p_234383_, p_234384_, p_234385_, p_234386_, p_234389_);
                } else {
                    this.tesselateWithoutAO(p_234380_, p_393688_, p_234382_, p_234383_, p_234384_, p_234385_, p_234386_, p_234389_);
                }

                if (p_234385_.getVertexCount() != i) {
                    this.renderOverlayModels(p_234380_, p_393688_, p_234382_, p_234383_, p_234384_, p_234385_, p_234389_, p_234386_, renderenv, flag, vec3);
                }

                if (Config.isShaders()) {
                    SVertexBuilder.popEntity(p_234385_);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block model");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Block model being tesselated");
                CrashReportCategory.populateBlockDetails(crashreportcategory, p_234380_, p_234383_, p_234382_);
                crashreportcategory.setDetail("Using AO", flag);
                throw new ReportedException(crashreport);
            }
        }
    }

    private static boolean shouldRenderFace(BlockAndTintGetter p_391703_, BlockState p_396742_, boolean p_394639_, Direction p_397078_, BlockPos p_395848_) {
        if (!p_394639_) {
            return true;
        }

        BlockState blockstate = p_391703_.getBlockState(p_395848_);
        return Block.shouldRenderFace(p_396742_, blockstate, p_397078_);
    }

    public void tesselateWithAO(
        BlockAndTintGetter p_234391_,
        List<BlockModelPart> p_395824_,
        BlockState p_234393_,
        BlockPos p_234394_,
        PoseStack p_234395_,
        VertexConsumer p_234396_,
        boolean p_234397_,
        int p_234400_
    ) {
        RenderEnv renderenv = p_234396_.getRenderEnv(p_234393_, p_234394_);
        ChunkSectionLayer chunksectionlayer = p_234396_.getBlockLayer();
        ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage = renderenv.getAoRenderStorage();
        int i = 0;
        int j = 0;

        for (BlockModelPart blockmodelpart : p_395824_) {
            for (Direction direction : DIRECTIONS) {
                int k = 1 << direction.ordinal();
                boolean flag = (i & k) == 1;
                boolean flag1 = (j & k) == 1;
                if (!flag || flag1) {
                    List<BakedQuad> list = blockmodelpart.getQuads(direction);
                    if (!list.isEmpty()) {
                        if (!flag) {
                            flag1 = shouldRenderFace(
                                p_234391_,
                                p_234393_,
                                p_234397_,
                                direction,
                                modelblockrenderer$ambientocclusionrenderstorage.scratchPos.setWithOffset(p_234394_, direction)
                            );
                            i |= k;
                            if (flag1) {
                                j |= k;
                            }
                        }

                        if (flag1) {
                            list = BlockModelCustomizer.getRenderQuads(list, p_234391_, p_234393_, p_234394_, direction, chunksectionlayer, renderenv);
                            this.renderQuadsSmooth(
                                p_234391_,
                                p_234393_,
                                p_234394_,
                                p_234395_,
                                p_234396_,
                                list,
                                modelblockrenderer$ambientocclusionrenderstorage,
                                p_234400_,
                                renderenv
                            );
                        }
                    }
                }
            }

            List<BakedQuad> list1 = blockmodelpart.getQuads(null);
            if (!list1.isEmpty()) {
                list1 = BlockModelCustomizer.getRenderQuads(list1, p_234391_, p_234393_, p_234394_, null, chunksectionlayer, renderenv);
                this.renderQuadsSmooth(
                    p_234391_, p_234393_, p_234394_, p_234395_, p_234396_, list1, modelblockrenderer$ambientocclusionrenderstorage, p_234400_, renderenv
                );
            }
        }
    }

    public void tesselateWithoutAO(
        BlockAndTintGetter p_234402_,
        List<BlockModelPart> p_394148_,
        BlockState p_234404_,
        BlockPos p_234405_,
        PoseStack p_234406_,
        VertexConsumer p_234407_,
        boolean p_234408_,
        int p_234411_
    ) {
        RenderEnv renderenv = p_234407_.getRenderEnv(p_234404_, p_234405_);
        ChunkSectionLayer chunksectionlayer = p_234407_.getBlockLayer();
        ModelBlockRenderer.CommonRenderStorage modelblockrenderer$commonrenderstorage = renderenv.getCommonRenderStorage();
        int i = 0;
        int j = 0;

        for (BlockModelPart blockmodelpart : p_394148_) {
            for (Direction direction : DIRECTIONS) {
                int k = 1 << direction.ordinal();
                boolean flag = (i & k) == 1;
                boolean flag1 = (j & k) == 1;
                if (!flag || flag1) {
                    List<BakedQuad> list = blockmodelpart.getQuads(direction);
                    if (!list.isEmpty()) {
                        BlockPos blockpos = modelblockrenderer$commonrenderstorage.scratchPos.setWithOffset(p_234405_, direction);
                        if (!flag) {
                            flag1 = shouldRenderFace(p_234402_, p_234404_, p_234408_, direction, blockpos);
                            i |= k;
                            if (flag1) {
                                j |= k;
                            }
                        }

                        if (flag1) {
                            list = BlockModelCustomizer.getRenderQuads(list, p_234402_, p_234404_, p_234405_, direction, chunksectionlayer, renderenv);
                            int l = LightCacheOF.getPackedLight(p_234404_, p_234402_, blockpos);
                            this.renderQuadsFlat(
                                p_234402_,
                                p_234404_,
                                p_234405_,
                                l,
                                p_234411_,
                                false,
                                p_234406_,
                                p_234407_,
                                list,
                                modelblockrenderer$commonrenderstorage,
                                renderenv
                            );
                        }
                    }
                }
            }

            List<BakedQuad> list1 = blockmodelpart.getQuads(null);
            if (!list1.isEmpty()) {
                list1 = BlockModelCustomizer.getRenderQuads(list1, p_234402_, p_234404_, p_234405_, null, chunksectionlayer, renderenv);
                this.renderQuadsFlat(
                    p_234402_, p_234404_, p_234405_, -1, p_234411_, true, p_234406_, p_234407_, list1, modelblockrenderer$commonrenderstorage, renderenv
                );
            }
        }
    }

    private void renderQuadsSmooth(
        BlockAndTintGetter worldIn,
        BlockState stateIn,
        BlockPos posIn,
        PoseStack matrixStackIn,
        VertexConsumer buffer,
        List<BakedQuad> quadsIn,
        ModelBlockRenderer.AmbientOcclusionRenderStorage aoStorageIn,
        int combinedOverlayIn,
        RenderEnv renderEnv
    ) {
        int i = quadsIn.size();

        for (int j = 0; j < i; j++) {
            BakedQuad bakedquad = quadsIn.get(j);
            calculateShape(worldIn, stateIn, posIn, bakedquad, aoStorageIn);
            aoStorageIn.calculate(worldIn, stateIn, posIn, bakedquad.direction(), bakedquad.shade());
            if (bakedquad.sprite().isSpriteEmissive) {
                aoStorageIn.setMaxBlockLight();
            }

            this.renderQuadSmooth(worldIn, stateIn, posIn, buffer, matrixStackIn.last(), bakedquad, aoStorageIn, combinedOverlayIn, renderEnv);
        }
    }

    private void renderQuadSmooth(
        BlockAndTintGetter worldIn,
        BlockState stateIn,
        BlockPos posIn,
        VertexConsumer buffer,
        PoseStack.Pose matrixStackIn,
        BakedQuad quadIn,
        ModelBlockRenderer.CommonRenderStorage aoStorageIn,
        int combinedOverlayIn,
        RenderEnv renderEnv
    ) {
        int i = quadIn.tintIndex();
        int j = CustomColors.getColorMultiplier(quadIn, stateIn, worldIn, posIn, renderEnv);
        float f;
        float f1;
        float f2;
        if (j != -1) {
            f = ARGB.redFloat(j);
            f1 = ARGB.greenFloat(j);
            f2 = ARGB.blueFloat(j);
        } else if (i != -1) {
            int k;
            if (aoStorageIn.tintCacheIndex == i) {
                k = aoStorageIn.tintCacheValue;
            } else {
                k = this.blockColors.getColor(stateIn, worldIn, posIn, i);
                aoStorageIn.tintCacheIndex = i;
                aoStorageIn.tintCacheValue = k;
            }

            f = ARGB.redFloat(k);
            f1 = ARGB.greenFloat(k);
            f2 = ARGB.blueFloat(k);
        } else {
            f = 1.0F;
            f1 = 1.0F;
            f2 = 1.0F;
        }

        buffer.putBulkData(matrixStackIn, quadIn, aoStorageIn.brightness, f, f1, f2, 1.0F, aoStorageIn.lightmap, combinedOverlayIn);
    }

    private static void calculateShape(
        BlockAndTintGetter p_111040_, BlockState p_111041_, BlockPos p_111042_, BakedQuad p_454292_, ModelBlockRenderer.CommonRenderStorage p_394767_
    ) {
        float f = 32.0F;
        float f1 = 32.0F;
        float f2 = 32.0F;
        float f3 = -32.0F;
        float f4 = -32.0F;
        float f5 = -32.0F;

        for (int i = 0; i < 4; i++) {
            Vector3fc vector3fc = p_454292_.position(i);
            float f6 = vector3fc.x();
            float f7 = vector3fc.y();
            float f8 = vector3fc.z();
            f = Math.min(f, f6);
            f1 = Math.min(f1, f7);
            f2 = Math.min(f2, f8);
            f3 = Math.max(f3, f6);
            f4 = Math.max(f4, f7);
            f5 = Math.max(f5, f8);
        }

        if (p_394767_ instanceof ModelBlockRenderer.AmbientOcclusionRenderStorage modelblockrenderer$ambientocclusionrenderstorage) {
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.WEST.index] = f;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.EAST.index] = f3;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.DOWN.index] = f1;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.UP.index] = f4;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.NORTH.index] = f2;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.SOUTH.index] = f5;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_WEST.index] = 1.0F - f;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_EAST.index] = 1.0F - f3;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_DOWN.index] = 1.0F - f1;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_UP.index] = 1.0F - f4;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_NORTH.index] = 1.0F - f2;
            modelblockrenderer$ambientocclusionrenderstorage.faceShape[ModelBlockRenderer.SizeInfo.FLIP_SOUTH.index] = 1.0F - f5;
        }

        float f9 = 1.0E-4F;
        float f10 = 0.9999F;

        p_394767_.facePartial = switch (p_454292_.direction()) {
            case DOWN, UP -> f >= 1.0E-4F || f2 >= 1.0E-4F || f3 <= 0.9999F || f5 <= 0.9999F;
            case NORTH, SOUTH -> f >= 1.0E-4F || f1 >= 1.0E-4F || f3 <= 0.9999F || f4 <= 0.9999F;
            case WEST, EAST -> f1 >= 1.0E-4F || f2 >= 1.0E-4F || f4 <= 0.9999F || f5 <= 0.9999F;
        };

        p_394767_.faceCubic = switch (p_454292_.direction()) {
            case DOWN -> f1 == f4 && (f1 < 1.0E-4F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
            case UP -> f1 == f4 && (f4 > 0.9999F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
            case NORTH -> f2 == f5 && (f2 < 1.0E-4F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
            case SOUTH -> f2 == f5 && (f5 > 0.9999F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
            case WEST -> f == f3 && (f < 1.0E-4F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
            case EAST -> f == f3 && (f3 > 0.9999F || p_111041_.isCollisionShapeFullBlock(p_111040_, p_111042_));
        };
    }

    private void renderQuadsFlat(
        BlockAndTintGetter worldIn,
        BlockState stateIn,
        BlockPos posIn,
        int brightnessIn,
        int combinedOverlayIn,
        boolean ownBrightness,
        PoseStack matrixStackIn,
        VertexConsumer buffer,
        List<BakedQuad> quadsIn,
        ModelBlockRenderer.CommonRenderStorage aoStorageIn,
        RenderEnv renderEnv
    ) {
        int i = quadsIn.size();

        for (int j = 0; j < i; j++) {
            BakedQuad bakedquad = quadsIn.get(j);
            if (ownBrightness) {
                calculateShape(worldIn, stateIn, posIn, bakedquad, aoStorageIn);
                BlockPos blockpos = aoStorageIn.faceCubic ? aoStorageIn.scratchPos.setWithOffset(posIn, bakedquad.direction()) : posIn;
                brightnessIn = LightCacheOF.getPackedLight(stateIn, worldIn, blockpos);
            }

            if (bakedquad.sprite().isSpriteEmissive) {
                brightnessIn = LightTexture.MAX_BRIGHTNESS;
            }

            float f = worldIn.getShade(bakedquad.direction(), bakedquad.shade());
            aoStorageIn.brightness[0] = f;
            aoStorageIn.brightness[1] = f;
            aoStorageIn.brightness[2] = f;
            aoStorageIn.brightness[3] = f;
            aoStorageIn.lightmap[0] = brightnessIn;
            aoStorageIn.lightmap[1] = brightnessIn;
            aoStorageIn.lightmap[2] = brightnessIn;
            aoStorageIn.lightmap[3] = brightnessIn;
            this.renderQuadSmooth(worldIn, stateIn, posIn, buffer, matrixStackIn.last(), bakedquad, aoStorageIn, combinedOverlayIn, renderEnv);
        }
    }

    public static void renderModel(
        PoseStack.Pose p_111068_,
        VertexConsumer p_111069_,
        BlockStateModel p_397754_,
        float p_111072_,
        float p_111073_,
        float p_111074_,
        int p_111075_,
        int p_111076_
    ) {
        renderModel(p_111068_, p_111069_, p_397754_, p_111072_, p_111073_, p_111074_, p_111075_, p_111076_, ModelData.EMPTY, null);
    }

    public static void renderModel(
        PoseStack.Pose matrixEntry,
        VertexConsumer buffer,
        BlockStateModel modelIn,
        float red,
        float green,
        float blue,
        int combinedLightIn,
        int combinedOverlayIn,
        ModelData modelData,
        ChunkSectionLayer renderType
    ) {
        for (BlockModelPart blockmodelpart : forge
            ? modelIn.collectParts(RandomSource.create(42L), modelData, renderType)
            : modelIn.collectParts(RandomSource.create(42L))) {
            for (Direction direction : DIRECTIONS) {
                renderQuadList(matrixEntry, buffer, red, green, blue, blockmodelpart.getQuads(direction), combinedLightIn, combinedOverlayIn);
            }

            renderQuadList(matrixEntry, buffer, red, green, blue, blockmodelpart.getQuads(null), combinedLightIn, combinedOverlayIn);
        }
    }

    private static void renderQuadList(
        PoseStack.Pose p_111059_,
        VertexConsumer p_111060_,
        float p_111061_,
        float p_111062_,
        float p_111063_,
        List<BakedQuad> p_111064_,
        int p_111065_,
        int p_111066_
    ) {
        boolean flag = EmissiveTextures.isActive();

        for (BakedQuad bakedquad : p_111064_) {
            if (flag) {
                bakedquad = EmissiveTextures.getEmissiveQuad(bakedquad);
                if (bakedquad == null) {
                    continue;
                }
            }

            float f;
            float f1;
            float f2;
            if (bakedquad.isTinted()) {
                f = Mth.clamp(p_111061_, 0.0F, 1.0F);
                f1 = Mth.clamp(p_111062_, 0.0F, 1.0F);
                f2 = Mth.clamp(p_111063_, 0.0F, 1.0F);
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
            }

            p_111060_.putBulkData(p_111059_, bakedquad, f, f1, f2, 1.0F, p_111065_, p_111066_);
        }
    }

    public static void enableCaching() {
        CACHE.get().enable();
    }

    public static void clearCache() {
        CACHE.get().disable();
    }

    public static float fixAoLightValue(float val) {
        return val == 0.2F ? aoLightValueOpaque : val;
    }

    public static void updateAoLightValue() {
        aoLightValueOpaque = 1.0F - Config.getAmbientOcclusionLevel() * 0.8F;
        separateAoLightValue = Config.isShaders() && Shaders.isSeparateAo();
    }

    public static boolean isSeparateAoLightValue() {
        return separateAoLightValue;
    }

    private void renderOverlayModels(
        BlockAndTintGetter worldIn,
        List<BlockModelPart> partsIn,
        BlockState stateIn,
        BlockPos posIn,
        PoseStack matrixStackIn,
        VertexConsumer buffer,
        int combinedOverlayIn,
        boolean checkSides,
        RenderEnv renderEnv,
        boolean smooth,
        Vec3 renderOffset
    ) {
        if (renderEnv.isOverlaysRendered()) {
            renderEnv.setOverlaysRendered(false);

            for (int i = 0; i < OVERLAY_LAYERS.length; i++) {
                ChunkSectionLayer chunksectionlayer = OVERLAY_LAYERS[i];
                ListQuadsOverlay listquadsoverlay = renderEnv.getListQuadsOverlay(chunksectionlayer);
                if (listquadsoverlay.size() > 0) {
                    SectionCompiler sectioncompiler = renderEnv.getSectionCompiler();
                    Map<ChunkSectionLayer, BufferBuilder> map = renderEnv.getBufferBuilderMap();
                    SectionBufferBuilderPack sectionbufferbuilderpack = renderEnv.getSectionBufferBuilderPack();
                    Vector3f vector3f = Config.isShaders() ? buffer.getMidBlock() : null;
                    if (sectioncompiler != null && map != null && sectionbufferbuilderpack != null) {
                        BufferBuilder bufferbuilder = sectioncompiler.getOrBeginLayer(map, sectionbufferbuilderpack, chunksectionlayer);

                        for (int j = 0; j < listquadsoverlay.size(); j++) {
                            BakedQuad bakedquad = listquadsoverlay.getQuad(j);
                            List<BakedQuad> list = listquadsoverlay.getListQuadsSingle(bakedquad);
                            BlockState blockstate = listquadsoverlay.getBlockState(j);
                            if (bakedquad.getQuadEmissive() != null) {
                                listquadsoverlay.addQuad(bakedquad.getQuadEmissive(), blockstate);
                            }

                            renderEnv.reset(blockstate, posIn);
                            if (vector3f != null) {
                                bufferbuilder.setMidBlock(vector3f.x, vector3f.y, vector3f.z);
                            }

                            if (smooth) {
                                this.renderQuadsSmooth(
                                    worldIn,
                                    blockstate,
                                    posIn,
                                    matrixStackIn,
                                    bufferbuilder,
                                    list,
                                    renderEnv.getAoRenderStorage(),
                                    combinedOverlayIn,
                                    renderEnv
                                );
                            } else {
                                int k = LevelRenderer.getLightColor(
                                    LevelRenderer.BrightnessGetter.DEFAULT, worldIn, blockstate, posIn.relative(bakedquad.direction())
                                );
                                this.renderQuadsFlat(
                                    worldIn,
                                    blockstate,
                                    posIn,
                                    k,
                                    combinedOverlayIn,
                                    false,
                                    matrixStackIn,
                                    bufferbuilder,
                                    list,
                                    renderEnv.getAoRenderStorage(),
                                    renderEnv
                                );
                            }
                        }
                    }

                    listquadsoverlay.clear();
                }
            }
        }

        if (Config.isBetterSnow() && !renderEnv.isBreakingAnimation() && BetterSnow.shouldRender(worldIn, stateIn, posIn)) {
            BlockModelPart blockmodelpart = BetterSnow.getModelSnowLayer();
            BlockState blockstate1 = BetterSnow.getStateSnowLayer();
            matrixStackIn.translate(-renderOffset.x, -renderOffset.y, -renderOffset.z);
            List<BlockModelPart> list1 = renderEnv.getBlockModelParts(blockmodelpart);
            this.tesselateBlock(worldIn, list1, blockstate1, posIn, matrixStackIn, buffer, checkSides, combinedOverlayIn);
        }
    }

    protected enum AdjacencyInfo {
        DOWN(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH},
            0.5F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        UP(
            new Direction[]{Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH},
            1.0F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        NORTH(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST
            }
        ),
        SOUTH(
            new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP},
            0.8F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_WEST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.WEST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.WEST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.EAST
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_EAST,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.EAST,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.EAST
            }
        ),
        WEST(
            new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        ),
        EAST(
            new Direction[]{Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH},
            0.6F,
            true,
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.SOUTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.DOWN,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.NORTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_NORTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.NORTH
            },
            new ModelBlockRenderer.SizeInfo[]{
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.SOUTH,
                ModelBlockRenderer.SizeInfo.FLIP_UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.FLIP_SOUTH,
                ModelBlockRenderer.SizeInfo.UP,
                ModelBlockRenderer.SizeInfo.SOUTH
            }
        );

        final Direction[] corners;
        final boolean doNonCubicWeight;
        final ModelBlockRenderer.SizeInfo[] vert0Weights;
        final ModelBlockRenderer.SizeInfo[] vert1Weights;
        final ModelBlockRenderer.SizeInfo[] vert2Weights;
        final ModelBlockRenderer.SizeInfo[] vert3Weights;
        private static final ModelBlockRenderer.AdjacencyInfo[] BY_FACING = Util.make(new ModelBlockRenderer.AdjacencyInfo[6], infoIn -> {
            infoIn[Direction.DOWN.get3DDataValue()] = DOWN;
            infoIn[Direction.UP.get3DDataValue()] = UP;
            infoIn[Direction.NORTH.get3DDataValue()] = NORTH;
            infoIn[Direction.SOUTH.get3DDataValue()] = SOUTH;
            infoIn[Direction.WEST.get3DDataValue()] = WEST;
            infoIn[Direction.EAST.get3DDataValue()] = EAST;
        });

        AdjacencyInfo(
            final Direction[] p_111122_,
            final float p_111123_,
            final boolean p_111124_,
            final ModelBlockRenderer.SizeInfo[] p_111125_,
            final ModelBlockRenderer.SizeInfo[] p_111126_,
            final ModelBlockRenderer.SizeInfo[] p_111127_,
            final ModelBlockRenderer.SizeInfo[] p_111128_
        ) {
            this.corners = p_111122_;
            this.doNonCubicWeight = p_111124_;
            this.vert0Weights = p_111125_;
            this.vert1Weights = p_111126_;
            this.vert2Weights = p_111127_;
            this.vert3Weights = p_111128_;
        }

        public static ModelBlockRenderer.AdjacencyInfo fromFacing(Direction p_111132_) {
            return BY_FACING[p_111132_.get3DDataValue()];
        }
    }

    public static class AmbientOcclusionRenderStorage extends ModelBlockRenderer.CommonRenderStorage {
        final float[] faceShape = new float[ModelBlockRenderer.SizeInfo.COUNT];
        private BlockPosM blockPos = new BlockPosM();

        public void setMaxBlockLight() {
            int i = LightTexture.MAX_BRIGHTNESS;
            this.lightmap[0] = i;
            this.lightmap[1] = i;
            this.lightmap[2] = i;
            this.lightmap[3] = i;
            this.brightness[0] = 1.0F;
            this.brightness[1] = 1.0F;
            this.brightness[2] = 1.0F;
            this.brightness[3] = 1.0F;
        }

        public void calculate(BlockAndTintGetter p_392472_, BlockState p_392545_, BlockPos p_393020_, Direction p_392793_, boolean p_394796_) {
            BlockPos blockpos = this.faceCubic ? p_393020_.relative(p_392793_) : p_393020_;
            ModelBlockRenderer.AdjacencyInfo modelblockrenderer$adjacencyinfo = ModelBlockRenderer.AdjacencyInfo.fromFacing(p_392793_);
            BlockPosM blockposm = this.blockPos;
            blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0]);
            BlockState blockstate = p_392472_.getBlockState(blockposm);
            int i = LightCacheOF.getPackedLight(blockstate, p_392472_, blockposm);
            float f = LightCacheOF.getBrightness(blockstate, p_392472_, blockposm);
            blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1]);
            BlockState blockstate1 = p_392472_.getBlockState(blockposm);
            int j = LightCacheOF.getPackedLight(blockstate1, p_392472_, blockposm);
            float f1 = LightCacheOF.getBrightness(blockstate1, p_392472_, blockposm);
            blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[2]);
            BlockState blockstate2 = p_392472_.getBlockState(blockposm);
            int k = LightCacheOF.getPackedLight(blockstate2, p_392472_, blockposm);
            float f2 = LightCacheOF.getBrightness(blockstate2, p_392472_, blockposm);
            blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[3]);
            BlockState blockstate3 = p_392472_.getBlockState(blockposm);
            int l = LightCacheOF.getPackedLight(blockstate3, p_392472_, blockposm);
            float f3 = LightCacheOF.getBrightness(blockstate3, p_392472_, blockposm);
            BlockState blockstate4 = blockstate;
            boolean flag = !blockstate4.isViewBlocking(p_392472_, blockposm) || blockstate4.getLightBlock() == 0;
            BlockState blockstate5 = blockstate1;
            boolean flag1 = !blockstate5.isViewBlocking(p_392472_, blockposm) || blockstate5.getLightBlock() == 0;
            BlockState blockstate6 = blockstate2;
            boolean flag2 = !blockstate6.isViewBlocking(p_392472_, blockposm) || blockstate6.getLightBlock() == 0;
            BlockState blockstate7 = blockstate3;
            boolean flag3 = !blockstate7.isViewBlocking(p_392472_, blockposm) || blockstate7.getLightBlock() == 0;
            float f4;
            int i1;
            if (!flag2 && !flag) {
                f4 = (f + f2) / 2.0F;
                i1 = blend(i, k, 0, 0);
            } else {
                blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0], modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate8 = p_392472_.getBlockState(blockposm);
                f4 = LightCacheOF.getBrightness(blockstate8, p_392472_, blockposm);
                i1 = LightCacheOF.getPackedLight(blockstate8, p_392472_, blockposm);
            }

            int j1;
            float f26;
            if (!flag3 && !flag) {
                f26 = (f + f3) / 2.0F;
                j1 = blend(i, l, 0, 0);
            } else {
                blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[0], modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate9 = p_392472_.getBlockState(blockposm);
                f26 = LightCacheOF.getBrightness(blockstate9, p_392472_, blockposm);
                j1 = LightCacheOF.getPackedLight(blockstate9, p_392472_, blockposm);
            }

            int k1;
            float f27;
            if (!flag2 && !flag1) {
                f27 = (f1 + f2) / 2.0F;
                k1 = blend(j, k, 0, 0);
            } else {
                blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1], modelblockrenderer$adjacencyinfo.corners[2]);
                BlockState blockstate10 = p_392472_.getBlockState(blockposm);
                f27 = LightCacheOF.getBrightness(blockstate10, p_392472_, blockposm);
                k1 = LightCacheOF.getPackedLight(blockstate10, p_392472_, blockposm);
            }

            int l1;
            float f28;
            if (!flag3 && !flag1) {
                f28 = (f1 + f3) / 2.0F;
                l1 = blend(j, l, 0, 0);
            } else {
                blockposm.setPosOffset(blockpos, modelblockrenderer$adjacencyinfo.corners[1], modelblockrenderer$adjacencyinfo.corners[3]);
                BlockState blockstate11 = p_392472_.getBlockState(blockposm);
                f28 = LightCacheOF.getBrightness(blockstate11, p_392472_, blockposm);
                l1 = LightCacheOF.getPackedLight(blockstate11, p_392472_, blockposm);
            }

            int i3 = LightCacheOF.getPackedLight(p_392545_, p_392472_, p_393020_);
            blockposm.setPosOffset(p_393020_, p_392793_);
            BlockState blockstate12 = p_392472_.getBlockState(blockposm);
            if (this.faceCubic || !blockstate12.isSolidRender()) {
                i3 = LightCacheOF.getPackedLight(blockstate12, p_392472_, blockposm);
            }

            float f5 = this.faceCubic
                ? LightCacheOF.getBrightness(p_392472_.getBlockState(blockpos), p_392472_, blockpos)
                : LightCacheOF.getBrightness(p_392472_.getBlockState(p_393020_), p_392472_, p_393020_);
            ModelBlockRenderer.AmbientVertexRemap modelblockrenderer$ambientvertexremap = ModelBlockRenderer.AmbientVertexRemap.fromFacing(p_392793_);
            if (this.facePartial && modelblockrenderer$adjacencyinfo.doNonCubicWeight) {
                float f29 = (f3 + f + f26 + f5) * 0.25F;
                float f31 = (f2 + f + f4 + f5) * 0.25F;
                float f32 = (f2 + f1 + f27 + f5) * 0.25F;
                float f33 = (f3 + f1 + f28 + f5) * 0.25F;
                float f10 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[1].index];
                float f11 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[3].index];
                float f12 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[5].index];
                float f13 = this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert0Weights[7].index];
                float f14 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[1].index];
                float f15 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[3].index];
                float f16 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[5].index];
                float f17 = this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert1Weights[7].index];
                float f18 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[1].index];
                float f19 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[3].index];
                float f20 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[5].index];
                float f21 = this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert2Weights[7].index];
                float f22 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[0].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[1].index];
                float f23 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[2].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[3].index];
                float f24 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[4].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[5].index];
                float f25 = this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[6].index]
                    * this.faceShape[modelblockrenderer$adjacencyinfo.vert3Weights[7].index];
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = Math.clamp(f29 * f10 + f31 * f11 + f32 * f12 + f33 * f13, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = Math.clamp(f29 * f14 + f31 * f15 + f32 * f16 + f33 * f17, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = Math.clamp(f29 * f18 + f31 * f19 + f32 * f20 + f33 * f21, 0.0F, 1.0F);
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = Math.clamp(f29 * f22 + f31 * f23 + f32 * f24 + f33 * f25, 0.0F, 1.0F);
                int i2 = blend(l, i, j1, i3);
                int j2 = blend(k, i, i1, i3);
                int k2 = blend(k, j, k1, i3);
                int l2 = blend(l, j, l1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(i2, j2, k2, l2, f10, f11, f12, f13);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(i2, j2, k2, l2, f14, f15, f16, f17);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(i2, j2, k2, l2, f18, f19, f20, f21);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(i2, j2, k2, l2, f22, f23, f24, f25);
            } else {
                float f6 = (f3 + f + f26 + f5) * 0.25F;
                float f7 = (f2 + f + f4 + f5) * 0.25F;
                float f8 = (f2 + f1 + f27 + f5) * 0.25F;
                float f9 = (f3 + f1 + f28 + f5) * 0.25F;
                this.lightmap[modelblockrenderer$ambientvertexremap.vert0] = blend(l, i, j1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert1] = blend(k, i, i1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert2] = blend(k, j, k1, i3);
                this.lightmap[modelblockrenderer$ambientvertexremap.vert3] = blend(l, j, l1, i3);
                this.brightness[modelblockrenderer$ambientvertexremap.vert0] = f6;
                this.brightness[modelblockrenderer$ambientvertexremap.vert1] = f7;
                this.brightness[modelblockrenderer$ambientvertexremap.vert2] = f8;
                this.brightness[modelblockrenderer$ambientvertexremap.vert3] = f9;
            }

            float f30 = p_392472_.getShade(p_392793_, p_394796_);

            for (int j3 = 0; j3 < this.brightness.length; j3++) {
                this.brightness[j3] = this.brightness[j3] * f30;
            }
        }

        public static int blend(int p_394213_, int p_397905_, int p_392318_, int p_397150_) {
            if (p_394213_ != 15794417 && p_397905_ != 15794417 && p_392318_ != 15794417 && p_397150_ != 15794417) {
                int i = p_394213_ + p_397905_ + p_392318_ + p_397150_;
                int j = 4;
                if (p_394213_ == 0) {
                    j--;
                }

                if (p_397905_ == 0) {
                    j--;
                }

                if (p_392318_ == 0) {
                    j--;
                }

                if (p_397150_ == 0) {
                    j--;
                }

                switch (j) {
                    case 0:
                    case 1:
                        return i;
                    case 2:
                        return i >> 1 & 16711935;
                    case 3:
                        return i / 3 & 0xFF0000 | (i & 65535) / 3;
                    default:
                        return i >> 2 & 16711935;
                }
            } else {
                return p_394213_ + p_397905_ + p_392318_ + p_397150_ >> 2 & 16711935;
            }
        }

        private static int blend(
            int p_395262_, int p_396930_, int p_395695_, int p_393923_, float p_397544_, float p_391464_, float p_398036_, float p_397919_
        ) {
            int i = (int)(
                    (p_395262_ >> 16 & 0xFF) * p_397544_
                        + (p_396930_ >> 16 & 0xFF) * p_391464_
                        + (p_395695_ >> 16 & 0xFF) * p_398036_
                        + (p_393923_ >> 16 & 0xFF) * p_397919_
                )
                & 0xFF;
            int j = (int)((p_395262_ & 0xFF) * p_397544_ + (p_396930_ & 0xFF) * p_391464_ + (p_395695_ & 0xFF) * p_398036_ + (p_393923_ & 0xFF) * p_397919_)
                & 0xFF;
            return i << 16 | j;
        }
    }

    enum AmbientVertexRemap {
        DOWN(0, 1, 2, 3),
        UP(2, 3, 0, 1),
        NORTH(3, 0, 1, 2),
        SOUTH(0, 1, 2, 3),
        WEST(3, 0, 1, 2),
        EAST(1, 2, 3, 0);

        final int vert0;
        final int vert1;
        final int vert2;
        final int vert3;
        private static final ModelBlockRenderer.AmbientVertexRemap[] BY_FACING = Util.make(new ModelBlockRenderer.AmbientVertexRemap[6], remapIn -> {
            remapIn[Direction.DOWN.get3DDataValue()] = DOWN;
            remapIn[Direction.UP.get3DDataValue()] = UP;
            remapIn[Direction.NORTH.get3DDataValue()] = NORTH;
            remapIn[Direction.SOUTH.get3DDataValue()] = SOUTH;
            remapIn[Direction.WEST.get3DDataValue()] = WEST;
            remapIn[Direction.EAST.get3DDataValue()] = EAST;
        });

        AmbientVertexRemap(final int p_111195_, final int p_111196_, final int p_111197_, final int p_111198_) {
            this.vert0 = p_111195_;
            this.vert1 = p_111196_;
            this.vert2 = p_111197_;
            this.vert3 = p_111198_;
        }

        public static ModelBlockRenderer.AmbientVertexRemap fromFacing(Direction p_111202_) {
            return BY_FACING[p_111202_.get3DDataValue()];
        }
    }

    static class Cache {
        private boolean enabled;
        private final Long2IntLinkedOpenHashMap colorCache = Util.make(() -> {
            Long2IntLinkedOpenHashMap long2intlinkedopenhashmap = new Long2IntLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int p_111238_) {
                }
            };
            long2intlinkedopenhashmap.defaultReturnValue(Integer.MAX_VALUE);
            return long2intlinkedopenhashmap;
        });
        private final Long2FloatLinkedOpenHashMap brightnessCache = Util.make(() -> {
            Long2FloatLinkedOpenHashMap long2floatlinkedopenhashmap = new Long2FloatLinkedOpenHashMap(100, 0.25F) {
                @Override
                protected void rehash(int p_111245_) {
                }
            };
            long2floatlinkedopenhashmap.defaultReturnValue(Float.NaN);
            return long2floatlinkedopenhashmap;
        });
        private final LevelRenderer.BrightnessGetter cachedBrightnessGetter = (blockGetterIn, blockPosIn) -> {
            long i = blockPosIn.asLong();
            int j = this.colorCache.get(i);
            if (j != Integer.MAX_VALUE) {
                return j;
            }

            int k = LevelRenderer.BrightnessGetter.DEFAULT.packedBrightness(blockGetterIn, blockPosIn);
            if (this.colorCache.size() == 100) {
                this.colorCache.removeFirstInt();
            }

            this.colorCache.put(i, k);
            return k;
        };

        private Cache() {
        }

        public void enable() {
            this.enabled = true;
        }

        public void disable() {
            this.enabled = false;
            this.colorCache.clear();
            this.brightnessCache.clear();
        }

        public int getLightColor(BlockState p_111222_, BlockAndTintGetter p_111223_, BlockPos p_111224_) {
            return LevelRenderer.getLightColor(this.enabled ? this.cachedBrightnessGetter : LevelRenderer.BrightnessGetter.DEFAULT, p_111223_, p_111222_, p_111224_);
        }

        public float getShadeBrightness(BlockState p_111227_, BlockAndTintGetter p_111228_, BlockPos p_111229_) {
            long i = p_111229_.asLong();
            if (this.enabled) {
                float f = this.brightnessCache.get(i);
                if (!Float.isNaN(f)) {
                    return f;
                }
            }

            float f1 = p_111227_.getShadeBrightness(p_111228_, p_111229_);
            if (this.enabled) {
                if (this.brightnessCache.size() == 100) {
                    this.brightnessCache.removeFirstFloat();
                }

                this.brightnessCache.put(i, f1);
            }

            return f1;
        }
    }

    public static class CommonRenderStorage {
        public final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
        public boolean faceCubic;
        public boolean facePartial;
        public final float[] brightness = new float[4];
        public final int[] lightmap = new int[4];
        public int tintCacheIndex = -1;
        public int tintCacheValue;
        public final LightCacheOF cache = ModelBlockRenderer.LIGHT_CACHE_OF;

        public void resetCache() {
            this.tintCacheIndex = -1;
            this.tintCacheValue = 0;
        }
    }

    protected enum SizeInfo {
        DOWN(0),
        UP(1),
        NORTH(2),
        SOUTH(3),
        WEST(4),
        EAST(5),
        FLIP_DOWN(6),
        FLIP_UP(7),
        FLIP_NORTH(8),
        FLIP_SOUTH(9),
        FLIP_WEST(10),
        FLIP_EAST(11);

        public static final int COUNT = values().length;
        final int index;

        SizeInfo(final int p_396212_) {
            this.index = p_396212_;
        }
    }
}
