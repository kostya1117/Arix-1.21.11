package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.optifine.Mipmaps;
import net.optifine.texture.IColorBlender;

public class MipmapGenerator {
    private static final String ITEM_PREFIX = "item/";
    private static final float ALPHA_CUTOFF = 0.5F;
    private static final float STRICT_ALPHA_CUTOFF = 0.3F;

    private MipmapGenerator() {
    }

    private static float alphaTestCoverage(NativeImage p_460701_, float p_459489_, float p_457323_) {
        int i = p_460701_.getWidth();
        int j = p_460701_.getHeight();
        float f = 0.0F;
        int k = 4;

        for (int l = 0; l < j - 1; l++) {
            for (int i1 = 0; i1 < i - 1; i1++) {
                float f1 = Math.clamp(ARGB.alphaFloat(p_460701_.getPixel(i1, l)) * p_457323_, 0.0F, 1.0F);
                float f2 = Math.clamp(ARGB.alphaFloat(p_460701_.getPixel(i1 + 1, l)) * p_457323_, 0.0F, 1.0F);
                float f3 = Math.clamp(ARGB.alphaFloat(p_460701_.getPixel(i1, l + 1)) * p_457323_, 0.0F, 1.0F);
                float f4 = Math.clamp(ARGB.alphaFloat(p_460701_.getPixel(i1 + 1, l + 1)) * p_457323_, 0.0F, 1.0F);
                float f5 = 0.0F;

                for (int j1 = 0; j1 < 4; j1++) {
                    float f6 = (j1 + 0.5F) / 4.0F;

                    for (int k1 = 0; k1 < 4; k1++) {
                        float f7 = (k1 + 0.5F) / 4.0F;
                        float f8 = f1 * (1.0F - f7) * (1.0F - f6) + f2 * f7 * (1.0F - f6) + f3 * (1.0F - f7) * f6 + f4 * f7 * f6;
                        if (f8 > p_459489_) {
                            f5++;
                        }
                    }
                }

                f += f5 / 16.0F;
            }
        }

        return f / ((i - 1) * (j - 1));
    }

    private static void scaleAlphaToCoverage(NativeImage p_457433_, float p_460973_, float p_451027_, float p_451309_) {
        float f = 0.0F;
        float f1 = 4.0F;
        float f2 = 1.0F;
        float f3 = 1.0F;
        float f4 = Float.MAX_VALUE;
        int i = p_457433_.getWidth();
        int j = p_457433_.getHeight();

        for (int k = 0; k < 5; k++) {
            float f5 = alphaTestCoverage(p_457433_, p_451027_, f2);
            float f6 = Math.abs(f5 - p_460973_);
            if (f6 < f4) {
                f4 = f6;
                f3 = f2;
            }

            if (f5 < p_460973_) {
                f = f2;
            } else {
                if (!(f5 > p_460973_)) {
                    break;
                }

                f1 = f2;
            }

            f2 = (f + f1) * 0.5F;
        }

        for (int l = 0; l < j; l++) {
            for (int i1 = 0; i1 < i; i1++) {
                int j1 = p_457433_.getPixel(i1, l);
                float f7 = ARGB.alphaFloat(j1);
                f7 = f7 * f3 + p_451309_ + 0.025F;
                f7 = Math.clamp(f7, 0.0F, 1.0F);
                p_457433_.setPixel(i1, l, ARGB.color(f7, j1));
            }
        }
    }

    public static NativeImage[] generateMipLevels(Identifier p_451926_, NativeImage[] p_251300_, int p_252326_, MipmapStrategy p_450366_, float p_460450_) {
        return generateMipLevels(p_451926_, p_251300_, p_252326_, p_450366_, p_460450_, null);
    }

