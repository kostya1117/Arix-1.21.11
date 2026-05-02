package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.SmartAnimations;
import net.optifine.TextureProperties;
import net.optifine.reflect.Reflector;
import net.optifine.shaders.ITextureFormat;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersTex;
import net.optifine.shaders.ShadersTextureType;
import net.optifine.texture.ColorBlenderLinear;
import net.optifine.texture.IColorBlender;
import net.optifine.util.CounterInt;
import net.optifine.util.TextureUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

public class TextureAtlas extends AbstractTexture implements Dumpable, TickableTexture {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Deprecated
    public static final Identifier LOCATION_BLOCKS = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
    @Deprecated
    public static final Identifier LOCATION_ITEMS = Identifier.withDefaultNamespace("textures/atlas/items.png");
    @Deprecated
    public static final Identifier LOCATION_PARTICLES = Identifier.withDefaultNamespace("textures/atlas/particles.png");
    private List<TextureAtlasSprite> sprites = List.of();
    private List<SpriteContents.AnimationState> animatedTexturesStates = List.of();
    private Map<Identifier, TextureAtlasSprite> texturesByName = Map.of();
    private  TextureAtlasSprite missingSprite;
    private final Identifier location;
    private final int maxSupportedTextureSize;
    private int width;
    private int height;
    private int maxMipLevel;
    private int mipLevelCount;
    private GpuTextureView[] mipViews = new GpuTextureView[0];
    private  GpuBuffer spriteUbos;
    private Map<Identifier, TextureAtlasSprite> mapRegisteredSprites = new LinkedHashMap<>();
    private Map<Identifier, TextureAtlasSprite> mapMissingSprites = new LinkedHashMap<>();
    private TextureAtlasSprite[] iconGrid = null;
    private int iconGridSize = -1;
    private int iconGridCountX = -1;
    private int iconGridCountY = -1;
    private double iconGridSizeU = -1.0;
    private double iconGridSizeV = -1.0;
    private CounterInt counterIndexInMap = new CounterInt(0);
    public int atlasWidth = 0;
    public int atlasHeight = 0;
    public int mipmapLevel = 0;
    private List<SpriteContents.AnimationState> activeAnimations = new ArrayList<>();
    private int countAnimationsActive;
    private int frameCountAnimations;
    private boolean terrain;
    private boolean items;
    private ITextureFormat textureFormat;
    private GpuTextureView[] mipViewsNormal = new GpuTextureView[0];
    private GpuTextureView[] mipViewsSpecular = new GpuTextureView[0];

    public TextureAtlas(Identifier p_458227_) {
        this.location = p_458227_;
        this.maxSupportedTextureSize = RenderSystem.getDevice().getMaxTextureSize();
        this.terrain = p_458227_.equals(LOCATION_BLOCKS);
        this.items = p_458227_.equals(LOCATION_ITEMS);
        if (this.terrain) {
            Config.setTextureMapBlocks(this);
        }

        if (this.items) {
            Config.setTextureMapItems(this);
        }
    }

