package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.function.Supplier;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.optifine.render.BufferUploader;
import org.jspecify.annotations.Nullable;

public class Tesselator {
    private static final int MAX_BYTES = 786432;
    private final ByteBufferBuilder buffer;
    private static  Tesselator instance;

    public static void init() {
        if (instance != null) {
            throw new IllegalStateException("Tesselator has already been initialized");
        }

        instance = new Tesselator();
    }

    public static Tesselator getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Tesselator has not been initialized");
        } else {
            return instance;
        }
    }

    public Tesselator(int p_85912_) {
        this.buffer = new ByteBufferBuilder(p_85912_);
    }

    public Tesselator() {
        this(786432);
    }

    public BufferBuilder begin(VertexFormat.Mode p_342351_, VertexFormat p_344902_) {
        return new BufferBuilder(this.buffer, p_342351_, p_344902_);
    }

    public void clear() {
        this.buffer.clear();
    }

    public void draw(RenderType renderType, BufferBuilder bufferIn) {
        MeshData meshdata = bufferIn.build();
        if (meshdata != null) {
            renderType.draw(meshdata);
        }
    }

    public void draw(RenderPipeline pipeline, BufferBuilder bufferIn, Supplier<String> labelIn) {
        BufferUploader.draw(pipeline, bufferIn.buildOrThrow(), labelIn);
    }
}
