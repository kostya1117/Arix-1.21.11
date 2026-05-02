package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import net.optifine.render.MultiTextureData;
import net.optifine.render.VboRange;
import net.optifine.render.VboRegion;
import net.optifine.util.GlUtil;
import org.jspecify.annotations.Nullable;

public final class SectionBuffers implements AutoCloseable {
    private GpuBuffer vertexBuffer;
    private  GpuBuffer indexBuffer;
    private int indexCount;
    private VertexFormat.IndexType indexType;
    private VboRegion vboRegion;
    private VboRange vboRange;
    private MultiTextureData multiTextureData;
    private static ByteBuffer emptyBuffer = GlUtil.allocateMemory(0);

    public SectionBuffers(GpuBuffer p_408356_,  GpuBuffer p_408653_, int p_409167_, VertexFormat.IndexType p_405985_) {
        this.vertexBuffer = p_408356_;
        this.indexBuffer = p_408653_;
        this.indexCount = p_409167_;
        this.indexType = p_405985_;
    }

    public GpuBuffer getVertexBuffer() {
        return this.vertexBuffer;
    }

    public  GpuBuffer getIndexBuffer() {
        return this.indexBuffer;
    }

    public void setIndexBuffer( GpuBuffer p_406609_) {
        this.indexBuffer = p_406609_;
    }

    public int getIndexCount() {
        return this.indexCount;
    }

    public VertexFormat.IndexType getIndexType() {
        return this.indexType;
    }

    public void setIndexType(VertexFormat.IndexType p_409241_) {
        this.indexType = p_409241_;
    }

    public void setIndexCount(int p_406950_) {
        this.indexCount = p_406950_;
    }

    public void setVertexBuffer(GpuBuffer p_407204_) {
        this.vertexBuffer = p_407204_;
    }

    @Override
    public void close() {
        if (this.vboRegion != null) {
            if (this.vboRange.isAllocated()) {
                this.vboRegion.bufferData(emptyBuffer, this.vboRange);
            }
        } else {
            this.vertexBuffer.close();
            if (this.indexBuffer != null) {
                this.indexBuffer.close();
            }
        }
    }

    public void setVboRegion(VboRegion vboRegionIn) {
        if (vboRegionIn != null) {
            this.close();
            if (this.vboRegion == null && this.vboRange == null) {
                this.vboRegion = vboRegionIn;
                this.vboRange = new VboRange();
            } else {
                throw new IllegalArgumentException("Region is already set");
            }
        }
    }

    public VboRegion getVboRegion() {
        return this.vboRegion;
    }

    public void upload(MeshData dataIn) {
        MeshData.DrawState meshdata$drawstate = dataIn.drawState();
        if (this.vboRegion != null) {
            ByteBuffer bytebuffer = dataIn.vertexBuffer();
            bytebuffer.position(0);
            bytebuffer.limit(meshdata$drawstate.getVertexBufferSize());
            this.vboRegion.bufferData(bytebuffer, this.vboRange);
            bytebuffer.position(0);
            bytebuffer.limit(meshdata$drawstate.getVertexBufferSize());
        }

        this.multiTextureData = dataIn.getMultiTextureData();
        this.updateMultiTextureData();
    }

    public void updateMultiTextureData() {
        if (this.multiTextureData != null) {
            this.multiTextureData.applySort();
        }
    }

    public void drawInRegion() {
        if (this.vboRegion != null) {
            this.vboRegion.drawArrays(VertexFormat.Mode.QUADS, this.vboRange);
        }
    }

    public MultiTextureData getMultiTextureData() {
        return this.multiTextureData;
    }
}
