package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;
import net.optifine.render.RegionLayerRenderer;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;

public class LiquidBlockRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;
    private final TextureAtlasSprite lavaStill;
    private final TextureAtlasSprite lavaFlowing;
    private final TextureAtlasSprite waterStill;
    private final TextureAtlasSprite waterFlowing;
    private final TextureAtlasSprite waterOverlay;

    public LiquidBlockRenderer(MaterialSet p_451072_) {
        this.lavaStill = p_451072_.get(ModelBakery.LAVA_STILL);
        this.lavaFlowing = p_451072_.get(ModelBakery.LAVA_FLOW);
        this.waterStill = p_451072_.get(ModelBakery.WATER_STILL);
        this.waterFlowing = p_451072_.get(ModelBakery.WATER_FLOW);
        this.waterOverlay = p_451072_.get(ModelBakery.WATER_OVERLAY);
    }

    private static boolean isNeighborSameFluid(FluidState p_203186_, FluidState p_203187_) {
        return p_203187_.getType().isSame(p_203186_.getType());
    }

    private static boolean isFaceOccludedByState(Direction p_110980_, float p_110981_, BlockState p_110983_) {
        VoxelShape voxelshape = p_110983_.getFaceOcclusionShape(p_110980_.getOpposite());
        if (voxelshape == Shapes.empty()) {
            return false;
        } else if (voxelshape == Shapes.block()) {
            boolean flag = p_110981_ == 1.0F;
            return p_110980_ != Direction.UP || flag;
        } else {
            VoxelShape voxelshape1 = Shapes.box(0.0, 0.0, 0.0, 1.0, p_110981_, 1.0);
            return Shapes.blockOccludes(voxelshape1, voxelshape, p_110980_);
        }
    }

    private static boolean isFaceOccludedByNeighbor(Direction p_203182_, float p_203183_, BlockState p_203184_) {
        return isFaceOccludedByState(p_203182_, p_203183_, p_203184_);
    }

    private static boolean isFaceOccludedBySelf(BlockState p_110962_, Direction p_110963_) {
        return isFaceOccludedByState(p_110963_.getOpposite(), 1.0F, p_110962_);
    }

    public static boolean shouldRenderFace(FluidState p_203169_, BlockState p_203170_, Direction p_203171_, FluidState p_203172_) {
        return !isFaceOccludedBySelf(p_203170_, p_203171_) && !isNeighborSameFluid(p_203169_, p_203172_);
    }

    public void tesselate(BlockAndTintGetter p_234370_, BlockPos p_234371_, VertexConsumer p_234372_, BlockState p_234373_, FluidState p_234374_) {
        BlockState blockstate = p_234374_.createLegacyBlock();

        try {
            if (Config.isShaders()) {
                SVertexBuilder.pushEntity(blockstate, p_234372_);
            }

            boolean flag = p_234374_.is(FluidTags.LAVA);
            TextureAtlasSprite textureatlassprite = flag ? this.lavaStill : this.waterStill;
            TextureAtlasSprite textureatlassprite1 = flag ? this.lavaFlowing : this.waterFlowing;
            TextureAtlasSprite[] atextureatlassprite = null;
            if (Reflector.ForgeHooksClient_getFluidSprites.exists()) {
                atextureatlassprite = (TextureAtlasSprite[])Reflector.call(Reflector.ForgeHooksClient_getFluidSprites, p_234370_, p_234371_, p_234374_);
                if (atextureatlassprite != null && atextureatlassprite.length >= 2) {
                    textureatlassprite = atextureatlassprite[0];
                    textureatlassprite1 = atextureatlassprite[1];
                }
            }

            RenderEnv renderenv = p_234372_.getRenderEnv(blockstate, p_234371_);
            boolean flag1 = !flag && Minecraft.useAmbientOcclusion();
            int i = -1;
            float f = 1.0F;
            if (Reflector.ForgeHooksClient.exists()) {
                i = IClientFluidTypeExtensions.of(p_234374_).getTintColor(p_234374_, p_234370_, p_234371_);
                f = (i >> 24 & 0xFF) / 255.0F;
            }

            BlockState blockstate1 = p_234370_.getBlockState(p_234371_.relative(Direction.DOWN));
            FluidState fluidstate = blockstate1.getFluidState();
            BlockState blockstate2 = p_234370_.getBlockState(p_234371_.relative(Direction.UP));
            FluidState fluidstate1 = blockstate2.getFluidState();
            BlockState blockstate3 = p_234370_.getBlockState(p_234371_.relative(Direction.NORTH));
            FluidState fluidstate2 = blockstate3.getFluidState();
            BlockState blockstate4 = p_234370_.getBlockState(p_234371_.relative(Direction.SOUTH));
            FluidState fluidstate3 = blockstate4.getFluidState();
            BlockState blockstate5 = p_234370_.getBlockState(p_234371_.relative(Direction.WEST));
            FluidState fluidstate4 = blockstate5.getFluidState();
            BlockState blockstate6 = p_234370_.getBlockState(p_234371_.relative(Direction.EAST));
            FluidState fluidstate5 = blockstate6.getFluidState();
            boolean flag2 = !isNeighborSameFluid(p_234374_, fluidstate1);
            boolean flag3 = shouldRenderFace(p_234374_, p_234373_, Direction.DOWN, fluidstate) && !isFaceOccludedByNeighbor(Direction.DOWN, 0.8888889F, blockstate1);
            boolean flag4 = shouldRenderFace(p_234374_, p_234373_, Direction.NORTH, fluidstate2);
            boolean flag5 = shouldRenderFace(p_234374_, p_234373_, Direction.SOUTH, fluidstate3);
            boolean flag6 = shouldRenderFace(p_234374_, p_234373_, Direction.WEST, fluidstate4);
            boolean flag7 = shouldRenderFace(p_234374_, p_234373_, Direction.EAST, fluidstate5);
            if (flag2 || flag3 || flag7 || flag6 || flag4 || flag5) {
                if (i < 0) {
                    i = CustomColors.getFluidColor(p_234370_, blockstate, p_234371_, renderenv);
                }

                float f1 = (i >> 16 & 0xFF) / 255.0F;
                float f2 = (i >> 8 & 0xFF) / 255.0F;
                float f3 = (i & 0xFF) / 255.0F;
                float f4 = p_234370_.getShade(Direction.DOWN, true);
                float f5 = p_234370_.getShade(Direction.UP, true);
                float f6 = p_234370_.getShade(Direction.NORTH, true);
                float f7 = p_234370_.getShade(Direction.WEST, true);
                Fluid fluid = p_234374_.getType();
                float f8 = this.getHeight(p_234370_, fluid, p_234371_, p_234373_, p_234374_);
                float f9;
                float f10;
                float f11;
                float f12;
                if (f8 >= 1.0F) {
                    f9 = 1.0F;
                    f10 = 1.0F;
                    f11 = 1.0F;
                    f12 = 1.0F;
                } else {
                    float f13 = this.getHeight(p_234370_, fluid, p_234371_.north(), blockstate3, fluidstate2);
                    float f14 = this.getHeight(p_234370_, fluid, p_234371_.south(), blockstate4, fluidstate3);
                    float f15 = this.getHeight(p_234370_, fluid, p_234371_.east(), blockstate6, fluidstate5);
                    float f16 = this.getHeight(p_234370_, fluid, p_234371_.west(), blockstate5, fluidstate4);
                    f9 = this.calculateAverageHeight(p_234370_, fluid, f8, f13, f15, p_234371_.relative(Direction.NORTH).relative(Direction.EAST));
                    f10 = this.calculateAverageHeight(p_234370_, fluid, f8, f13, f16, p_234371_.relative(Direction.NORTH).relative(Direction.WEST));
                    f11 = this.calculateAverageHeight(p_234370_, fluid, f8, f14, f15, p_234371_.relative(Direction.SOUTH).relative(Direction.EAST));
                    f12 = this.calculateAverageHeight(p_234370_, fluid, f8, f14, f16, p_234371_.relative(Direction.SOUTH).relative(Direction.WEST));
                }

                float f23 = p_234371_.getX() & 15;
                float f24 = p_234371_.getY() & 15;
                float f25 = p_234371_.getZ() & 15;
                if (Config.isRenderRegions() && RegionLayerRenderer.canRenderLayer(p_234372_.getBlockLayer())) {
                    int l4 = p_234371_.getX() >> 4 << 4;
                    int j = p_234371_.getY() >> 4 << 4;
                    int k = p_234371_.getZ() >> 4 << 4;
                    int l = 8;
                    int i1 = l4 >> l << l;
                    int j1 = k >> l << l;
                    int k1 = l4 - i1;
                    int l1 = j;
                    int i2 = k - j1;
                    f23 += k1;
                    f24 += l1;
                    f25 += i2;
                }

                if (Config.isShaders() && Shaders.useMidBlockAttrib) {
                    p_234372_.setMidBlock((float)(f23 + 0.5), (float)(f24 + 0.5), (float)(f25 + 0.5));
                }

                float f26 = 0.001F;
                float f27 = flag3 ? 0.001F : 0.0F;
                if (flag2 && !isFaceOccludedByNeighbor(Direction.UP, Math.min(Math.min(f10, f12), Math.min(f11, f9)), blockstate2)) {
                    f10 -= 0.001F;
                    f12 -= 0.001F;
                    f11 -= 0.001F;
                    f9 -= 0.001F;
                    Vec3 vec3 = p_234374_.getFlow(p_234370_, p_234371_);
                    float f17;
                    float f18;
                    float f29;
                    float f31;
                    float f33;
                    float f36;
                    float f38;
                    float f41;
                    if (vec3.x == 0.0 && vec3.z == 0.0) {
                        p_234372_.setSprite(textureatlassprite);
                        f29 = textureatlassprite.getU(0.0F);
                        f38 = textureatlassprite.getV(0.0F);
                        f31 = f29;
                        f41 = textureatlassprite.getV(1.0F);
                        f33 = textureatlassprite.getU(1.0F);
                        f17 = f41;
                        f36 = f33;
                        f18 = f38;
                    } else {
                        float f19 = (float)Mth.atan2(vec3.z, vec3.x) - (float) (Math.PI / 2);
                        float f20 = Mth.sin(f19) * 0.25F;
                        float f21 = Mth.cos(f19) * 0.25F;
                        float f22 = 0.5F;
                        p_234372_.setSprite(textureatlassprite1);
                        f29 = textureatlassprite1.getU(0.5F + (-f21 - f20));
                        f38 = textureatlassprite1.getV(0.5F + (-f21 + f20));
                        f31 = textureatlassprite1.getU(0.5F + (-f21 + f20));
                        f41 = textureatlassprite1.getV(0.5F + (f21 + f20));
                        f33 = textureatlassprite1.getU(0.5F + (f21 + f20));
                        f17 = textureatlassprite1.getV(0.5F + (f21 - f20));
                        f36 = textureatlassprite1.getU(0.5F + (f21 - f20));
                        f18 = textureatlassprite1.getV(0.5F + (-f21 - f20));
                    }

                    int k5 = this.getLightColor(p_234370_, p_234371_);
                    int l5 = k5;
                    int i6 = l5;
                    int j6 = l5;
                    int j2 = l5;
                    int k2 = l5;
                    if (flag1) {
                        BlockPos blockpos = p_234371_.north();
                        BlockPos blockpos1 = p_234371_.south();
                        BlockPos blockpos2 = p_234371_.east();
                        BlockPos blockpos3 = p_234371_.west();
                        int l2 = this.getLightColor(p_234370_, blockpos);
                        int i3 = this.getLightColor(p_234370_, blockpos1);
                        int j3 = this.getLightColor(p_234370_, blockpos2);
                        int k3 = this.getLightColor(p_234370_, blockpos3);
                        int l3 = this.getLightColor(p_234370_, blockpos.west());
                        int i4 = this.getLightColor(p_234370_, blockpos1.west());
                        int j4 = this.getLightColor(p_234370_, blockpos1.east());
                        int k4 = this.getLightColor(p_234370_, blockpos.east());
                        i6 = ModelBlockRenderer.AmbientOcclusionRenderStorage.blend(l2, l3, k3, l5);
                        j6 = ModelBlockRenderer.AmbientOcclusionRenderStorage.blend(i3, i4, k3, l5);
                        j2 = ModelBlockRenderer.AmbientOcclusionRenderStorage.blend(i3, j4, j3, l5);
                        k2 = ModelBlockRenderer.AmbientOcclusionRenderStorage.blend(l2, k4, j3, l5);
                    }

                    float f50 = f5 * f1;
                    float f52 = f5 * f2;
                    float f54 = f5 * f3;
                    this.vertexVanilla(p_234372_, f23 + 0.0F, f24 + f10, f25 + 0.0F, f50, f52, f54, f29, f38, i6, f);
                    this.vertexVanilla(p_234372_, f23 + 0.0F, f24 + f12, f25 + 1.0F, f50, f52, f54, f31, f41, j6, f);
                    this.vertexVanilla(p_234372_, f23 + 1.0F, f24 + f11, f25 + 1.0F, f50, f52, f54, f33, f17, j2, f);
                    this.vertexVanilla(p_234372_, f23 + 1.0F, f24 + f9, f25 + 0.0F, f50, f52, f54, f36, f18, k2, f);
                    if (p_234374_.shouldRenderBackwardUpFace(p_234370_, p_234371_.above())) {
                        this.vertexVanilla(p_234372_, f23 + 0.0F, f24 + f10, f25 + 0.0F, f50, f52, f54, f29, f38, i6, f);
                        this.vertexVanilla(p_234372_, f23 + 1.0F, f24 + f9, f25 + 0.0F, f50, f52, f54, f36, f18, k2, f);
                        this.vertexVanilla(p_234372_, f23 + 1.0F, f24 + f11, f25 + 1.0F, f50, f52, f54, f33, f17, j2, f);
                        this.vertexVanilla(p_234372_, f23 + 0.0F, f24 + f12, f25 + 1.0F, f50, f52, f54, f31, f41, j6, f);
                    }
                }

                if (flag3) {
                    p_234372_.setSprite(textureatlassprite);
                    float f28 = textureatlassprite.getU0();
                    float f30 = textureatlassprite.getU1();
                    float f32 = textureatlassprite.getV0();
                    float f34 = textureatlassprite.getV1();
                    int j5 = this.getLightColor(p_234370_, p_234371_.below());
                    float f39 = p_234370_.getShade(Direction.DOWN, true);
                    float f42 = f39 * f1;
                    float f44 = f39 * f2;
                    float f46 = f39 * f3;
                    this.vertexVanilla(p_234372_, f23, f24 + f27, f25 + 1.0F, f42, f44, f46, f28, f34, j5, f);
                    this.vertexVanilla(p_234372_, f23, f24 + f27, f25, f42, f44, f46, f28, f32, j5, f);
                    this.vertexVanilla(p_234372_, f23 + 1.0F, f24 + f27, f25, f42, f44, f46, f30, f32, j5, f);
                    this.vertexVanilla(p_234372_, f23 + 1.0F, f24 + f27, f25 + 1.0F, f42, f44, f46, f30, f34, j5, f);
                }

                int i5 = this.getLightColor(p_234370_, p_234371_);

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    float f35;
                    float f37;
                    float f40;
                    float f43;
                    float f45;
                    float f47;
                    boolean flag8;
                    switch (direction) {
                        case NORTH:
                            f35 = f10;
                            f37 = f9;
                            f40 = f23;
                            f45 = f23 + 1.0F;
                            f43 = f25 + 0.001F;
                            f47 = f25 + 0.001F;
                            flag8 = flag4;
                            break;
                        case SOUTH:
                            f35 = f11;
                            f37 = f12;
                            f40 = f23 + 1.0F;
                            f45 = f23;
                            f43 = f25 + 1.0F - 0.001F;
                            f47 = f25 + 1.0F - 0.001F;
                            flag8 = flag5;
                            break;
                        case WEST:
                            f35 = f12;
                            f37 = f10;
                            f40 = f23 + 0.001F;
                            f45 = f23 + 0.001F;
                            f43 = f25 + 1.0F;
                            f47 = f25;
                            flag8 = flag6;
                            break;
                        default:
                            f35 = f9;
                            f37 = f11;
                            f40 = f23 + 1.0F - 0.001F;
                            f45 = f23 + 1.0F - 0.001F;
                            f43 = f25;
                            f47 = f25 + 1.0F;
                            flag8 = flag7;
                    }

                    if (flag8 && !isFaceOccludedByNeighbor(direction, Math.max(f35, f37), p_234370_.getBlockState(p_234371_.relative(direction)))) {
                        BlockPos blockpos4 = p_234371_.relative(direction);
                        TextureAtlasSprite textureatlassprite2 = textureatlassprite1;
                        float f48 = 0.0F;
                        float f49 = 0.0F;
                        boolean flag9 = !flag;
                        if (Reflector.IForgeBlockState_shouldDisplayFluidOverlay.exists()) {
                            flag9 = atextureatlassprite.length > 2 && atextureatlassprite[2] != null;
                        }

                        if (flag9) {
                            BlockState blockstate7 = p_234370_.getBlockState(blockpos4);
                            Block block = blockstate7.getBlock();
                            boolean flag10 = false;
                            if (Reflector.IForgeBlockState_shouldDisplayFluidOverlay.exists()) {
                                flag10 = Reflector.callBoolean(
                                    blockstate7, Reflector.IForgeBlockState_shouldDisplayFluidOverlay, p_234370_, blockpos4, p_234374_
                                );
                            }

                            if (flag10 || block instanceof HalfTransparentBlock || block instanceof LeavesBlock || block == Blocks.BEACON) {
                                textureatlassprite2 = this.waterOverlay;
                            }

                            if (block == Blocks.FARMLAND || block == Blocks.DIRT_PATH) {
                                f48 = 0.9375F;
                                f49 = 0.9375F;
                            }

                            if (block instanceof SlabBlock slabblock && blockstate7.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                                f48 = 0.5F;
                                f49 = 0.5F;
                            }
                        }

                        p_234372_.setSprite(textureatlassprite2);
                        if (!(f35 <= f48) || !(f37 <= f49)) {
                            f48 = Math.min(f48, f35);
                            f49 = Math.min(f49, f37);
                            if (f48 > f26) {
                                f48 -= f26;
                            }

                            if (f49 > f26) {
                                f49 -= f26;
                            }

                            float f51 = textureatlassprite2.getV((1.0F - f48) * 0.5F);
                            float f53 = textureatlassprite2.getV((1.0F - f49) * 0.5F);
                            float f55 = textureatlassprite2.getU(0.0F);
                            float f56 = textureatlassprite2.getU(0.5F);
                            float f57 = textureatlassprite2.getV((1.0F - f35) * 0.5F);
                            float f58 = textureatlassprite2.getV((1.0F - f37) * 0.5F);
                            float f59 = textureatlassprite2.getV(0.5F);
                            float f60 = direction.getAxis() == Direction.Axis.Z ? f6 : f7;
                            float f61 = direction != Direction.NORTH && direction != Direction.SOUTH
                                ? p_234370_.getShade(Direction.WEST, true)
                                : p_234370_.getShade(Direction.NORTH, true);
                            float f62 = f5 * f61 * f1;
                            float f63 = f5 * f61 * f2;
                            float f64 = f5 * f61 * f3;
                            this.vertexVanilla(p_234372_, f40, f24 + f35, f43, f62, f63, f64, f55, f57, i5, f);
                            this.vertexVanilla(p_234372_, f45, f24 + f37, f47, f62, f63, f64, f56, f58, i5, f);
                            this.vertexVanilla(p_234372_, f45, f24 + f27, f47, f62, f63, f64, f56, f59, i5, f);
                            this.vertexVanilla(p_234372_, f40, f24 + f27, f43, f62, f63, f64, f55, f59, i5, f);
                            if (textureatlassprite2 != this.waterOverlay) {
                                this.vertexVanilla(p_234372_, f40, f24 + f27, f43, f62, f63, f64, f55, f59, i5, f);
                                this.vertexVanilla(p_234372_, f45, f24 + f27, f47, f62, f63, f64, f56, f59, i5, f);
                                this.vertexVanilla(p_234372_, f45, f24 + f37, f47, f62, f63, f64, f56, f58, i5, f);
                                this.vertexVanilla(p_234372_, f40, f24 + f35, f43, f62, f63, f64, f55, f57, i5, f);
                            }
                        }
                    }
                }

                p_234372_.setSprite(null);
            }
        } finally {
            if (Config.isShaders()) {
                SVertexBuilder.popEntity(p_234372_);
            }
        }
    }

    private float calculateAverageHeight(BlockAndTintGetter p_203150_, Fluid p_203151_, float p_203152_, float p_203153_, float p_203154_, BlockPos p_203155_) {
        if (!(p_203154_ >= 1.0F) && !(p_203153_ >= 1.0F)) {
            float[] afloat = new float[2];
            if (p_203154_ > 0.0F || p_203153_ > 0.0F) {
                float f = this.getHeight(p_203150_, p_203151_, p_203155_);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                this.addWeightedHeight(afloat, f);
            }

            this.addWeightedHeight(afloat, p_203152_);
            this.addWeightedHeight(afloat, p_203154_);
            this.addWeightedHeight(afloat, p_203153_);
            return afloat[0] / afloat[1];
        } else {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] p_203189_, float p_203190_) {
        if (p_203190_ >= 0.8F) {
            p_203189_[0] += p_203190_ * 10.0F;
            p_203189_[1] += 10.0F;
        } else if (p_203190_ >= 0.0F) {
            p_203189_[0] += p_203190_;
            p_203189_[1]++;
        }
    }

    private float getHeight(BlockAndTintGetter p_203157_, Fluid p_203158_, BlockPos p_203159_) {
        BlockState blockstate = p_203157_.getBlockState(p_203159_);
        return this.getHeight(p_203157_, p_203158_, p_203159_, blockstate, blockstate.getFluidState());
    }

    private float getHeight(BlockAndTintGetter p_203161_, Fluid p_203162_, BlockPos p_203163_, BlockState p_203164_, FluidState p_203165_) {
        if (p_203162_.isSame(p_203165_.getType())) {
            BlockState blockstate = p_203161_.getBlockState(p_203163_.above());
            return p_203162_.isSame(blockstate.getFluidState().getType()) ? 1.0F : p_203165_.getOwnHeight();
        } else {
            return !p_203164_.isSolid() ? 0.0F : -1.0F;
        }
    }

    private void vertex(
        VertexConsumer p_110985_,
        float p_110989_,
        float p_110990_,
        float p_110991_,
        float p_110992_,
        float p_110993_,
        float p_343128_,
        float p_344448_,
        float p_344284_,
        int p_110994_
    ) {
        p_110985_.addVertex(p_110989_, p_110990_, p_110991_)
            .setColor(p_110992_, p_110993_, p_343128_, 1.0F)
            .setUv(p_344448_, p_344284_)
            .setLight(p_110994_)
            .setNormal(0.0F, 1.0F, 0.0F);
    }

    private void vertexVanilla(
        VertexConsumer buffer, float x, float y, float z, float red, float green, float blue, float u, float v, int combinedLight, float alpha
    ) {
        buffer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(combinedLight).setNormal(0.0F, 1.0F, 0.0F);
    }

    private int getLightColor(BlockAndTintGetter p_110946_, BlockPos p_110947_) {
        int i = LevelRenderer.getLightColor(p_110946_, p_110947_);
        int j = LevelRenderer.getLightColor(p_110946_, p_110947_.above());
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }
}
