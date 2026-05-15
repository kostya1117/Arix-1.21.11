package ru.arixcompany.utils.render.shader.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.optifine.render.BufferUploader;
import ru.arixcompany.utils.render.shader.shaders.states.FractalFlameRenderState;
import ru.arixcompany.utils.render.shader.IShader;

public final class FractalFlameShader implements IShader {

    private static boolean initialized = false;

    private static final RenderPipeline FRACTAL_FLAME_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/fractal_flame")
                    .withVertexShader("core/fractal_flame")
                    .withFragmentShader("core/fractal_flame")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );

    @Override
    public void init() {
        if (initialized) return;
        initialized = true;
    }

    public static void draw(float x, float y, float w, float h) {
        draw(x, y, w, h, 1.0f);
    }

    public static void draw(float x, float y, float w, float h, float alpha) {
        if (w <= 0.0f || h <= 0.0f) return;

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;
        float aspectRatio = w / h;

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(
                DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4
        );

        BufferBuilder builder = new BufferBuilder(
                byteBuffer,
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        int aspectPacked = Mth.clamp((int) (aspectRatio * 100.0f), 0, 255);
        int alphaPacked = Mth.clamp((int) (alpha * 255.0f), 0, 255);

        builder.addVertex(x,     y + h, time).setUv(0.0f, 1.0f).setColor(aspectPacked, 0, 0, alphaPacked);
        builder.addVertex(x + w, y + h, time).setUv(1.0f, 1.0f).setColor(aspectPacked, 0, 0, alphaPacked);
        builder.addVertex(x + w, y,     time).setUv(1.0f, 0.0f).setColor(aspectPacked, 0, 0, alphaPacked);
        builder.addVertex(x,     y,     time).setUv(0.0f, 0.0f).setColor(aspectPacked, 0, 0, alphaPacked);

        MeshData mesh = builder.buildOrThrow();
        BufferUploader.draw(FRACTAL_FLAME_PIPELINE, mesh, () -> "fractal_flame");

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    public static void draw(GuiGraphics ctx, float x, float y, float w, float h) {
        draw(ctx, x, y, w, h, 1.0f);
    }

    public static void draw(GuiGraphics ctx, float x, float y, float w, float h, float alpha) {
        if (w <= 0.0f || h <= 0.0f) return;

        float time = (System.currentTimeMillis() % 100000L) / 1000.0f;

        ctx.getGuiRenderState().submitGuiElement(
                new FractalFlameRenderState(
                        FRACTAL_FLAME_PIPELINE,
                        x, y, w, h,
                        time, alpha,
                        ctx.getCurrentScissor()
                )
        );
    }
}