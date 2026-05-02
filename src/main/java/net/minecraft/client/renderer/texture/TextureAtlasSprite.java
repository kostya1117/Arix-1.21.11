package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.SpriteCoordinateExpander;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.optifine.Config;
import net.optifine.TextureProperties;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersTextureType;
import net.optifine.util.CounterInt;
import net.optifine.util.TextureUtils;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

public class TextureAtlasSprite implements AutoCloseable {
    private Identifier atlasLocation;
    private SpriteContents contents;
    private int x;
    private int y;
    private float u0;
    private float u1;
    private float v0;
    private float v1;
    private int padding;
    private int indexInMap = -1;
    public float baseU;
    public float baseV;
    public int sheetWidth;
    public int sheetHeight;
    private final Identifier name;
    private GpuTexture spriteTexture;
    public TextureAtlasSprite spriteSingle = null;
    public boolean isSpriteSingle = false;
    public static final String SUFFIX_SPRITE_SINGLE = ".sprite_single";
    public TextureAtlasSprite spriteNormal = null;
    public TextureAtlasSprite spriteSpecular = null;
    public ShadersTextureType spriteShadersType = null;
    public TextureAtlasSprite spriteEmissive = null;
    public boolean isSpriteEmissive = false;
    protected int animationIndex = -1;
    private boolean terrain;
    private boolean shaders;
    private boolean multiTexture;
    private ResourceManager resourceManager;
    private int imageWidth;
    private int imageHeight;
    private TextureAtlas atlasTexture;
    private SpriteContents.AnimationState spriteContentsTicker;
    protected TextureAtlasSprite parentSprite;
    protected boolean usesParentAnimationTime = false;

    public TextureAtlasSprite(Identifier atlasLocation, Identifier name) {
        this.atlasLocation = atlasLocation;
        this.name = name;
        this.contents = null;
        this.atlasTexture = null;
        this.x = 0;
        this.y = 0;
        this.u0 = 0.0F;
        this.u1 = 0.0F;
        this.v0 = 0.0F;
        this.v1 = 0.0F;
        this.imageWidth = 0;
        this.imageHeight = 0;
    }

    private TextureAtlasSprite(TextureAtlasSprite parent) {
        this.atlasTexture = parent.atlasTexture;
        this.name = parent.getName();
        SpriteContents spritecontents = parent.contents;
        this.contents = new SpriteContents(
            spritecontents.name(),
            new FrameSize(spritecontents.width, spritecontents.height),
            spritecontents.getOriginalImage(),
            spritecontents.getAnimMetadata(),
            spritecontents.getAdditionalMetadata(),
            spritecontents.getTextureMetadata()
        );
        this.contents.setSprite(this);
        this.contents.setScaleFactor(spritecontents.getScaleFactor());
        this.imageWidth = parent.imageWidth;
        this.imageHeight = parent.imageHeight;
        this.usesParentAnimationTime = true;
        this.x = 0;
        this.y = 0;
        this.u0 = 0.0F;
        this.u1 = 1.0F;
        this.v0 = 0.0F;
        this.v1 = 1.0F;
        this.baseU = Math.min(this.u0, this.u1);
        this.baseV = Math.min(this.v0, this.v1);
        this.indexInMap = parent.indexInMap;
        this.baseU = parent.baseU;
        this.baseV = parent.baseV;
        this.sheetWidth = parent.sheetWidth;
        this.sheetHeight = parent.sheetHeight;
        this.isSpriteSingle = true;
        this.animationIndex = parent.animationIndex;
        if (this.spriteContentsTicker != null && parent.spriteContentsTicker != null) {
            this.spriteContentsTicker.animationActive = parent.spriteContentsTicker.animationActive;
        }
    }

