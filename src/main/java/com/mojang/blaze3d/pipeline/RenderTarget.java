package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.opengl.GlDevice;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.opengl.GlTextureView;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.OptionalInt;
import net.minecraft.client.renderer.RenderPipelines;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import static com.mojang.blaze3d.opengl.GlConst.GL_COLOR_BUFFER_BIT;
import static com.mojang.blaze3d.opengl.GlConst.GL_DEPTH_BUFFER_BIT;

public abstract class RenderTarget {
    private static int UNNAMED_RENDER_TARGETS = 0;
    public int width;
    public int height;
    protected final String label;
    public final boolean useDepth;
    protected @Nullable GpuTexture colorTexture;
    protected @Nullable GpuTextureView colorTextureView;
    protected @Nullable GpuTexture depthTexture;
    protected @Nullable GpuTextureView depthTextureView;
    public FilterMode filterMode;
    private boolean stencilEnabled = false;

    public RenderTarget(@Nullable String p_392164_, boolean p_166199_) {
        this.label = p_392164_ == null ? "FBO " + UNNAMED_RENDER_TARGETS++ : p_392164_;
        this.useDepth = p_166199_;
    }

    public void resize(int p_83942_, int p_83943_) {
        RenderSystem.assertOnRenderThread();
        this.destroyBuffers();
        this.createBuffers(p_83942_, p_83943_);
    }

    public void destroyBuffers() {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
        }

        if (this.depthTextureView != null) {
            this.depthTextureView.close();
            this.depthTextureView = null;
        }

        if (this.colorTexture != null) {
            this.colorTexture.close();
            this.colorTexture = null;
        }

        if (this.colorTextureView != null) {
            this.colorTextureView.close();
            this.colorTextureView = null;
        }
    }

    public void copyDepthFrom(RenderTarget p_83946_) {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
        }

        if (p_83946_.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
        }

        RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(p_83946_.depthTexture, this.depthTexture, 0, 0, 0, 0, 0, this.width, this.height);
    }

    public void createBuffers(int p_83951_, int p_83952_) {
        RenderSystem.assertOnRenderThread();
        GpuDevice gpudevice = RenderSystem.getDevice();
        int i = gpudevice.getMaxTextureSize();
        if (p_83951_ > 0 && p_83951_ <= i && p_83952_ > 0 && p_83952_ <= i) {
            this.width = p_83951_;
            this.height = p_83952_;
            if (this.useDepth) {
                this.depthTexture = ((GlDevice)gpudevice)
                    .createTexture(() -> this.label + " / Depth", 15, TextureFormat.DEPTH32, p_83951_, p_83952_, 1, 1, this.stencilEnabled);
                this.depthTextureView = gpudevice.createTextureView(this.depthTexture);
            }

            this.colorTexture = gpudevice.createTexture(() -> this.label + " / Color", 15, TextureFormat.RGBA8, p_83951_, p_83952_, 1, 1);
            this.colorTextureView = gpudevice.createTextureView(this.colorTexture);
        } else {
            throw new IllegalArgumentException("Window " + p_83951_ + "x" + p_83952_ + " size out of bounds (max. size: " + i + ")");
        }
    }

    public void blitToScreen() {
        if (this.colorTexture == null) {
            throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
        }

        RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTextureView);
    }

    public void blitAndBlendToTexture(GpuTextureView p_409912_) {
        RenderSystem.assertOnRenderThread();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(() -> "Blit render target", p_409912_, OptionalInt.empty())) {
            renderpass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
            RenderSystem.bindDefaultUniforms(renderpass);
            renderpass.bindTexture("InSampler", this.colorTextureView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderpass.draw(0, 3);
        }
    }

    public void bindWrite(boolean clear) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.outputColorTextureOverride = this.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = this.useDepth ? this.getDepthTextureView() : null;
        if (clear) {
            GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);

            int mask = GL_COLOR_BUFFER_BIT;

            if (this.useDepth) {
                GL11.glClearDepth(1.0);
                mask |= GL_DEPTH_BUFFER_BIT;
            }
            GlStateManager._clear(mask);
        }
    }

    public void unbindWrite() {
        RenderSystem.assertOnRenderThread();
        RenderSystem.outputColorTextureOverride = null;
        RenderSystem.outputDepthTextureOverride = null;
    }

    public void setFilterMode(FilterMode p_397955_) {
        this.setFilterMode(p_397955_, false);
    }

    private void setFilterMode(FilterMode p_397959_, boolean p_333030_) {
        if (this.colorTexture == null) {
            throw new IllegalStateException("Can't change filter mode, color texture doesn't exist yet");
        } else {
            if (p_333030_ || p_397959_ != this.filterMode) {
                this.filterMode = p_397959_;
                this.colorTexture.setTextureFilter(p_397959_, false);
            }
        }
    }

    public @Nullable GpuTexture getColorTexture() {
        return this.colorTexture;
    }

    public @Nullable GpuTextureView getColorTextureView() {
        return this.colorTextureView;
    }

    public @Nullable GpuTexture getDepthTexture() {
        return this.depthTexture;
    }

    public @Nullable GpuTextureView getDepthTextureView() {
        return this.depthTextureView;
    }

    public boolean hasFramebuffer(int framebufferIn) {
        return this.colorTexture != null && ((GlTexture)this.colorTexture).hasFramebuffer(framebufferIn)
            ? true
            : this.colorTextureView != null && ((GlTextureView)this.colorTextureView).hasFramebuffer(framebufferIn);
    }

    @Override
    public String toString() {
        return this.label + ", " + this.width + "x" + this.height + ", color: " + this.colorTexture + ", depth: " + this.depthTexture;
    }

    public void enableStencil() {
        if (!this.stencilEnabled) {
            this.stencilEnabled = true;
            this.resize(this.width, this.height);
        }
    }

    public boolean isStencilEnabled() {
        return this.stencilEnabled;
    }
}
