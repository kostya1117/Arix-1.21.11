package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.optifine.Config;
import net.optifine.reflect.Reflector;
import net.optifine.util.TextureUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SpriteLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Identifier location;
    private final int maxSupportedTextureSize;
    private TextureAtlas atlas;

    public SpriteLoader(Identifier p_454492_, int p_276121_) {
        this.location = p_454492_;
        this.maxSupportedTextureSize = p_276121_;
    }

    public static SpriteLoader create(TextureAtlas p_249085_) {
        SpriteLoader spriteloader = new SpriteLoader(p_249085_.location(), p_249085_.maxSupportedTextureSize());
        spriteloader.atlas = p_249085_;
        return spriteloader;
    }

    private SpriteLoader.Preparations stitch(List<SpriteContents> p_262029_, int p_261919_, Executor p_261665_) {
        int i = this.atlas.mipmapLevel;
        int j = this.atlas.getIconGridSize();

        try (Zone zone = Profiler.get().zone(() -> "stitch " + this.location)) {
            int k = this.maxSupportedTextureSize;
            int l = Integer.MAX_VALUE;
            int i1 = 1 << p_261919_;

            for (SpriteContents spritecontents : p_262029_) {
                int j1 = spritecontents.getSpriteWidth();
                int k1 = spritecontents.getSpriteHeight();
                if (j1 >= 1 && k1 >= 1) {
                    if (j1 < j || i > 0) {
                        int l1 = i > 0 ? TextureUtils.scaleToGrid(j1, j) : TextureUtils.scaleToMin(j1, j);
                        if (l1 != j1) {
                            if (!TextureUtils.isPowerOfTwo(j1)) {
                                Config.log("Scaled non power of 2: " + spritecontents.getSpriteLocation() + ", " + j1 + " -> " + l1);
                            } else {
                                Config.log("Scaled too small texture: " + spritecontents.getSpriteLocation() + ", " + j1 + " -> " + l1);
                            }

                            int i2 = k1 * l1 / j1;
                            double d0 = l1 * 1.0 / j1;
                            spritecontents.setSpriteWidth(l1);
                            spritecontents.setSpriteHeight(i2);
                            spritecontents.setScaleFactor(d0);
                            spritecontents.rescale();
                        }
                    }

                    l = Math.min(l, Math.min(spritecontents.width(), spritecontents.height()));
                    int i3 = Math.min(Integer.lowestOneBit(spritecontents.width()), Integer.lowestOneBit(spritecontents.height()));
                    if (i3 < i1) {
                        LOGGER.warn(
                            "Texture {} with size {}x{} limits mip level from {} to {}",
                            spritecontents.name(),
                            spritecontents.width(),
                            spritecontents.height(),
                            Mth.log2(i1),
                            Mth.log2(i3)
                        );
                        i1 = i3;
                    }
                } else {
                    Config.warn("Invalid sprite size: " + spritecontents.getSpriteLocation());
                }
            }

            int j2 = Math.min(l, i1);
            int k2 = Mth.log2(j2);
            if (k2 < 0) {
                k2 = 0;
            }

            int l2;
            if (k2 < p_261919_) {
                LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.location, p_261919_, k2, j2);
                l2 = k2;
            } else {
                l2 = p_261919_;
            }

            Options options = Minecraft.getInstance().options;
            int j3 = l2 != 0 && options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyBit().get() : 0;
            Stitcher<SpriteContents> stitcher = new Stitcher<>(k, k, l2, j3);

            for (SpriteContents spritecontents1 : p_262029_) {
                stitcher.registerSprite(spritecontents1);
            }

            try {
                stitcher.stitch();
            } catch (StitcherException stitcherexception) {
                CrashReport crashreport = CrashReport.forThrowable(stitcherexception, "Stitching");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Stitcher");
                crashreportcategory.setDetail(
                    "Sprites",
                    stitcherexception.getAllSprites()
                        .stream()
                        .map(entry2In -> String.format(Locale.ROOT, "%s[%dx%d]", entry2In.name(), entry2In.width(), entry2In.height()))
                        .collect(Collectors.joining(","))
                );
                crashreportcategory.setDetail("Max Texture Size", k);
                throw new ReportedException(crashreport);
            }

            int k3 = stitcher.getWidth();
            int l3 = stitcher.getHeight();
            Map<Identifier, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, k3, l3);
            TextureAtlasSprite textureatlassprite = map.get(MissingTextureAtlasSprite.getLocation());
            CompletableFuture<Void> completablefuture = CompletableFuture.runAsync(() -> map.values().forEach(spriteIn -> {
                spriteIn.setTextureAtlas(this.atlas);
                spriteIn.increaseMipLevel(l2);
            }), p_261665_);
            return new SpriteLoader.Preparations(k3, l3, l2, textureatlassprite, map, completablefuture);
        }
    }

    private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(SpriteResourceLoader p_297457_, List<SpriteSource.Loader> p_261516_, Executor p_261791_) {
        List<CompletableFuture<SpriteContents>> list = p_261516_.stream()
            .map(functionIn -> CompletableFuture.supplyAsync(() -> functionIn.get(p_297457_), p_261791_))
            .toList();
        return Util.sequence(list).thenApply(listSpritesIn -> listSpritesIn.stream().filter(Objects::nonNull).toList());
    }

    public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
        ResourceManager p_262108_, Identifier p_459205_, int p_262104_, Executor p_261687_, Set<MetadataSectionType<?>> p_430900_
    ) {
        if (Reflector.ForgeHooksClient_getAtlastMetadataSections.exists()) {
            p_430900_ = (Set<MetadataSectionType<?>>)Reflector.ForgeHooksClient_getAtlastMetadataSections.call(p_459205_, p_430900_);
        }

        SpriteResourceLoader spriteresourceloader = SpriteResourceLoader.create(p_430900_);
        return CompletableFuture.<List<SpriteSource.Loader>>supplyAsync(() -> {
                SpriteSourceList spritesourcelist = SpriteSourceList.load(p_262108_, p_459205_);
                Set<Identifier> set = spritesourcelist.getSpriteNames(p_262108_);
                spritesourcelist.filterSpriteNames(set);
                Set<Identifier> set1 = new LinkedHashSet<>(set);
                this.atlas.preStitch(set1, p_262108_, p_262104_);
                set1.removeAll(set);
                spritesourcelist.addSpriteSources(set1);
                return spritesourcelist.list(p_262108_);
            }, p_261687_)
            .thenCompose(functionsIn -> runSpriteSuppliers(spriteresourceloader, (List<SpriteSource.Loader>)functionsIn, p_261687_))
            .thenApply(contentsIn -> this.stitch((List<SpriteContents>)contentsIn, p_262104_, p_261687_));
    }

    private Map<Identifier, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> p_276117_, int p_276111_, int p_276112_) {
        Map<Identifier, TextureAtlasSprite> map = new HashMap<>();
        p_276117_.gatherSprites(
            (contents2In, x2In, y2In, padding2In) -> {
                if (Reflector.ForgeHooksClient_loadTextureAtlasSprite.exists()) {
                    TextureAtlasSprite textureatlassprite = (TextureAtlasSprite)Reflector.ForgeHooksClient_loadTextureAtlasSprite
                        .call(this.location, contents2In, p_276111_, p_276112_, x2In, y2In, padding2In, contents2In.byMipLevel.length - 1);
                    if (textureatlassprite != null) {
                        map.put(contents2In.name(), textureatlassprite);
                        return;
                    }
                }

                TextureAtlasSprite textureatlassprite1 = this.atlas.getRegisteredSprite(contents2In.name());
                if (textureatlassprite1 != null) {
                    textureatlassprite1.init(this.location, contents2In, p_276111_, p_276112_, x2In, y2In, padding2In);
                } else {
                    textureatlassprite1 = new TextureAtlasSprite(this.location, contents2In, p_276111_, p_276112_, x2In, y2In, padding2In, this.atlas, null);
                }

                textureatlassprite1.update(Config.getResourceManager());
                map.put(contents2In.name(), textureatlassprite1);
            }
        );
        return map;
    }

    public record Preparations(
        int width,
        int height,
        int mipLevel,
        TextureAtlasSprite missing,
        Map<Identifier, TextureAtlasSprite> regions,
        CompletableFuture<Void> readyForUpload
    ) {
        public @Nullable TextureAtlasSprite getSprite(Identifier p_459172_) {
            return this.regions.get(p_459172_);
        }
    }
}
