package net.optifine.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlBuffer;
import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class BufferUploader {
    private static final Vector3f VEC3_ZERO = new Vector3f();
    private static final Matrix4f MAT4_ZERO = new Matrix4f();

    public static void draw(RenderPipeline pipelineIn, MeshData dataIn, Supplier<String> labelIn) {
        draw(pipelineIn, dataIn, labelIn, (GpuTextureView)null, (GpuSampler)null);
    }

    public static void draw(RenderPipeline pipelineIn, MeshData dataIn, Supplier<String> labelIn, Identifier locationTextureIn, FilterMode filterIn) {
        AbstractTexture abstracttexture = Minecraft.getInstance().getTextureManager().getTexture(locationTextureIn);
        GpuTextureView gputextureview = abstracttexture.getTextureView();
        draw(pipelineIn, dataIn, labelIn, gputextureview, filterIn);
    }

    public static void draw(RenderPipeline pipelineIn, MeshData dataIn, Supplier<String> labelIn, GpuTextureView textureIn, FilterMode filterIn) {
        GpuSampler gpusampler = RenderSystem.getSamplerCache().getRepeat(filterIn);
        draw(pipelineIn, dataIn, labelIn, textureIn, gpusampler);
    }

    public static void draw(RenderPipeline pipelineIn, MeshData dataIn, Supplier<String> labelIn, GpuTextureView textureIn, GpuSampler samplerIn) {
        GpuBufferSlice gpubufferslice = RenderSystem.getDynamicUniforms()
            .writeTransform(RenderSystem.getModelViewMatrix(), RenderSystem.getShaderColor(), VEC3_ZERO, MAT4_ZERO);
        MeshData meshdata = dataIn;

        try {
            GpuBuffer gpubuffer = pipelineIn.getVertexFormat().uploadImmediateVertexBuffer(dataIn.vertexBuffer());
            GpuBuffer gpubuffer1;
            VertexFormat.IndexType vertexformat$indextype;
            if (dataIn.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(dataIn.drawState().mode());
                gpubuffer1 = rendersystem$autostorageindexbuffer.getBuffer(dataIn.drawState().indexCount());
                vertexformat$indextype = rendersystem$autostorageindexbuffer.type();
            } else {
                gpubuffer1 = pipelineIn.getVertexFormat().uploadImmediateIndexBuffer(dataIn.indexBuffer());
                vertexformat$indextype = dataIn.drawState().indexType();
            }

            RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
            GpuTextureView gputextureview = RenderSystem.outputColorTextureOverride != null
                ? RenderSystem.outputColorTextureOverride
                : rendertarget.getColorTextureView();
            GpuTextureView gputextureview1 = rendertarget.useDepth
                ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : rendertarget.getDepthTextureView())
                : null;

            try (RenderPass renderpass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(labelIn, gputextureview, OptionalInt.empty(), gputextureview1, OptionalDouble.empty())) {
                renderpass.setPipeline(pipelineIn);
                ScissorState scissorstate = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissorstate.enabled()) {
                    renderpass.enableScissor(scissorstate.x(), scissorstate.y(), scissorstate.width(), scissorstate.height());
                }

                RenderSystem.bindDefaultUniforms(renderpass);
                renderpass.setUniform("DynamicTransforms", gpubufferslice);
                renderpass.setVertexBuffer(0, gpubuffer);
                if (textureIn != null && samplerIn != null) {
                    renderpass.bindTexture("Sampler0", textureIn, samplerIn);
                }

                renderpass.setIndexBuffer(gpubuffer1, vertexformat$indextype);
                renderpass.drawIndexed(0, 0, dataIn.drawState().indexCount(), 1);
            }
        } catch (Throwable throwable2) {
            if (dataIn != null) {
                try {
                    meshdata.close();
                } catch (Throwable throwable) {
                    throwable2.addSuppressed(throwable);
                }
            }

            throw throwable2;
        }

        if (dataIn != null) {
            dataIn.close();
        }
    }

    public static void draw(MeshData dataIn) {
        MeshData.DrawState meshdata$drawstate = dataIn.drawState();
        VertexFormat vertexformat = meshdata$drawstate.format();
        int i = meshdata$drawstate.indexCount();
        VertexFormat.Mode vertexformat$mode = meshdata$drawstate.mode();
        GlBuffer glbuffer = (GlBuffer)vertexformat.uploadImmediateVertexBuffer(dataIn.vertexBuffer());
        RenderSystem.getGlDevice().vertexArrayCache().bindVertexArray(vertexformat, glbuffer);
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(vertexformat$mode);
        GlBuffer glbuffer1 = (GlBuffer)rendersystem$autostorageindexbuffer.getBuffer(i);
        VertexFormat.IndexType vertexformat$indextype = rendersystem$autostorageindexbuffer.type();
        GlStateManager._glBindBuffer(34963, glbuffer1.getHandle());
        GlStateManager._drawElements(GlConst.toGl(vertexformat$mode), i, GlConst.toGl(vertexformat$indextype), 0L);
        dataIn.close();
    }

    public static String getSamplerName(int indexIn) {
        switch (indexIn) {
            case 0:
                return "Sampler0";
            case 1:
                return "Sampler1";
            case 2:
                return "Sampler2";
            case 3:
                return "Sampler3";
            case 4:
                return "Sampler4";
            case 5:
                return "Sampler5";
            case 6:
                return "Sampler6";
            case 7:
                return "Sampler7";
            case 8:
                return "Sampler8";
            case 9:
                return "Sampler9";
            case 10:
                return "Sampler10";
            case 11:
                return "Sampler11";
            default:
                return "Sampler" + indexIn;
        }
    }
}