    public static NativeImage[] generateMipLevels(
        Identifier locationIn, NativeImage[] imagesIn, int mipmapLevelsIn, MipmapStrategy strategyIn, float alphaCutoffBiasIn, IColorBlender colorBlender
    ) {
        if (strategyIn == MipmapStrategy.AUTO) {
            strategyIn = hasTransparentPixel(imagesIn[0]) ? MipmapStrategy.CUTOUT : MipmapStrategy.MEAN;
        }

        if (imagesIn.length == 1 && !locationIn.getPath().startsWith("item/") && colorBlender == null) {
            if (strategyIn == MipmapStrategy.CUTOUT || strategyIn == MipmapStrategy.STRICT_CUTOUT) {
                TextureAtlasSprite.fixTransparentColor(imagesIn[0]);
            } else if (strategyIn == MipmapStrategy.DARK_CUTOUT) {
                TextureUtil.fillEmptyAreasWithDarkColor(imagesIn[0]);
            }
        }

        if (mipmapLevelsIn + 1 <= imagesIn.length) {
            return imagesIn;
        }

        NativeImage[] anativeimage = new NativeImage[mipmapLevelsIn + 1];
        anativeimage[0] = imagesIn[0];
        boolean flag = strategyIn == MipmapStrategy.CUTOUT || strategyIn == MipmapStrategy.STRICT_CUTOUT || strategyIn == MipmapStrategy.DARK_CUTOUT;
        float f = strategyIn == MipmapStrategy.STRICT_CUTOUT ? 0.3F : 0.5F;
        float f1 = flag ? alphaTestCoverage(imagesIn[0], f, 1.0F) : 0.0F;

        for (int i = 1; i <= mipmapLevelsIn; i++) {
            if (i < imagesIn.length) {
                anativeimage[i] = imagesIn[i];
            } else {
                NativeImage nativeimage = anativeimage[i - 1];
                int j = Math.max(nativeimage.getWidth() >> 1, 1);
                int k = Math.max(nativeimage.getHeight() >> 1, 1);
                NativeImage nativeimage1 = new NativeImage(j, k, false);
                int l = nativeimage1.getWidth();
                int i1 = nativeimage1.getHeight();

                for (int j1 = 0; j1 < l; j1++) {
                    for (int k1 = 0; k1 < i1; k1++) {
                        if (colorBlender != null) {
                            nativeimage1.setPixelABGR(
                                j1,
                                k1,
                                colorBlender.blend(
                                    nativeimage.getPixelABGR(j1 * 2 + 0, k1 * 2 + 0),
                                    nativeimage.getPixelABGR(j1 * 2 + 1, k1 * 2 + 0),
                                    nativeimage.getPixelABGR(j1 * 2 + 0, k1 * 2 + 1),
                                    nativeimage.getPixelABGR(j1 * 2 + 1, k1 * 2 + 1)
                                )
                            );
                        } else {
                            int l1 = nativeimage.getPixel(j1 * 2 + 0, k1 * 2 + 0);
                            int i2 = nativeimage.getPixel(j1 * 2 + 1, k1 * 2 + 0);
                            int j2 = nativeimage.getPixel(j1 * 2 + 0, k1 * 2 + 1);
                            int k2 = nativeimage.getPixel(j1 * 2 + 1, k1 * 2 + 1);
                            int l2;
                            if (Boolean.TRUE) {
                                l2 = Mipmaps.alphaBlend(l1, i2, j2, k2);
                            } else if (strategyIn == MipmapStrategy.DARK_CUTOUT) {
                                l2 = darkenedAlphaBlend(l1, i2, j2, k2);
                            } else {
                                l2 = ARGB.meanLinear(l1, i2, j2, k2);
                            }

                            nativeimage1.setPixel(j1, k1, l2);
                        }
                    }
                }

                anativeimage[i] = nativeimage1;
            }

            if (flag && colorBlender == null) {
                scaleAlphaToCoverage(anativeimage[i], f1, f, alphaCutoffBiasIn);
            }
        }

        return anativeimage;
    }

    private static boolean hasTransparentPixel(NativeImage p_252279_) {
        for (int i = 0; i < p_252279_.getWidth(); i++) {
            for (int j = 0; j < p_252279_.getHeight(); j++) {
                if (ARGB.alpha(p_252279_.getPixel(i, j)) == 0) {
                    return true;
                }
            }
        }

        return false;
    }

    private static int darkenedAlphaBlend(int p_458163_, int p_456572_, int p_451104_, int p_459696_) {
        float f = 0.0F;
        float f1 = 0.0F;
        float f2 = 0.0F;
        float f3 = 0.0F;
        if (ARGB.alpha(p_458163_) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(p_458163_));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(p_458163_));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(p_458163_));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_458163_));
        }

        if (ARGB.alpha(p_456572_) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(p_456572_));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(p_456572_));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(p_456572_));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_456572_));
        }

        if (ARGB.alpha(p_451104_) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(p_451104_));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(p_451104_));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(p_451104_));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_451104_));
        }

        if (ARGB.alpha(p_459696_) != 0) {
            f += ARGB.srgbToLinearChannel(ARGB.alpha(p_459696_));
            f1 += ARGB.srgbToLinearChannel(ARGB.red(p_459696_));
            f2 += ARGB.srgbToLinearChannel(ARGB.green(p_459696_));
            f3 += ARGB.srgbToLinearChannel(ARGB.blue(p_459696_));
        }

        f /= 4.0F;
        f1 /= 4.0F;
        f2 /= 4.0F;
        f3 /= 4.0F;
        return ARGB.color(ARGB.linearToSrgbChannel(f), ARGB.linearToSrgbChannel(f1), ARGB.linearToSrgbChannel(f2), ARGB.linearToSrgbChannel(f3));
    }
}
