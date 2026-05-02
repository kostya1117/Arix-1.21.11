package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import net.optifine.render.MultiTextureData;
import net.optifine.render.RegionRenderData;
import org.jspecify.annotations.Nullable;

@DontObfuscate
public interface RenderPass extends AutoCloseable {
    void pushDebugGroup(Supplier<String> p_408185_);

    void popDebugGroup();

    void setPipeline(RenderPipeline p_394712_);

    void bindTexture(String p_395678_,  GpuTextureView p_406805_,  GpuSampler p_453654_);

    void setUniform(String p_394004_, GpuBuffer p_408232_);

    void setUniform(String p_397813_, GpuBufferSlice p_409190_);

    void enableScissor(int p_392594_, int p_394512_, int p_391828_, int p_391712_);

    void disableScissor();

    void setVertexBuffer(int p_393394_, GpuBuffer p_395764_);

    void setIndexBuffer(GpuBuffer p_393127_, VertexFormat.IndexType p_397465_);

    void drawIndexed(int p_393708_, int p_396477_, int p_409446_, int p_407636_);

    <T> void drawMultipleIndexed(
        Collection<RenderPass.Draw<T>> p_392442_,
         GpuBuffer p_396172_,
        VertexFormat. IndexType p_394399_,
        Collection<String> p_406241_,
        T p_406608_
    );

    void draw(int p_397730_, int p_394941_);

    @Override
    void close();

    record Draw<T>(
        int slot,
        GpuBuffer vertexBuffer,
        GpuBuffer indexBuffer,
        VertexFormat.IndexType indexType,
        int firstIndex,
        int indexCount,
        BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer,
        RegionRenderData regionRenderData,
        MultiTextureData multiTextureData
    ) {
        public Draw(
            int slot,
            GpuBuffer vertexBuffer,
            GpuBuffer indexBuffer,
            VertexFormat.IndexType indexType,
            int firstIndex,
            int indexCount,
             BiConsumer<T, RenderPass.UniformUploader> uniformUploaderConsumer
        ) {
            this(slot, vertexBuffer, indexBuffer, indexType, firstIndex, indexCount, uniformUploaderConsumer, null, null);
        }

        public Draw(int p_394209_, GpuBuffer p_394761_, GpuBuffer p_393439_, VertexFormat.IndexType p_393418_, int p_392985_, int p_394886_) {
            this(p_394209_, p_394761_, p_393439_, p_393418_, p_392985_, p_394886_, null);
        }
    }

    interface UniformUploader {
        void upload(String p_391168_, GpuBufferSlice p_407704_);
    }
}