    private void createTexture(int p_410800_, int p_410805_, int p_410791_) {
        LOGGER.info("Created: {}x{}x{} {}-atlas", p_410800_, p_410805_, p_410791_, this.location);
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.close();
        this.texture = gpudevice.createTexture(this.location::toString, 15, TextureFormat.RGBA8, p_410800_, p_410805_, 1, p_410791_ + 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        this.width = p_410800_;
        this.height = p_410805_;
        this.maxMipLevel = p_410791_;
        this.mipLevelCount = p_410791_ + 1;
        this.mipViews = new GpuTextureView[this.mipLevelCount];
        if (Config.isShaders()) {
            if (Shaders.configNormalMap) {
                this.mipViewsNormal = new GpuTextureView[this.mipLevelCount];
            }

            if (Shaders.configSpecularMap) {
                this.mipViewsSpecular = new GpuTextureView[this.mipLevelCount];
            }
        }

        for (int i = 0; i <= this.maxMipLevel; i++) {
            this.mipViews[i] = gpudevice.createTextureView(this.texture, i, 1);
            if (Config.isShaders()) {
                if (Shaders.configNormalMap) {
                    this.mipViewsNormal[i] = gpudevice.createTextureView(this.getMultiTexID().normTex, i, 1);
                }

                if (Shaders.configSpecularMap) {
                    this.mipViewsSpecular[i] = gpudevice.createTextureView(this.getMultiTexID().specTex, i, 1);
                }
            }
        }

        this.texture.setParentTexture(this);
    }

    public void upload(SpriteLoader.Preparations p_250662_) {
        this.createTexture(p_250662_.width(), p_250662_.height(), p_250662_.mipLevel());
        this.atlasWidth = p_250662_.width();
        this.atlasHeight = p_250662_.height();
        this.mipmapLevel = p_250662_.mipLevel();
        if (Config.isShaders()) {
            ShadersTex.allocateTextureMapNS(this.mipmapLevel, this.atlasWidth, this.atlasHeight, this);
        }

        this.clearTextureData();
        this.sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        this.texturesByName = Map.copyOf(p_250662_.regions());
        this.missingSprite = this.texturesByName.get(MissingTextureAtlasSprite.getLocation());
        if (this.missingSprite == null) {
            throw new IllegalStateException("Atlas '" + this.location + "' (" + this.texturesByName.size() + " sprites) has no missing texture sprite");
        }

        List<TextureAtlasSprite> list = new ArrayList<>();
        List<SpriteContents.AnimationState> list1 = new ArrayList<>();
        int i = (int)p_250662_.regions().values().stream().filter(TextureAtlasSprite::isAnimated).count();
        int j = Mth.roundToward(SpriteContents.UBO_SIZE, RenderSystem.getDevice().getUniformOffsetAlignment());
        int k = j * this.mipLevelCount;
        ByteBuffer bytebuffer = MemoryUtil.memAlloc(i * k);
        int l = 0;

        for (TextureAtlasSprite textureatlassprite : p_250662_.regions().values()) {
            textureatlassprite.setTextureAtlas(this);
            if (textureatlassprite.isAnimated()) {
                textureatlassprite.uploadSpriteUbo(bytebuffer, l * k, this.maxMipLevel, this.width, this.height, j);
                l++;
            }
        }

        GpuBuffer gpubuffer = l > 0 ? RenderSystem.getDevice().createBuffer(() -> this.location + " sprite UBOs", 128, bytebuffer) : null;
        l = 0;

        for (TextureAtlasSprite textureatlassprite1 : p_250662_.regions().values()) {
            list.add(textureatlassprite1);
            textureatlassprite1.setTextureAtlas(this);
            if (textureatlassprite1.isAnimated() && gpubuffer != null) {
                GpuBufferSlice gpubufferslice = gpubuffer.slice(l * k, k);
                SpriteContents.AnimationState spritecontents$animationstate = textureatlassprite1.createAnimationState(gpubufferslice, j);
                l++;
                if (spritecontents$animationstate != null) {
                    textureatlassprite1.setTicker(spritecontents$animationstate);
                    textureatlassprite1.setAnimationIndex(list1.size());
                    list1.add(spritecontents$animationstate);
                    TextureAtlasSprite textureatlassprite2 = textureatlassprite1.spriteNormal;
                    if (textureatlassprite2 != null) {
                        SpriteContents.AnimationState spritecontents$animationstate1 = textureatlassprite2.createAnimationState(gpubufferslice, j);
                        if (spritecontents$animationstate1 != null) {
                            textureatlassprite2.setTicker(spritecontents$animationstate1);
                            textureatlassprite2.setAnimationIndex(list1.size());
                        }
                    }

                    TextureAtlasSprite textureatlassprite4 = textureatlassprite1.spriteSpecular;
                    if (textureatlassprite4 != null) {
                        SpriteContents.AnimationState spritecontents$animationstate2 = textureatlassprite4.createAnimationState(gpubufferslice, j);
                        if (spritecontents$animationstate2 != null) {
                            textureatlassprite4.setTicker(spritecontents$animationstate2);
                            textureatlassprite4.setAnimationIndex(list1.size());
                        }
                    }
                }
            }
        }

        this.spriteUbos = gpubuffer;
        this.sprites = list;
        this.animatedTexturesStates = List.copyOf(list1);
        this.uploadInitialContents();
        TextureUtils.refreshCustomSprites(this);
        Config.log("Animated sprites: " + this.animatedTexturesStates.size());
        if (Config.isMultiTexture()) {
            for (TextureAtlasSprite textureatlassprite3 : p_250662_.regions().values()) {
                uploadMipmapsSingle(textureatlassprite3);
                if (textureatlassprite3.spriteNormal != null) {
                    uploadMipmapsSingle(textureatlassprite3.spriteNormal);
                }

                if (textureatlassprite3.spriteSpecular != null) {
                    uploadMipmapsSingle(textureatlassprite3.spriteSpecular);
                }
            }

            GlStateManager._bindTexture(this.getGlTextureId());
        }

        if (Config.isShaders()) {
            if (Shaders.configNormalMap) {
                this.uploadInitialContents(ShadersTextureType.NORMAL);
            }

            if (Shaders.configSpecularMap) {
                this.uploadInitialContents(ShadersTextureType.SPECULAR);
            }
        }

        Reflector.callVoid(Reflector.ForgeHooksClient_onTextureStitchedPost, this);
        this.updateIconGrid(this.atlasWidth, this.atlasHeight);
        if (Config.equals(System.getProperty("saveTextureMap"), "true")) {
            Config.dbg("Exporting texture map: " + this.location);
            TextureUtils.saveGlTexture(
                "debug/" + this.location.getPath().replaceAll("/", "_"), this.getGlTextureId(), this.mipmapLevel, this.atlasWidth, this.atlasHeight
            );
            if (Config.isShaders()) {
                if (Shaders.configNormalMap) {
                    TextureUtils.saveGlTexture(
                        "debug/" + this.location.getPath().replaceAll("/", "_").replace(".png", "_n.png"),
                        this.getMultiTexID().norm,
                        this.mipmapLevel,
                        this.atlasWidth,
                        this.atlasHeight
                    );
                }

                if (Shaders.configSpecularMap) {
                    TextureUtils.saveGlTexture(
                        "debug/" + this.location.getPath().replaceAll("/", "_").replace(".png", "_s.png"),
                        this.getMultiTexID().spec,
                        this.mipmapLevel,
                        this.atlasWidth,
                        this.atlasHeight
                    );
                }

                GlStateManager._bindTexture(this.getGlTextureId());
            }
        }

        if (SharedConstants.DEBUG_DUMP_TEXTURE_ATLAS) {
            Path path = TextureUtil.getDebugTexturePath();

            try {
                Files.createDirectories(path);
                this.dumpContents(this.location, path);
            } catch (Exception exception) {
                LOGGER.warn("Failed to dump atlas contents to {}", path);
            }
        }
    }

    private void uploadInitialContents() {
        this.uploadInitialContents(null);
    }

    private void uploadInitialContents(ShadersTextureType spriteType) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        int i = Mth.roundToward(SpriteContents.UBO_SIZE, RenderSystem.getDevice().getUniformOffsetAlignment());
        int j = i * this.mipLevelCount;
        GpuSampler gpusampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true);
        List<TextureAtlasSprite> list = this.sprites;
        if (spriteType != null) {
            list = this.sprites.stream().filter(sprite2In -> sprite2In.getSubSprite(spriteType) != null).toList();
        }

