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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.optifine.render.BufferUploader;
import ru.arixcompany.utils.render.shader.IShader;
import ru.arixcompany.utils.render.shader.shaders.states.CircleRenderState;

public final class CircleShader implements IShader {

    private static boolean initialized = false;

    private static final RenderPipeline CIRCLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/circle")
                    .withVertexShader("core/circle")
                    .withFragmentShader("core/circle")
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

    public static void drawCircle(float cx, float cy, float startDeg, float endDeg, float radius, float thickness, int color) {
        if (radius <= 0.0f || thickness <= 0.0f) return;

        thickness = Mth.clamp(thickness, 0.0f, radius);

        submitCircleQuad(cx, cy, radius, thickness, startDeg, endDeg, color);
    }

    public static void drawCircle(GuiGraphics ctx, float cx, float cy, float startDeg, float endDeg, float radius, float thickness, int color) {
        if (radius <= 0.0f || thickness <= 0.0f) return;

        thickness = Mth.clamp(thickness, 0.0f, radius);

        submitCircleQuad(cx, cy, radius, thickness, startDeg, endDeg, color);
    }

    private static void submitCircleQuad(float cx, float cy, float radius, float thickness, float startDeg, float endDeg, int color) {
        RenderSystem.setShaderColor(
                ARGB.red(color)   / 255.0f,
                ARGB.green(color) / 255.0f,
                ARGB.blue(color)  / 255.0f,
                ARGB.alpha(color) / 255.0f
        );

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(
                DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4
        );

        BufferBuilder builder = new BufferBuilder(
                byteBuffer,
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        float x1 = toClipX(cx - radius);
        float y1 = toClipY(cy - radius);
        float x2 = toClipX(cx + radius);
        float y2 = toClipY(cy + radius);

        float thicknessNorm = thickness / radius;

        int startPacked = Mth.clamp((int) (Mth.clamp(startDeg, 0, 360) / 360.0f * 255.0f), 0, 255);
        int endPacked   = Mth.clamp((int) (Mth.clamp(endDeg,   0, 360) / 360.0f * 255.0f), 0, 255);

        builder.addVertex(x1, y2, thicknessNorm).setUv(0.0f, 1.0f).setColor(startPacked, endPacked, 0, 255);
        builder.addVertex(x2, y2, thicknessNorm).setUv(1.0f, 1.0f).setColor(startPacked, endPacked, 0, 255);
        builder.addVertex(x2, y1, thicknessNorm).setUv(1.0f, 0.0f).setColor(startPacked, endPacked, 0, 255);
        builder.addVertex(x1, y1, thicknessNorm).setUv(0.0f, 0.0f).setColor(startPacked, endPacked, 0, 255);

        MeshData mesh = builder.buildOrThrow();
        BufferUploader.draw(CIRCLE_PIPELINE, mesh, () -> "circle");

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static float toClipX(float x) {
        float guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return x / guiWidth * 2.0f - 1.0f;
    }

    private static float toClipY(float y) {
        float guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return 1.0f - y / guiHeight * 2.0f;
    }
}
