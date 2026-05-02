package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.optifine.shaders.MultiTexID;
import org.jspecify.annotations.Nullable;

public abstract class AbstractTexture implements AutoCloseable {
    protected  GpuTexture texture;
    protected  GpuTextureView textureView;
    protected GpuSampler sampler = RenderSystem.getSamplerCache()
        .getSampler(AddressMode.REPEAT, AddressMode.REPEAT, FilterMode.NEAREST, FilterMode.LINEAR, false);

    @Override
    public void close() {
        if (this.texture != null) {
            this.texture.close();
            this.texture.setParentTexture(null);
            this.texture = null;
        }

        if (this.textureView != null) {
            this.textureView.close();
            this.textureView = null;
        }
    }

    public GpuTexture getTexture() {
        if (this.texture == null) {
            throw new IllegalStateException("Texture does not exist, can't get it before something initializes it");
        } else {
            return this.texture;
        }
    }

    public GpuTextureView getTextureView() {
        if (this.textureView == null) {
            throw new IllegalStateException("Texture view does not exist, can't get it before something initializes it");
        } else {
            return this.textureView;
        }
    }

    public GpuSampler getSampler() {
        return this.sampler;
    }

    public MultiTexID getMultiTexID() {
        return this.texture.getMultiTexID();
    }

    public int getGlTextureId() {
        return this.texture.getGlTextureId();
    }

    public GlTexture getGlTexture() {
        return (GlTexture)this.texture;
    }

    public void bindTexture() {
        this.texture.bindTexture();
    }

    public void deleteGlTexture() {
        if (this.texture != null) {
            this.texture.deleteGlTexture();
        }
    }

    public void setSampler(GpuSampler gpuSampler) {
        this.sampler = gpuSampler;
    }

    public boolean hasGlTexture() {
        return this.texture != null;
    }

    @Override
    public String toString() {
        return this.texture + "";
    }
}
