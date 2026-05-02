package net.optifine.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.SectionBuffers;
import net.optifine.util.PairInt;
import org.joml.Matrix4fc;

public class RegionRenderer {
    private ChunkSectionLayer layer;
    private Map<PairInt, Map<VboRegion, List<SectionBuffers>>> mapPosRegionBuffers = new LinkedHashMap<>(16);
    private int lastRegionX = Integer.MIN_VALUE;
    private int lastRegionZ = Integer.MIN_VALUE;
    private Map<VboRegion, List<SectionBuffers>> lastRegionBuffers;
    private VboRegion lastVboRegion;
    private List<SectionBuffers> lastBuffers;

    public RegionRenderer(ChunkSectionLayer layer) {
        this.layer = layer;
    }

    public void addSection(int regionX, int regionZ, SectionBuffers buffersIn) {
        if (regionX != this.lastRegionX || regionZ != this.lastRegionZ) {
            PairInt pairint = PairInt.of(regionX, regionZ);
            this.lastRegionBuffers = this.mapPosRegionBuffers.computeIfAbsent(pairint, k -> new LinkedHashMap<>(8));
            this.lastRegionX = regionX;
            this.lastRegionZ = regionZ;
            this.lastVboRegion = null;
        }

        VboRegion vboregion = buffersIn.getVboRegion();
        if (vboregion != this.lastVboRegion) {
            this.lastBuffers = this.lastRegionBuffers.computeIfAbsent(vboregion, k -> new ArrayList<>());
            this.lastVboRegion = vboregion;
        }

        this.lastBuffers.add(buffersIn);
    }

    public void reset() {
        this.lastRegionX = Integer.MIN_VALUE;
        this.lastRegionZ = Integer.MIN_VALUE;
        this.lastRegionBuffers = null;
        this.lastVboRegion = null;
        this.lastBuffers = null;
    }

    public void finishPrepare(
        Matrix4fc viewIn,
        double xIn,
        double yIn,
        double zIn,
        List<DynamicUniforms.ChunkSectionInfo> sectionInfos,
        EnumMap<ChunkSectionLayer, List<RenderPass.Draw<GpuBufferSlice[]>>> mapLayerDraws,
        int atlasWidth,
        int atlasHeight
    ) {
        for (Entry<PairInt, Map<VboRegion, List<SectionBuffers>>> entry : this.mapPosRegionBuffers.entrySet()) {
            PairInt pairint = entry.getKey();
            if (entry.getValue() != null) {
                DynamicUniforms.ChunkSectionInfo dynamicuniforms$chunksectioninfo = new DynamicUniforms.ChunkSectionInfo(
                    viewIn, pairint.getLeft(), 0, pairint.getRight(), 1.0F, atlasWidth, atlasHeight, xIn, yIn, zIn
                );
                int i = sectionInfos.size();
                sectionInfos.add(dynamicuniforms$chunksectioninfo);
                RegionRenderData regionrenderdata = new RegionRenderData(this, pairint);
                BiConsumer<GpuBufferSlice[], RenderPass.UniformUploader> biconsumer = (buffers2In, uploader2In) -> uploader2In.upload(
                    "ChunkSection", buffers2In[i]
                );
                RenderPass.Draw renderpass$draw = new RenderPass.Draw<>(0, null, null, null, 0, 0, biconsumer, regionrenderdata, null);
                mapLayerDraws.get(this.layer).add(renderpass$draw);
            }
        }
    }

    public void renderRegion(PairInt positionIn) {
        Map<VboRegion, List<SectionBuffers>> map = this.mapPosRegionBuffers.get(positionIn);
        if (this.mapPosRegionBuffers != null) {
            for (Entry<VboRegion, List<SectionBuffers>> entry : map.entrySet()) {
                VboRegion vboregion = entry.getKey();
                List<SectionBuffers> list = entry.getValue();
                if (!list.isEmpty()) {
                    for (SectionBuffers sectionbuffers : list) {
                        sectionbuffers.drawInRegion();
                    }

                    vboregion.finishDraw();
                    list.clear();
                }
            }
        }
    }

    public void clear() {
        this.reset();
        this.mapPosRegionBuffers.clear();
    }

    @Override
    public String toString() {
        return this.layer + "";
    }
}
