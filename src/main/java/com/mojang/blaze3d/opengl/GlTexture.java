package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import java.util.OptionalDouble;
import net.optifine.Config;
import net.optifine.shaders.MultiTexID;
import net.optifine.shaders.ShadersTex;
import org.jspecify.annotations.Nullable;

public class GlTexture extends GpuTexture {
    private static final int EMPTY = -1;
    public final int id;
    private int firstFboId = -1;
    private int firstFboDepthId = -1;
    private @Nullable Int2IntMap fboCache;
    protected boolean closed;
    private int views;
    public MultiTexID multiTex;
    private GlSampler sampler = new GlSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, 1, OptionalDouble.empty());
    private final boolean stencilEnabled;

    protected GlTexture(
        @GpuTexture.Usage int p_394590_, String p_393950_, TextureFormat p_392837_, int p_391379_, int p_391947_, int p_396659_, int p_408255_, int p_408889_
    ) {
        this(p_394590_, p_393950_, p_392837_, p_391379_, p_391947_, p_396659_, p_408255_, p_408889_, false);
    }

    protected GlTexture(int usageIn, String labelIn, TextureFormat formatIn, int widthIn, int heightIn, int depthIn, int levelsIn, int idIn, boolean stencil) {
        super(usageIn, labelIn, formatIn, widthIn, heightIn, depthIn, levelsIn);
        this.id = idIn;
        this.stencilEnabled = stencil;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            if (this.views == 0) {
                this.destroyImmediately();
            }
        }
    }

    private void destroyImmediately() {
        GlStateManager._deleteTexture(this.id);
        ShadersTex.deleteTextures(this, this.id);
        if (this.firstFboId != -1) {
            GlStateManager._glDeleteFramebuffers(this.firstFboId);
        }

        if (this.fboCache != null) {
            for (int i : this.fboCache.values()) {
                GlStateManager._glDeleteFramebuffers(i);
            }
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    public int getFbo(DirectStateAccess p_393100_, @Nullable GpuTexture p_394451_) {
        int i = p_394451_ == null ? 0 : ((GlTexture)p_394451_).id;
        if (this.firstFboDepthId == i) {
            return this.firstFboId;
        }

        if (this.firstFboId == -1) {
            this.firstFboId = this.createFbo(p_393100_, i);
            this.firstFboDepthId = i;
            return this.firstFboId;
        }

        if (this.fboCache == null) {
            this.fboCache = new Int2IntArrayMap();
        }

        return this.fboCache.computeIfAbsent(i, p_447708_ -> this.createFbo(p_393100_, p_447708_));
    }

    private int createFbo(DirectStateAccess p_457028_, int p_457100_) {
        int i = p_457028_.createFrameBufferObject();
        p_457028_.bindFrameBufferTextures(i, this.id, p_457100_, 0, 0);
        RenderSystem.getGlDevice().debugLabels().applyLabelFramebuffer(i, this.getLabel() + "/" + i);
        return i;
    }

    public int glId() {
        return this.id;
    }

    public void addViews() {
        this.views++;
    }

    public void removeViews() {
        this.views--;
        if (this.closed && this.views == 0) {
            this.destroyImmediately();
        }
    }

    @Override
    public MultiTexID getMultiTexID() {
        return ShadersTex.getMultiTexID(this);
    }

    @Override
    public int getGlTextureId() {
        return this.glId();
    }

    @Override
    public void bindTexture() {
        GlStateManager._bindTexture(this.glId());
    }

    @Override
    public void deleteGlTexture() {
        this.close();
    }

    @Override
    public boolean isStencilEnabled() {
        return this.stencilEnabled;
    }

    public static String usageToString(int usageIn) {
        StringBuilder stringbuilder = new StringBuilder();
        if ((usageIn & 2) != 0) {
            stringbuilder.append("S");
        }

        if ((usageIn & 1) != 0) {
            stringbuilder.append("D");
        }

        if ((usageIn & 4) != 0) {
            stringbuilder.append("T");
        }

        if ((usageIn & 8) != 0) {
            stringbuilder.append("A");
        }

        if ((usageIn & 16) != 0) {
            stringbuilder.append("C");
        }

        if (stringbuilder.isEmpty()) {
            stringbuilder.append("0x" + Integer.toHexString(usageIn));
        }

        return stringbuilder.toString();
    }

    public void applySampler(GlSampler samplerIn) {
        if (!this.sampler.matches(samplerIn)) {
            DirectStateAccess directstateaccess = RenderSystem.getGlDevice().directStateAccess();
            if (this.sampler.getAddressModeU() != samplerIn.getAddressModeU()) {
                directstateaccess.texParameter(this.id, 10242, GlConst.toGl(samplerIn.getAddressModeU()));
            }

            if (this.sampler.getAddressModeV() != samplerIn.getAddressModeV()) {
                directstateaccess.texParameter(this.id, 10243, GlConst.toGl(samplerIn.getAddressModeV()));
            }

            if (this.sampler.getMinFilter() != samplerIn.getMinFilter()) {
                if (samplerIn.getMinFilter() == FilterMode.NEAREST) {
                    directstateaccess.texParameter(this.id, 10241, 9986);
                } else if (samplerIn.getMinFilter() == FilterMode.LINEAR) {
                    directstateaccess.texParameter(this.id, 10241, 9987);
                }
            }

            if (this.sampler.getMagFilter() != samplerIn.getMagFilter()) {
                if (samplerIn.getMagFilter() == FilterMode.NEAREST) {
                    directstateaccess.texParameter(this.id, 10240, 9728);
                } else if (samplerIn.getMagFilter() == FilterMode.LINEAR) {
                    directstateaccess.texParameter(this.id, 10240, 9729);
                }
            }

            if (this.sampler.getMaxAnisotropy() != samplerIn.getMaxAnisotropy() && !Config.isAnisotropicFiltering()) {
                directstateaccess.texParameter(this.id, 34046, samplerIn.getMaxAnisotropy());
            }

            if (!this.sampler.getMaxLod().equals(samplerIn.getMaxLod())) {
                if (samplerIn.getMaxLod().isPresent()) {
                    directstateaccess.texParameter(this.id, 33083, (float)samplerIn.getMaxLod().getAsDouble());
                } else {
                    directstateaccess.texParameter(this.id, 33083, 1000.0F);
                }
            }

            this.sampler = samplerIn;
        }
    }

    public boolean hasFramebuffer(int framebufferIn) {
        return framebufferIn == this.firstFboId;
    }

    @Override
    public String toString() {
        String s = this.firstFboId + (this.fboCache != null ? " + " + this.fboCache : "");
        return this.getLabel() + ", " + this.getWidth(0) + "x" + this.getHeight(0) + ", " + usageToString(this.usage()) + ", fbo: " + s;
    }
}