        List<GpuTextureView[]> list1 = new ArrayList<>();
        ByteBuffer bytebuffer = MemoryUtil.memAlloc(list.size() * j);

        for (int k = 0; k < list.size(); k++) {
            TextureAtlasSprite textureatlassprite = list.get(k).getSubSprite(spriteType);
            textureatlassprite.uploadSpriteUbo(bytebuffer, k * j, this.maxMipLevel, this.width, this.height, i);
            GpuTexture gputexture = gpudevice.createTexture(
                () -> textureatlassprite.contents().name().toString(),
                5,
                TextureFormat.RGBA8,
                textureatlassprite.contents().width(),
                textureatlassprite.contents().height(),
                1,
                this.mipLevelCount
            );
            GpuTextureView[] agputextureview = new GpuTextureView[this.mipLevelCount];

            for (int l = 0; l <= this.maxMipLevel; l++) {
                textureatlassprite.uploadFirstFrame(gputexture, l);
                agputextureview[l] = gpudevice.createTextureView(gputexture);
            }

            list1.add(agputextureview);
        }

        GpuTextureView[] agputextureview1 = this.getMipViews(spriteType);

        try (GpuBuffer gpubuffer = gpudevice.createBuffer(() -> "SpriteAnimationInfo", 128, bytebuffer)) {
            for (int i1 = 0; i1 < this.mipLevelCount; i1++) {
                try (RenderPass renderpass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Animate " + this.location, agputextureview1[i1], OptionalInt.empty())) {
                    renderpass.setPipeline(RenderPipelines.ANIMATE_SPRITE_BLIT);

                    for (int j1 = 0; j1 < list.size(); j1++) {
                        renderpass.bindTexture("Sprite", list1.get(j1)[i1], gpusampler);
                        renderpass.setUniform("SpriteAnimationInfo", gpubuffer.slice(j1 * j + i1 * i, SpriteContents.UBO_SIZE));
                        renderpass.draw(0, 6);
                    }
                }
            }
        }

        for (GpuTextureView[] agputextureview2 : list1) {
            for (GpuTextureView gputextureview : agputextureview2) {
                gputextureview.close();
                gputextureview.texture().close();
            }
        }