    public void init(Identifier locationIn, SpriteContents contentsIn, int atlasWidthIn, int atlasHeightIn, int xIn, int yIn, int paddingIn) {
        this.atlasLocation = locationIn;
        this.contents = contentsIn;
        this.contents.setSprite(this);
        this.sheetWidth = atlasWidthIn;
        this.sheetHeight = atlasHeightIn;
        this.imageWidth = this.contents.width;
        this.imageHeight = this.contents.height;
        this.padding = paddingIn;
        this.x = xIn;
        this.y = yIn;
        this.u0 = (float)(xIn + paddingIn) / atlasWidthIn;
        this.u1 = (float)(xIn + paddingIn + contentsIn.width()) / atlasWidthIn;
        this.v0 = (float)(yIn + paddingIn) / atlasHeightIn;
        this.v1 = (float)(yIn + paddingIn + contentsIn.height()) / atlasHeightIn;
        this.baseU = Math.min(this.u0, this.u1);
        this.baseV = Math.min(this.v0, this.v1);
    }

    protected TextureAtlasSprite(Identifier p_460544_, SpriteContents p_248526_, int p_248950_, int p_249741_, int p_248672_, int p_248637_, int p_452036_) {
        this(p_460544_, p_248526_, p_248950_, p_249741_, p_248672_, p_248637_, p_452036_, null, null);
    }

