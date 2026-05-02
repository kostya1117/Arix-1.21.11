package net.optifine.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.optifine.Config;
import org.joml.Matrix4fc;

public class RegionLayerRenderer {
    private RegionRenderer[] renderers = makeRenderers();

    private static RegionRenderer[] makeRenderers() {
        RegionRenderer[] aregionrenderer = new RegionRenderer[2];

        for (int i = 0; i < aregionrenderer.length; i++) {
            aregionrenderer[i] = new RegionRenderer(ChunkSectionLayer.VALUES[i]);
        }

        return aregionrenderer;
    }

    public static boolean canRenderLayer(ChunkSectionLayer layerIn) {
        return layerIn == null ? false : layerIn.ordinal() < 2;
    }

    public void addSection(ChunkSectionLayer layerIn, int regionX, int regionZ, SectionBuffers buffersIn) {
        int i = layerIn.ordinal();
        this.renderers[i].addSection(regionX, regionZ, buffersIn);
    }

    public void finishPrepare(
        Matrix4fc viewIn,
        double xIn,
        double yIn,
        double zIn,
        List<DynamicUniforms.ChunkSectionInfo> transforms,
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> mapLayerDraws
    ) {
        TextureAtlas textureatlas = Config.getTextureMapBlocks();
        int i = textureatlas.atlasWidth;
        int j = textureatlas.atlasHeight;

        for (int k = 0; k < this.renderers.length; k++) {
            this.renderers[k].finishPrepare(viewIn, xIn, yIn, zIn, transforms, mapLayerDraws, i, j);
        }
    }

    public void reset() {
        for (int i = 0; i < this.renderers.length; i++) {
            this.renderers[i].reset();
        }
    }

    public void clear() {
        for (int i = 0; i < this.renderers.length; i++) {
            this.renderers[i].clear();
        }
    }
}
