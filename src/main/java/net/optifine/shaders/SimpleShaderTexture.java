package net.optifine.shaders;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.server.packs.resources.ResourceManager;
import net.optifine.util.MetadataUtils;
import org.apache.commons.io.IOUtils;

public class SimpleShaderTexture extends AbstractTexture {
    private String texturePath;
    private long size = 0L;

    public SimpleShaderTexture(String texturePath) {
        this.texturePath = texturePath;
    }

    public void loadTexture(ResourceManager resourceManager) throws IOException {
        this.deleteGlTexture();
        InputStream inputstream = Shaders.getShaderPackResourceStream(this.texturePath);
        if (inputstream == null) {
            throw new FileNotFoundException("Shader texture not found: " + this.texturePath);
        }

        try {
            NativeImage nativeimage = NativeImage.read(inputstream);
            this.size = nativeimage.getSize();
            TextureMetadataSection texturemetadatasection = loadTextureMetadataSection(
                this.texturePath, new TextureMetadataSection(false, false, MipmapStrategy.AUTO, 0.0F)
            );
            this.texture = RenderSystem.getDevice()
                .createTexture(this.texturePath, 5, TextureFormat.RGBA8, nativeimage.getWidth(), nativeimage.getHeight(), 1, 1);
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
            nativeimage.uploadTextureSub(
                0,
                0,
                0,
                0,
                0,
                nativeimage.getWidth(),
                nativeimage.getHeight(),
                texturemetadatasection.blur(),
                texturemetadatasection.clamp(),
                false,
                true
            );
            this.texture.setParentTexture(this);
        } finally {
            IOUtils.closeQuietly(inputstream);
        }
    }

    public static TextureMetadataSection loadTextureMetadataSection(String texturePath, TextureMetadataSection def) {
        String s = texturePath + ".mcmeta";
        String s1 = "texture";
        InputStream inputstream = Shaders.getShaderPackResourceStream(s);
        if (inputstream != null) {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(inputstream));

            try {
                JsonObject jsonobject = new JsonParser().parse(bufferedreader).getAsJsonObject();
                JsonObject jsonobject1 = jsonobject.getAsJsonObject(s1);
                if (jsonobject1 != null) {
                    TextureMetadataSection texturemetadatasection = MetadataUtils.parseTextureMetadataSection(jsonobject1);
                    if (texturemetadatasection != null) {
                        return texturemetadatasection;
                    }
                }
            } catch (RuntimeException runtimeexception) {
                SMCLog.warning("Error reading metadata: " + s);
                SMCLog.warning(runtimeexception.getClass().getName() + ": " + runtimeexception.getMessage());
            } finally {
                IOUtils.closeQuietly(bufferedreader);
                IOUtils.closeQuietly(inputstream);
            }
        }

        return def;
    }

    public long getSize() {
        return this.size;
    }

    @Override
    public String toString() {
        return "Shader: " + this.texturePath + ", " + super.toString();
    }
}