    protected TextureAtlasSprite(
        Identifier locationIn,
        SpriteContents contentsIn,
        int atlasWidthIn,
        int atlasHeightIn,
        int xIn,
        int yIn,
        int paddingIn,
        TextureAtlas atlas,
        ShadersTextureType spriteShadersTypeIn
    ) {
        this.atlasTexture = atlas;
        this.spriteShadersType = spriteShadersTypeIn;
        this.atlasLocation = locationIn;
        this.contents = contentsIn;
        this.padding = paddingIn;
        this.x = xIn;
        this.y = yIn;
        this.u0 = (float)(xIn + paddingIn) / atlasWidthIn;
        this.u1 = (float)(xIn + paddingIn + contentsIn.width()) / atlasWidthIn;
        this.v0 = (float)(yIn + paddingIn) / atlasHeightIn;
        this.v1 = (float)(yIn + paddingIn + contentsIn.height()) / atlasHeightIn;
        this.name = contentsIn.name();
        this.imageWidth = this.contents.width;
        this.imageHeight = this.contents.height;
        this.baseU = Math.min(this.u0, this.u1);
        this.baseV = Math.min(this.v0, this.v1);
        this.sheetWidth = atlasWidthIn;
        this.sheetHeight = atlasHeightIn;
        this.contents.setSprite(this);
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public float getU0() {
        return this.u0;
    }

    public float getU1() {
        return this.u1;
    }

    public SpriteContents contents() {
        return this.contents;
    }

    public SpriteContents.@Nullable AnimationState createAnimationState(GpuBufferSlice p_455189_, int p_458866_) {
        SpriteContents.AnimationState spritecontents$animationstate = this.contents.createAnimationState(p_455189_, p_458866_);
        if (spritecontents$animationstate != null) {
            spritecontents$animationstate.setSprite(this);
        }

        return spritecontents$animationstate;
    }

    public float getU(float p_298825_) {
        float f = this.u1 - this.u0;
        return this.u0 + f * p_298825_;
    }

    public float getV0() {
        return this.v0;
    }

    public float getV1() {
        return this.v1;
    }

    public float getV(float p_299087_) {
        float f = this.v1 - this.v0;
        return this.v0 + f * p_299087_;
    }

    public Identifier atlasLocation() {
        return this.atlasLocation;
    }

    @Override
    public String toString() {
        return "TextureAtlasSprite{name="
            + this.name
            + ", contents='"
            + this.contents
            + "', u0="
            + this.u0
            + ", u1="
            + this.u1
            + ", v0="
            + this.v0
            + ", v1="
            + this.v1
            + "}";
    }

    public void uploadFirstFrame(GpuTexture p_397186_, int p_460430_) {
        this.contents.uploadFirstFrame(p_397186_, p_460430_);
    }

    public VertexConsumer wrap(VertexConsumer p_118382_) {
        return new SpriteCoordinateExpander(p_118382_, this);
    }

    boolean isAnimated() {
        return this.contents.isAnimated();
    }

    public void uploadSpriteUbo(ByteBuffer p_450246_, int p_456272_, int p_452106_, int p_453297_, int p_452181_, int p_457141_) {
        for (int i = 0; i <= p_452106_; i++) {
            Std140Builder.intoBuffer(MemoryUtil.memSlice(p_450246_, p_456272_ + i * p_457141_, p_457141_))
                .putMat4f(new Matrix4f().ortho2D(0.0F, p_453297_ >> i, 0.0F, p_452181_ >> i))
                .putMat4f(
                    new Matrix4f()
                        .translate(this.x >> i, this.y >> i, 0.0F)
                        .scale(this.contents.width() + this.padding * 2 >> i, this.contents.height() + this.padding * 2 >> i, 1.0F)
                )
                .putFloat((float)this.padding / this.contents.width())
                .putFloat((float)this.padding / this.contents.height())
                .putInt(i);
        }
    }

    public int getIndexInMap() {
        return this.indexInMap;
    }

    public void updateIndexInMap(CounterInt counterInt) {
        if (this.indexInMap < 0) {
            if (this.atlasTexture != null) {
                TextureAtlasSprite textureatlassprite = this.atlasTexture.getRegisteredSprite(this.getName());
                if (textureatlassprite != null) {
                    this.indexInMap = textureatlassprite.getIndexInMap();
                }
            }

            if (this.indexInMap < 0) {
                this.indexInMap = counterInt.nextValue();
            }
        }
    }

    public int getAnimationIndex() {
        return this.animationIndex;
    }

    public void setAnimationIndex(int animationIndex) {
        this.animationIndex = animationIndex;
        if (this.spriteSingle != null) {
            this.spriteSingle.setAnimationIndex(animationIndex);
        }

        if (this.spriteNormal != null) {
            this.spriteNormal.setAnimationIndex(animationIndex);
        }

        if (this.spriteSpecular != null) {
            this.spriteSpecular.setAnimationIndex(animationIndex);
        }
    }

    public boolean isAnimationActive() {
        return this.spriteContentsTicker == null ? false : this.spriteContentsTicker.animationActive;
    }

    public static void fixTransparentColor(NativeImage ni) {
        int[] aint = new int[ni.getWidth() * ni.getHeight()];
        ni.getBufferRGBA().get(aint);
        fixTransparentColor(aint);
        ni.getBufferRGBA().put(aint);
    }

    private static void fixTransparentColor(int[] data) {
        if (data != null) {
            int i = TextureProperties.getAlphaCutout();
            long j = 0L;
            long k = 0L;
            long l = 0L;
            long i1 = 0L;

            for (int j1 = 0; j1 < data.length; j1++) {
                int k1 = data[j1];
                int l1 = k1 >> 24 & 0xFF;
                if (l1 >= i) {
                    int i2 = k1 >> 16 & 0xFF;
                    int j2 = k1 >> 8 & 0xFF;
                    int k2 = k1 & 0xFF;
                    j += i2;
                    k += j2;
                    l += k2;
                    i1++;
                }
            }

            if (i1 > 0L) {
                int i3 = (int)(j / i1);
                int j3 = (int)(k / i1);
                int k3 = (int)(l / i1);
                int l3 = i3 << 16 | j3 << 8 | k3;

                for (int i4 = 0; i4 < data.length; i4++) {
                    int j4 = data[i4];
                    int l2 = j4 >> 24 & 0xFF;
                    if (l2 <= 16) {
                        data[i4] = l3;
                    }
                }
            }
        }
    }

    public void bindSpriteTexture() {
        TextureUtils.bindTexture(this.getSpriteTextureId());
    }

    public int getSpriteTextureId() {
        return this.getSpriteTexture().getGlTextureId();
    }

    public GpuTexture getSpriteTexture() {
        if (this.spriteTexture == null) {
            if (this.isSpriteSingle) {
                throw new IllegalArgumentException("Not allowed in spriteSingle");
            }

            int i = this.getMipmapLevels();
            this.spriteTexture = RenderSystem.getDevice()
                .createTexture(this.name::toString, 7, TextureFormat.RGBA8, this.getWidth(), this.getHeight(), 1, i + 1);
            NativeImage.setMinMagFilters(false, i > 0);
            NativeImage.setClamp(true);
            boolean flag = this.atlasTexture.isTextureBlend(this.spriteShadersType);
            if (flag) {
                TextureUtils.applyAnisotropicLevel();
            } else {
                GlStateManager.texParameter(3553, 34046, 1.0F);
                int j = i > 0 ? 9984 : 9728;
                GlStateManager._texParameter(3553, 10241, j);
                GlStateManager._texParameter(3553, 10240, 9728);
            }
        }

        return this.spriteTexture;
    }

    public void deleteSpriteTexture() {
        if (this.spriteTexture != null) {
            this.spriteTexture.close();
            this.spriteTexture = null;
        }
    }

    public float toSingleU(float u) {
        u -= this.baseU;
        float f = (float)this.sheetWidth / this.getWidth();
        return u * f;
    }

    public float toSingleV(float v) {
        v -= this.baseV;
        float f = (float)this.sheetHeight / this.getHeight();
        return v * f;
    }

    public NativeImage[] getMipmapImages() {
        return this.contents.byMipLevel;
    }

    public int getMipmapLevels() {
        return this.contents.byMipLevel.length - 1;
    }

    public int getOriginX() {
        return this.x;
    }

    public int getOriginY() {
        return this.y;
    }

    public float getUnInterpolatedU16(float u) {
        float f = this.u1 - this.u0;
        return (u - this.u0) / f * 16.0F;
    }

    public float getUnInterpolatedV16(float v) {
        float f = this.v1 - this.v0;
        return (v - this.v0) / f * 16.0F;
    }

    public float getInterpolatedU16(double u16) {
        float f = this.u1 - this.u0;
        return this.u0 + f * (float)u16 / 16.0F;
    }

    public float getInterpolatedV16(double v16) {
        float f = this.v1 - this.v0;
        return this.v0 + f * (float)v16 / 16.0F;
    }

    public Identifier getName() {
        return this.name;
    }

    public TextureAtlas getTextureAtlas() {
        return this.atlasTexture;
    }

    public void setTextureAtlas(TextureAtlas atlas) {
        this.atlasTexture = atlas;
        if (this.spriteSingle != null) {
            this.spriteSingle.setTextureAtlas(atlas);
        }

        if (this.spriteNormal != null) {
            this.spriteNormal.setTextureAtlas(atlas);
        }

        if (this.spriteSpecular != null) {
            this.spriteSpecular.setTextureAtlas(atlas);
        }
    }

    public int getWidth() {
        return this.contents.getSpriteWidth();
    }

    public int getHeight() {
        return this.contents.getSpriteHeight();
    }

    public TextureAtlasSprite makeSpriteSingle() {
        TextureAtlasSprite textureatlassprite = new TextureAtlasSprite(this);
        textureatlassprite.isSpriteSingle = true;
        return textureatlassprite;
    }

    public TextureAtlasSprite makeSpriteShaders(ShadersTextureType type, int colDef, SpriteContents.AnimatedTexture parentAnimatedTexture) {
        String s = type.getSuffix();
        Identifier identifier = new Identifier(this.getName().getNamespace(), this.getName().getPath() + s);
        Identifier identifier1 = this.atlasTexture.getSpritePath(identifier);
        TextureAtlasSprite textureatlassprite = null;
        Optional<Resource> optional = this.resourceManager.getResource(identifier1);
        if (optional.isPresent()) {
            try {
                Resource resource = optional.get();
                NativeImage nativeimage = NativeImage.read(resource.open());
                ResourceMetadata resourcemetadata = resource.metadata();
                AnimationMetadataSection animationmetadatasection = resourcemetadata.getSection(AnimationMetadataSection.TYPE).orElse(null);
                FrameSize framesize = calculateFrameSize(animationmetadatasection, nativeimage.getWidth(), nativeimage.getHeight());
                if (nativeimage.getWidth() != this.getWidth()) {
                    NativeImage nativeimage1 = TextureUtils.scaleImage(nativeimage, this.getWidth());
                    if (nativeimage1 != nativeimage) {
                        double d0 = 1.0 * this.getWidth() / nativeimage.getWidth();
                        nativeimage.close();
                        nativeimage = nativeimage1;
                        framesize = new FrameSize((int)(framesize.width() * d0), (int)(framesize.height() * d0));
                    }
                }

                Optional<AnimationMetadataSection> optional1 = resourcemetadata.getSection(AnimationMetadataSection.TYPE);
                SpriteContents spritecontents1 = new SpriteContents(identifier, framesize, nativeimage, optional1, List.of(), Optional.empty());
                textureatlassprite = new TextureAtlasSprite(
                    this.atlasLocation, spritecontents1, this.sheetWidth, this.sheetHeight, this.x, this.y, this.padding, this.atlasTexture, type
                );
                textureatlassprite.parentSprite = this;
            } catch (IOException ioexception) {
            }
        }

        if (textureatlassprite == null) {
            NativeImage nativeimage2 = new NativeImage(this.getWidth(), this.getHeight(), false);
            nativeimage2.fillRect(0, 0, nativeimage2.getWidth(), nativeimage2.getHeight(), colDef);
            SpriteContents spritecontents = new SpriteContents(identifier, new FrameSize(this.getWidth(), this.getHeight()), nativeimage2);
            textureatlassprite = new TextureAtlasSprite(
                this.atlasLocation, spritecontents, this.sheetWidth, this.sheetHeight, this.x, this.y, this.padding, this.atlasTexture, type
            );
        }

        if (this.terrain && this.multiTexture && !this.isSpriteSingle) {
            textureatlassprite.spriteSingle = textureatlassprite.makeSpriteSingle();
        }

        return textureatlassprite;
    }

    private static FrameSize calculateFrameSize(AnimationMetadataSection animMeta, int width, int height) {
        return animMeta != null ? animMeta.calculateFrameSize(width, height) : new FrameSize(width, height);
    }

    public boolean isTerrain() {
        return this.terrain;
    }

    private void setTerrain(boolean terrainIn) {
        this.terrain = terrainIn;
        this.multiTexture = false;
        this.shaders = false;
        if (this.spriteSingle != null) {
            this.deleteSpriteTexture();
            this.spriteSingle = null;
        }

        if (this.spriteNormal != null) {
            if (this.spriteNormal.spriteSingle != null) {
                this.spriteNormal.deleteSpriteTexture();
            }

            this.spriteNormal.contents().close();
            this.spriteNormal = null;
        }

        if (this.spriteSpecular != null) {
            if (this.spriteSpecular.spriteSingle != null) {
                this.spriteSpecular.deleteSpriteTexture();
            }

            this.spriteSpecular.contents().close();
            this.spriteSpecular = null;
        }

        this.multiTexture = Config.isMultiTexture();
        this.shaders = Config.isShaders();
        if (this.terrain && this.multiTexture && !this.isSpriteSingle) {
            this.spriteSingle = this.makeSpriteSingle();
        }

        if (this.shaders && !this.isSpriteSingle) {
            if (this.spriteNormal == null && Shaders.configNormalMap) {
                this.spriteNormal = this.makeSpriteShaders(ShadersTextureType.NORMAL, -8421377, this.contents.getAnimatedTexture());
            }

            if (this.spriteSpecular == null && Shaders.configSpecularMap) {
                this.spriteSpecular = this.makeSpriteShaders(ShadersTextureType.SPECULAR, 0, this.contents.getAnimatedTexture());
            }
        }
    }

    private static boolean matchesTiming(SpriteContents.AnimatedTexture at1, SpriteContents.AnimatedTexture at2) {
        if (at1 == null || at2 == null) {
            return false;
        }

        if (at1 == at2) {
            return true;
        }

        boolean flag = at1.interpolateFrames;
        boolean flag1 = at2.interpolateFrames;
        if (flag != flag1) {
            return false;
        }

        List<SpriteContents.FrameInfo> list = at1.frames;
        List<SpriteContents.FrameInfo> list1 = at2.frames;
        if (list != null && list1 != null) {
            if (list.size() != list1.size()) {
                return false;
            }

            for (int i = 0; i < list.size(); i++) {
                SpriteContents.FrameInfo spritecontents$frameinfo = list.get(i);
                SpriteContents.FrameInfo spritecontents$frameinfo1 = list1.get(i);
                if (spritecontents$frameinfo == null || spritecontents$frameinfo1 == null) {
                    return false;
                }

                if (spritecontents$frameinfo.index() != spritecontents$frameinfo1.index()) {
                    return false;
                }

                if (spritecontents$frameinfo.time() != spritecontents$frameinfo1.time()) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    public void update(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.updateIndexInMap(this.atlasTexture.getCounterIndexInMap());
        this.setTerrain(this.atlasTexture.isTerrain());
    }

    public int getPixelRGBA(int frameIndex, int x, int y) {
        if (this.contents.getAnimatedTexture() != null) {
            x += this.contents.getAnimatedTexture().getFrameX(frameIndex) * this.contents.width;
            y += this.contents.getAnimatedTexture().getFrameY(frameIndex) * this.contents.height;
        }

        return this.contents.getOriginalImage().getPixel(x, y);
    }

    public SpriteContents.AnimationState getSpriteContentsTicker() {
        return this.spriteContentsTicker;
    }

    public void setSpriteContentsTicker(SpriteContents.AnimationState spriteContentsTicker) {
        if (this.spriteContentsTicker != null) {
            this.spriteContentsTicker.close();
        }

        this.spriteContentsTicker = spriteContentsTicker;
        if (this.spriteContentsTicker != null && this.parentSprite != null && this.parentSprite.contents != null) {
            this.usesParentAnimationTime = matchesTiming(this.contents.getAnimatedTexture(), this.parentSprite.contents.getAnimatedTexture());
        }
    }

    public void setTicker(SpriteContents.AnimationState ticker) {
        this.setSpriteContentsTicker(ticker);
    }

    public void increaseMipLevel(int mipLevelIn) {
        this.contents.increaseMipLevel(mipLevelIn);
        if (this.spriteNormal != null) {
            this.spriteNormal.increaseMipLevel(mipLevelIn);
        }

        if (this.spriteSpecular != null) {
            this.spriteSpecular.increaseMipLevel(mipLevelIn);
        }
    }

    public void uploadMipmaps(GpuTexture textureIn) {
        for (int i = 0; i < this.contents.byMipLevel.length; i++) {
            this.contents.uploadFirstFrame(textureIn, i);
        }
    }

    public TextureAtlasSprite getSubSprite(ShadersTextureType spriteType) {
        if (spriteType == ShadersTextureType.NORMAL) {
            return this.spriteNormal;
        } else {
            return spriteType == ShadersTextureType.SPECULAR ? this.spriteSpecular : this;
        }
    }

    public int getPadding() {
        return this.padding;
    }

    @Override
    public void close() {
        this.deleteSpriteTexture();
        if (this.spriteNormal != null) {
            this.spriteNormal.deleteSpriteTexture();
        }

        if (this.spriteSpecular != null) {
            this.spriteSpecular.deleteSpriteTexture();
        }

        this.contents.close();
    }
}
