package com.mojang.blaze3d.textures;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.opengl.GlTexture;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraftforge.client.extensions.IForgeGpuTexture;
import net.optifine.shaders.MultiTexID;

@DontObfuscate
public abstract class GpuTexture implements AutoCloseable, IForgeGpuTexture {
    public static final int USAGE_COPY_DST = 1;
    public static final int USAGE_COPY_SRC = 2;
    public static final int USAGE_TEXTURE_BINDING = 4;
    public static final int USAGE_RENDER_ATTACHMENT = 8;
    public static final int USAGE_CUBEMAP_COMPATIBLE = 16;
    private final TextureFormat format;
    private final int width;
    private final int height;
    private final int depthOrLayers;
    private final int mipLevels;
    @GpuTexture.Usage
    private int usage;
    private final String label;
    private AbstractTexture parentTexture;

    public GpuTexture(@GpuTexture.Usage int p_393042_, String p_395679_, TextureFormat p_392008_, int p_394574_, int p_397229_, int p_406893_, int p_405806_) {
        this.usage = p_393042_;
        this.label = p_395679_;
        this.format = p_392008_;
        this.width = p_394574_;
        this.height = p_397229_;
        this.depthOrLayers = p_406893_;
        this.mipLevels = p_405806_;
    }

    public int getWidth(int p_397572_) {
        return this.width >> p_397572_;
    }

    public int getHeight(int p_394674_) {
        return this.height >> p_394674_;
    }

    public int getDepthOrLayers() {
        return this.depthOrLayers;
    }

    public int getMipLevels() {
        return this.mipLevels;
    }

    public TextureFormat getFormat() {
        return this.format;
    }

    @GpuTexture.Usage
    public int usage() {
        return this.usage;
    }

    public String getLabel() {
        return this.label;
    }

    @Override
    public abstract void close();

    public abstract boolean isClosed();

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public MultiTexID getMultiTexID() {
        return null;
    }

    public int getGlTextureId() {
        return 0;
    }

    public void bindTexture() {
    }

    public void deleteGlTexture() {
    }

    public void setUpdateBlurMipmap(boolean updateBlurMipmap) {
    }

    public void setParentTexture(AbstractTexture parentTexture) {
        this.parentTexture = parentTexture;
    }

    public AbstractTexture getParentTexture() {
        return this.parentTexture;
    }

    public void setUsage(int usage) {
        this.usage = usage;
    }

    public GlTexture getGlTexture() {
        return (GlTexture)this;
    }

    @Retention(RetentionPolicy.CLASS)
    @Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.TYPE_USE})
    public @interface Usage {
    }
}