        MemoryUtil.memFree(bytebuffer);
        this.uploadAnimationFrames();
    }

    public void preStitch(Set<Identifier> set, ResourceManager resourceManagerIn, int mipmapLevelIn) {
        this.mipmapLevel = mipmapLevelIn;
        Config.dbg("Pre-stitch: " + this.location);
        this.textureFormat = TextureProperties.getTextureFormat();
        this.mapRegisteredSprites.clear();
        this.mapMissingSprites.clear();
        this.counterIndexInMap.reset();
        Config.dbg("Multitexture: " + Config.isMultiTexture());
        TextureUtils.registerCustomSpriteLocations(this.location(), set);
        TextureUtils.registerCustomSprites(this);
        set.addAll(this.mapRegisteredSprites.keySet());
        Set<Identifier> setx = newHashSet(set, this.mapRegisteredSprites.keySet());
        EmissiveTextures.updateIcons(this, setx);
        set.addAll(this.mapRegisteredSprites.keySet());
        if (this.mipmapLevel >= 4) {
            this.mipmapLevel = this.detectMaxMipmapLevel(set, resourceManagerIn);
            Config.log("Mipmap levels: " + this.mipmapLevel);
        }

        int i = getMinSpriteSize(this.mipmapLevel);
        this.iconGridSize = i;
    }

    private GpuTextureView[] getMipViews(ShadersTextureType spriteType) {
        if (spriteType == ShadersTextureType.NORMAL) {
            return this.mipViewsNormal;
        } else {
            return spriteType == ShadersTextureType.SPECULAR ? this.mipViewsSpecular : this.mipViews;
        }
    }

    @Override
    public void dumpContents(Identifier p_450858_, Path p_276127_) throws IOException {
        String s = p_450858_.toDebugFileName();
        TextureUtil.writeAsPNG(p_276127_, s, this.getTexture(), this.maxMipLevel, colorIn -> colorIn);
        dumpSpriteNames(p_276127_, s, this.texturesByName);
    }

    private static void dumpSpriteNames(Path p_261769_, String p_262102_, Map<Identifier, TextureAtlasSprite> p_261722_) {
        Path path = p_261769_.resolve(p_262102_ + ".txt");

        try (Writer writer = Files.newBufferedWriter(path)) {
            for (Entry<Identifier, TextureAtlasSprite> entry : p_261722_.entrySet().stream().sorted(Entry.comparingByKey()).toList()) {
                TextureAtlasSprite textureatlassprite = entry.getValue();
                writer.write(
                    String.format(
                        Locale.ROOT,
                        "%s\tx=%d\ty=%d\tw=%d\th=%d%n",
                        entry.getKey(),
                        textureatlassprite.getX(),
                        textureatlassprite.getY(),
                        textureatlassprite.contents().width(),
                        textureatlassprite.contents().height()
                    )
                );
            }
        } catch (IOException ioexception) {
            LOGGER.warn("Failed to write file {}", path, ioexception);
        }
    }

    public void cycleAnimationFrames() {
        if (this.texture != null) {
            for (SpriteContents.AnimationState spritecontents$animationstate : this.animatedTexturesStates) {
                spritecontents$animationstate.tick();
            }

            this.uploadAnimationFrames();
        }
    }

    private void uploadAnimationFrames() {
        boolean flag = false;
        boolean flag1 = false;
        int i = 0;
        this.activeAnimations.clear();

        for (SpriteContents.AnimationState spritecontents$animationstate : this.animatedTexturesStates) {
            if (spritecontents$animationstate.needsToDraw()) {
                TextureAtlasSprite textureatlassprite = spritecontents$animationstate.getSprite();
                if (textureatlassprite == null
                    || this.isAnimationEnabled(textureatlassprite) && (!SmartAnimations.isActive() || SmartAnimations.isSpriteRendered(textureatlassprite))) {
                    this.activeAnimations.add(spritecontents$animationstate);
                }
            }
        }

        if (!this.activeAnimations.isEmpty()) {
            for (int j = 0; j <= this.maxMipLevel; j++) {
                int j1 = j;

                try (RenderPass renderpass = RenderSystem.getDevice()
                        .createCommandEncoder()
                        .createRenderPass(() -> "Animate " + j1 + " " + this.location, this.mipViews[j], OptionalInt.empty())) {
                    for (SpriteContents.AnimationState spritecontents$animationstate1 : this.activeAnimations) {
                        if (spritecontents$animationstate1.needsToDraw()) {
                            spritecontents$animationstate1.drawToAtlas(renderpass, spritecontents$animationstate1.getDrawUbo(j));
                            i++;
                            TextureAtlasSprite textureatlassprite1 = spritecontents$animationstate1.getSprite();
                            if (textureatlassprite1 != null) {
                                if (textureatlassprite1.spriteNormal != null) {
                                    flag = true;
                                }

                                if (textureatlassprite1.spriteSpecular != null) {
                                    flag1 = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (Config.isShaders()) {
            if (flag) {
                for (int k = 0; k <= this.maxMipLevel; k++) {
                    int k1 = k;

                    try (RenderPass renderpass1 = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "Animate normal " + k1 + " " + this.location, this.mipViewsNormal[k], OptionalInt.empty())) {
                        for (SpriteContents.AnimationState spritecontents$animationstate4 : this.activeAnimations) {
                            TextureAtlasSprite textureatlassprite3 = spritecontents$animationstate4.getSprite();
                            if (textureatlassprite3 != null && textureatlassprite3.spriteNormal != null) {
                                SpriteContents.AnimationState spritecontents$animationstate2 = textureatlassprite3.spriteNormal.getSpriteContentsTicker();
                                if (spritecontents$animationstate2 != null) {
                                    spritecontents$animationstate2.drawToAtlas(renderpass1, spritecontents$animationstate2.getDrawUbo(k));
                                }

                                i++;
                            }
                        }
                    }
                }
            }

            if (flag1) {
                for (int l = 0; l <= this.maxMipLevel; l++) {
                    int l1 = l;

                    try (RenderPass renderpass2 = RenderSystem.getDevice()
                            .createCommandEncoder()
                            .createRenderPass(() -> "Animate specular " + l1 + " " + this.location, this.mipViewsSpecular[l], OptionalInt.empty())) {
                        for (SpriteContents.AnimationState spritecontents$animationstate5 : this.activeAnimations) {
                            TextureAtlasSprite textureatlassprite4 = spritecontents$animationstate5.getSprite();
                            if (textureatlassprite4 != null && textureatlassprite4.spriteSpecular != null) {
                                SpriteContents.AnimationState spritecontents$animationstate6 = textureatlassprite4.spriteSpecular.getSpriteContentsTicker();
                                if (spritecontents$animationstate6 != null) {
                                    spritecontents$animationstate6.drawToAtlas(renderpass2, spritecontents$animationstate6.getDrawUbo(l));
                                }

                                i++;
                            }
                        }
                    }
                }
            }
        }

        if (Config.isMultiTexture()) {
            RenderSystem.getGlDevice().debugLabels().pushDebugGroup(() -> "Animate single " + this.location);

            for (SpriteContents.AnimationState spritecontents$animationstate3 : this.activeAnimations) {
                if (spritecontents$animationstate3.needsToDraw()) {
                    TextureAtlasSprite textureatlassprite2 = spritecontents$animationstate3.getSprite();
                    if (textureatlassprite2 != null) {
                        i += updateAnimationSingle(textureatlassprite2, this.mipViews);
                        if (textureatlassprite2.spriteNormal != null) {
                            i += updateAnimationSingle(textureatlassprite2.spriteNormal, this.mipViewsNormal);
                        }

                        if (textureatlassprite2.spriteSpecular != null) {
                            i += updateAnimationSingle(textureatlassprite2.spriteSpecular, this.mipViewsSpecular);
                        }
                    }
                }
            }

            RenderSystem.getGlDevice().debugLabels().popDebugGroup();
        }

        if (this.terrain) {
            int i1 = Config.getMinecraft().levelRenderer.getFrameCount();
            if (i1 != this.frameCountAnimations) {
                this.countAnimationsActive = i;
                this.frameCountAnimations = i1;
            }

            if (SmartAnimations.isActive()) {
                SmartAnimations.resetSpritesRendered(this);
            }
        }
    }

    @Override
    public void tick() {
        this.cycleAnimationFrames();
    }

    public TextureAtlasSprite getSprite(Identifier p_455105_) {
        TextureAtlasSprite textureatlassprite = this.texturesByName.getOrDefault(p_455105_, this.missingSprite);
        if (textureatlassprite == null) {
            throw new IllegalStateException("Tried to lookup sprite, but atlas is not initialized");
        } else {
            return textureatlassprite;
        }
    }

    public TextureAtlasSprite missingSprite() {
        return Objects.requireNonNull(this.missingSprite, "Atlas not initialized");
    }

    public void clearTextureData() {
        this.sprites.forEach(TextureAtlasSprite::close);
        this.sprites = List.of();
        this.animatedTexturesStates = List.of();
        this.texturesByName = Map.of();
        this.missingSprite = null;
    }

    @Override
    public void close() {
        super.close();

        for (GpuTextureView gputextureview : this.mipViews) {
            gputextureview.close();
        }

        for (GpuTextureView gputextureview1 : this.mipViewsNormal) {
            gputextureview1.close();
        }

        for (GpuTextureView gputextureview2 : this.mipViewsSpecular) {
            gputextureview2.close();
        }

        for (SpriteContents.AnimationState spritecontents$animationstate : this.animatedTexturesStates) {
            spritecontents$animationstate.close();
        }

        if (this.spriteUbos != null) {
            this.spriteUbos.close();
            this.spriteUbos = null;
        }
    }

    public Identifier location() {
        return this.location;
    }

    public int maxSupportedTextureSize() {
        return this.maxSupportedTextureSize;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public static boolean isAbsoluteLocation(Identifier loc) {
        String s = loc.getPath();
        return isAbsoluteLocationPath(s);
    }

    private static boolean isAbsoluteLocationPath(String resPath) {
        String s = resPath.toLowerCase();
        return s.startsWith("optifine/");
    }

    public TextureAtlasSprite getRegisteredSprite(String name) {
        Identifier identifier = new Identifier(name);
        return this.getRegisteredSprite(identifier);
    }

    public TextureAtlasSprite getRegisteredSprite(Identifier loc) {
        return this.mapRegisteredSprites.get(loc);
    }

    public TextureAtlasSprite getUploadedSprite(String name) {
        Identifier identifier = new Identifier(name);
        return this.getUploadedSprite(identifier);
    }

    public TextureAtlasSprite getUploadedSprite(Identifier loc) {
        return this.texturesByName.get(loc);
    }

    private boolean isAnimationEnabled(TextureAtlasSprite ts) {
        if (!this.terrain) {
            return true;
        } else if (ts == TextureUtils.iconWaterStill || ts == TextureUtils.iconWaterFlow) {
            return Config.isAnimatedWater();
        } else if (ts == TextureUtils.iconLavaStill || ts == TextureUtils.iconLavaFlow) {
            return Config.isAnimatedLava();
        } else if (ts == TextureUtils.iconFireLayer0 || ts == TextureUtils.iconFireLayer1) {
            return Config.isAnimatedFire();
        } else if (ts == TextureUtils.iconSoulFireLayer0 || ts == TextureUtils.iconSoulFireLayer1) {
            return Config.isAnimatedFire();
        } else if (ts == TextureUtils.iconCampFire || ts == TextureUtils.iconCampFireLogLit) {
            return Config.isAnimatedFire();
        } else if (ts == TextureUtils.iconSoulCampFire || ts == TextureUtils.iconSoulCampFireLogLit) {
            return Config.isAnimatedFire();
        } else {
            return ts == TextureUtils.iconPortal ? Config.isAnimatedPortal() : Config.isAnimatedTerrain();
        }
    }

    private static void uploadMipmapsSingle(TextureAtlasSprite tas) {
        TextureAtlasSprite textureatlassprite = tas.spriteSingle;
        if (textureatlassprite != null) {
            textureatlassprite.setAnimationIndex(tas.getAnimationIndex());
            SpriteContents.AnimationState spritecontents$animationstate = textureatlassprite.createAnimationState(null, 0);
            if (spritecontents$animationstate != null) {
                textureatlassprite.setTicker(spritecontents$animationstate);
            }

            try {
                textureatlassprite.uploadMipmaps(tas.getSpriteTexture());
            } catch (Exception exception) {
                Config.dbg("Error uploading sprite single: " + textureatlassprite + ", parent: " + tas);
                exception.printStackTrace();
            }
        }
    }

    private static int updateAnimationSingle(TextureAtlasSprite tas, GpuTextureView[] mipViewsRead) {
        TextureAtlasSprite textureatlassprite = tas.spriteSingle;
        if (textureatlassprite != null) {
            for (int i = 0; i < mipViewsRead.length; i++) {
                GpuTexture gputexture = mipViewsRead[i].texture();
                GpuTexture gputexture1 = tas.getSpriteTexture();
                int j = tas.getOriginX() + 0 >> i;
                int k = tas.getOriginY() + 0 >> i;
                int l = tas.getWidth() >> i;
                int i1 = tas.getHeight() >> i;
                RenderSystem.getGlDevice().getGlCommandEncoder().copyTextureToTexture(gputexture, gputexture1, i, 0, 0, j, k, l, i1);
            }

            if (textureatlassprite.isAnimationActive()) {
                return 1;
            }
        }

        return 0;
    }

    public int getCountRegisteredSprites() {
        return this.counterIndexInMap.getValue();
    }

    private int detectMaxMipmapLevel(Set<Identifier> setSpriteLocations, ResourceManager rm) {
        int i = this.detectMinimumSpriteSize(setSpriteLocations, rm, 20);
        if (i < 16) {
            i = 16;
        }

        i = Mth.smallestEncompassingPowerOfTwo(i);
        if (i > 16) {
            Config.log("Sprite size: " + i);
        }

        int j = Mth.log2(i);
        if (j < 4) {
            j = 4;
        }

        return j;
    }

    private int detectMinimumSpriteSize(Set<Identifier> setSpriteLocations, ResourceManager rm, int percentScale) {
        // 1. Указываем типы <Integer, Integer> для Map
        Map<Integer, Integer> map = new HashMap<>();

        for (Identifier identifier : setSpriteLocations) {
            Identifier identifier1 = this.getSpritePath(identifier);

            try {
                Resource resource = rm.getResourceOrThrow(identifier1);
                if (resource != null) {
                    InputStream inputstream = resource.open();
                    if (inputstream != null) {
                        Dimension dimension = TextureUtils.getImageSize(inputstream, "png");
                        inputstream.close();
                        if (dimension != null) {
                            int i = dimension.width;
                            int j = Mth.smallestEncompassingPowerOfTwo(i);

                            // Благодаря дженерикам каст (Integer) больше не нужен
                            if (!map.containsKey(j)) {
                                map.put(j, 1);
                            } else {
                                int k = map.get(j);
                                map.put(j, k + 1);
                            }
                        }
                    }
                }
            } catch (Exception exception) {
                // Игнорируем
            }
        }

        int l = 0;
        // 2. Указываем тип <Integer> для Set
        Set<Integer> set = map.keySet();
        Set<Integer> set1 = new TreeSet<>(set);

        // Теперь set1 содержит Integer, и Java автоматически распакует их в int (Auto-unboxing)
        for (int j1 : set1) {
            int l1 = map.get(j1); // Каст (Integer) больше не нужен
            l += l1;
        }

        int i1 = 16;
        int k1 = 0;
        int i2 = l * percentScale / 100;

        for (int j2 : set1) {
            int k2 = map.get(j2); // Каст (Integer) больше не нужен
            k1 += k2;
            if (j2 > i1) {
                i1 = j2;
            }

            if (k1 > i2) {
                return i1;
            }
        }

        return i1;
    }

    private static int getMinSpriteSize(int mipmapLevels) {
        int i = 1 << mipmapLevels;
        if (i < 8) {
            i = 8;
        }

        return i;
    }

    private static FrameSize fixSpriteSize(FrameSize info, int minSpriteSize) {
        if (info.width() >= minSpriteSize && info.height() >= minSpriteSize) {
            return info;
        }

        int i = Math.max(info.width(), minSpriteSize);
        int j = Math.max(info.height(), minSpriteSize);
        return new FrameSize(i, j);
    }

    public boolean isTextureBound() {
        int i = GlStateManager.getBoundTexture();
        int j = this.getGlTextureId();
        return i == j;
    }

    private void updateIconGrid(int sheetWidth, int sheetHeight) {
        this.iconGridCountX = -1;
        this.iconGridCountY = -1;
        this.iconGrid = null;
        if (this.iconGridSize > 0) {
            this.iconGridCountX = sheetWidth / this.iconGridSize;
            this.iconGridCountY = sheetHeight / this.iconGridSize;
            this.iconGrid = new TextureAtlasSprite[this.iconGridCountX * this.iconGridCountY];
            this.iconGridSizeU = 1.0 / this.iconGridCountX;
            this.iconGridSizeV = 1.0 / this.iconGridCountY;

            for (TextureAtlasSprite textureatlassprite : this.texturesByName.values()) {
                double d0 = 0.5 / sheetWidth;
                double d1 = 0.5 / sheetHeight;
                double d2 = Math.min(textureatlassprite.getU0(), textureatlassprite.getU1()) + d0;
                double d3 = Math.min(textureatlassprite.getV0(), textureatlassprite.getV1()) + d1;
                double d4 = Math.max(textureatlassprite.getU0(), textureatlassprite.getU1()) - d0;
                double d5 = Math.max(textureatlassprite.getV0(), textureatlassprite.getV1()) - d1;
                int i = (int)(d2 / this.iconGridSizeU);
                int j = (int)(d3 / this.iconGridSizeV);
                int k = (int)(d4 / this.iconGridSizeU);
                int l = (int)(d5 / this.iconGridSizeV);

                for (int i1 = i; i1 <= k; i1++) {
                    if (i1 >= 0 && i1 < this.iconGridCountX) {
                        for (int j1 = j; j1 <= l; j1++) {
                            if (j1 >= 0 && j1 < this.iconGridCountX) {
                                int k1 = j1 * this.iconGridCountX + i1;
                                this.iconGrid[k1] = textureatlassprite;
                            } else {
                                Config.warn("Invalid grid V: " + j1 + ", icon: " + textureatlassprite.getName());
                            }
                        }
                    } else {
                        Config.warn("Invalid grid U: " + i1 + ", icon: " + textureatlassprite.getName());
                    }
                }
            }
        }
    }

    public TextureAtlasSprite getIconByUV(double u, double v) {
        if (this.iconGrid == null) {
            return null;
        }

        int i = (int)(u / this.iconGridSizeU);
        int j = (int)(v / this.iconGridSizeV);
        int k = j * this.iconGridCountX + i;
        return k >= 0 && k <= this.iconGrid.length ? this.iconGrid[k] : null;
    }

    public int getCountAnimations() {
        return this.animatedTexturesStates.size();
    }

    public int getCountAnimationsActive() {
        return this.countAnimationsActive;
    }

    public int getIconGridSize() {
        return this.iconGridSize;
    }

    public TextureAtlasSprite registerSprite(Identifier location) {
        if (location == null) {
            throw new IllegalArgumentException("Location is null");
        }

        TextureAtlasSprite textureatlassprite = this.mapRegisteredSprites.get(location);
        if (textureatlassprite != null) {
            return textureatlassprite;
        }

        textureatlassprite = new TextureAtlasSprite(this.location, location);
        textureatlassprite.setTextureAtlas(this);
        this.mapRegisteredSprites.put(location, textureatlassprite);
        textureatlassprite.updateIndexInMap(this.counterIndexInMap);
        return textureatlassprite;
    }

    public Collection<TextureAtlasSprite> getRegisteredSprites() {
        return Collections.unmodifiableCollection(this.mapRegisteredSprites.values());
    }

    public Map<Identifier, TextureAtlasSprite> getMapRegisteredSprites() {
        return Collections.unmodifiableMap(this.mapRegisteredSprites);
    }

    public Collection<Identifier> getRegisteredSpriteNames() {
        return Collections.unmodifiableCollection(this.mapRegisteredSprites.keySet());
    }

    public boolean isTerrain() {
        return this.terrain;
    }

    public boolean isItems() {
        return this.items;
    }

    public CounterInt getCounterIndexInMap() {
        return this.counterIndexInMap;
    }

    private void onSpriteMissing(Identifier loc) {
        TextureAtlasSprite textureatlassprite = this.mapRegisteredSprites.get(loc);
        if (textureatlassprite != null) {
            this.mapMissingSprites.put(loc, textureatlassprite);
        }
    }

    private static <T> Set<T> newHashSet(Set<T> set1, Set<T> set2) {
        Set<T> set = new HashSet<>();
        set.addAll(set1);
        set.addAll(set2);
        return set;
    }

    public int getMipmapLevel() {
        return this.mipmapLevel;
    }

    public boolean isMipmaps() {
        return this.mipmapLevel > 0;
    }

    public ITextureFormat getTextureFormat() {
        return this.textureFormat;
    }

    public IColorBlender getShadersColorBlender(ShadersTextureType typeIn) {
        if (typeIn == null) {
            return null;
        } else {
            return this.textureFormat != null ? this.textureFormat.getColorBlender(typeIn) : new ColorBlenderLinear();
        }
    }

    public boolean isTextureBlend(ShadersTextureType typeIn) {
        if (typeIn == null) {
            return true;
        } else {
            return this.textureFormat != null ? this.textureFormat.isTextureBlend(typeIn) : true;
        }
    }

    public boolean isNormalBlend() {
        return this.isTextureBlend(ShadersTextureType.NORMAL);
    }

    public boolean isSpecularBlend() {
        return this.isTextureBlend(ShadersTextureType.SPECULAR);
    }

    public Identifier getSpritePath(Identifier location) {
        return isAbsoluteLocation(location)
            ? new Identifier(location.getNamespace(), location.getPath() + ".png")
            : new Identifier(location.getNamespace(), String.format(Locale.ROOT, "textures/%s%s", location.getPath(), ".png"));
    }

    @Override
    public String toString() {
        return this.location + ", texture: " + this.texture;
    }

    public Set<Identifier> getTextureLocations() {
        return Collections.unmodifiableSet(this.texturesByName.keySet());
    }
}
