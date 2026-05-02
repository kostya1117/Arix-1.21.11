package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.Config;
import net.optifine.EmissiveTextures;
import net.optifine.shaders.ShadersTex;

public abstract class ReloadableTexture extends AbstractTexture {
    private final Identifier resourceId;
    private ResourceManager resourceManager = Config.getResourceManager();
    public Identifier locationEmissive;
    public boolean isEmissive;
    public long size;

    public ReloadableTexture(Identifier p_454973_) {
        this.resourceId = p_454973_;
    }

    public Identifier resourceId() {
        return this.resourceId;
    }

    public void apply(TextureContents p_376644_) {
        boolean flag = p_376644_.clamp();
        boolean flag1 = p_376644_.blur();
        AddressMode addressmode = flag ? AddressMode.CLAMP_TO_EDGE : AddressMode.REPEAT;
        FilterMode filtermode = flag1 ? FilterMode.LINEAR : FilterMode.NEAREST;
        this.sampler = RenderSystem.getSamplerCache().getSampler(addressmode, addressmode, filtermode, filtermode, false);

        try (NativeImage nativeimage = p_376644_.image()) {
            this.doLoad(nativeimage);
        }
    }

    protected void doLoad(NativeImage p_378310_) {
        GpuDevice gpudevice = RenderSystem.getDevice();
        this.close();
        this.texture = gpudevice.createTexture(this.resourceId::toString, 5, TextureFormat.RGBA8, p_378310_.getWidth(), p_378310_.getHeight(), 1, 1);
        this.textureView = gpudevice.createTextureView(this.texture);
        gpudevice.createCommandEncoder().writeToTexture(this.texture, p_378310_);
        this.texture.setParentTexture(this);
        if (Config.isShaders()) {
            ShadersTex.loadSimpleTextureNS(this.getGlTextureId(), p_378310_, this.sampler, this.resourceManager, this.resourceId, this.getMultiTexID());
        }

        if (EmissiveTextures.isActive()) {
            EmissiveTextures.loadTexture(this.resourceId, this);
        }

        this.size = p_378310_.getSize();
    }

    public abstract TextureContents loadContents(ResourceManager p_378474_) throws IOException;
}
