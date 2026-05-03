package ru.arixcompany.utils.render;

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
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.optifine.render.BufferUploader;

public final class RoundRectShader {

    private static boolean initialized = false;

    private static final RenderPipeline ROUND_RECT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/round_rect")
                    .withVertexShader("core/round_rect")
                    .withFragmentShader("core/round_rect")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline ROUND_RECT_OUTLINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/round_rect_outline")
                    .withVertexShader("core/round_rect_outline")
                    .withFragmentShader("core/round_rect_outline")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );

    private static final RenderPipeline ROUND_RECT_CORNERS_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                    .withLocation("pipeline/round_rect_corners")
                    .withVertexShader("core/round_rect_corners")
                    .withFragmentShader("core/round_rect_corners")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .build()
    );

    private RoundRectShader() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;

        Object a = ROUND_RECT_PIPELINE;
        Object b = ROUND_RECT_OUTLINE_PIPELINE;
        Object c = ROUND_RECT_CORNERS_PIPELINE;
    }

    public static void drawRoundRect(float x, float y, float w, float h, float radius, int color) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;
        if (ARGB.alpha(color) <= 0) return;

        radius = Mth.clamp(radius, 0.0f, Math.min(w, h) * 0.5f);

        submitSimpleQuad(
                ROUND_RECT_PIPELINE,
                x, y, w, h, radius,
                color, color, color, color,
                "round_rect"
        );
    }

    public static void drawRoundRect(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl, int color) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;
        if (ARGB.alpha(color) <= 0) return;

        float max = Math.min(w, h) * 0.5f;
        tl = Mth.clamp(tl, 0.0f, max);
        tr = Mth.clamp(tr, 0.0f, max);
        br = Mth.clamp(br, 0.0f, max);
        bl = Mth.clamp(bl, 0.0f, max);

        if (almostEquals(tl, tr) && almostEquals(tr, br) && almostEquals(br, bl)) {
            drawRoundRect(x, y, w, h, tl, color);
            return;
        }

        submitCornerQuad(x, y, w, h, tl, tr, br, bl, color);
    }

    public static void drawRoundRectOutline(float x, float y, float w, float h,
                                            float radius, float thickness, int color) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;
        if (ARGB.alpha(color) <= 0) return;
        if (thickness <= 0.0f) return;

        float max = Math.min(w, h) * 0.5f;
        radius = Mth.clamp(radius, 0.0f, max);
        thickness = Mth.clamp(thickness, 0.0f, max);

        submitOutlineQuad(x, y, w, h, radius, thickness, color);
    }

    public static void drawRoundRectGradient(float x, float y, float w, float h,
                                             float radius, int topColor, int bottomColor) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;

        radius = Mth.clamp(radius, 0.0f, Math.min(w, h) * 0.5f);

        submitSimpleQuad(
                ROUND_RECT_PIPELINE,
                x, y, w, h, radius,
                bottomColor, bottomColor, topColor, topColor,
                "round_rect_gradient"
        );
    }

    public static void drawShadow(float x, float y, float w, float h,
                                  float radius, int layers, int shadowColor) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;
        if (layers <= 0) return;

        int baseAlpha = ARGB.alpha(shadowColor);
        if (baseAlpha <= 0) return;

        for (int i = layers; i >= 1; i--) {
            float spread = i;
            float t = i / (float) layers;

            int a = Math.max(1, (int) (baseAlpha * (t * t) * 0.35f));
            int col = (shadowColor & 0x00FFFFFF) | (a << 24);

            drawRoundRect(
                    x - spread,
                    y - spread,
                    w + spread * 2.0f,
                    h + spread * 2.0f,
                    radius + spread,
                    col
            );
        }
    }

    private static void submitSimpleQuad(RenderPipeline pipeline,
                                         float x, float y, float w, float h, float radius,
                                         int colorBL, int colorBR, int colorTR, int colorTL,
                                         String label) {
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(
                DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4
        );

        BufferBuilder builder = new BufferBuilder(
                byteBuffer,
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        float x1 = toClipX(x);
        float y1 = toClipY(y);
        float x2 = toClipX(x + w);
        float y2 = toClipY(y + h);

        builder.addVertex(x1, y2, radius)
                .setUv(0.0f, 1.0f)
                .setColor(ARGB.red(colorBL), ARGB.green(colorBL), ARGB.blue(colorBL), ARGB.alpha(colorBL));

        builder.addVertex(x2, y2, radius)
                .setUv(1.0f, 1.0f)
                .setColor(ARGB.red(colorBR), ARGB.green(colorBR), ARGB.blue(colorBR), ARGB.alpha(colorBR));

        builder.addVertex(x2, y1, radius)
                .setUv(1.0f, 0.0f)
                .setColor(ARGB.red(colorTR), ARGB.green(colorTR), ARGB.blue(colorTR), ARGB.alpha(colorTR));

        builder.addVertex(x1, y1, radius)
                .setUv(0.0f, 0.0f)
                .setColor(ARGB.red(colorTL), ARGB.green(colorTL), ARGB.blue(colorTL), ARGB.alpha(colorTL));

        MeshData mesh = builder.buildOrThrow();
        BufferUploader.draw(pipeline, mesh, () -> label);
    }

    private static void submitCornerQuad(float x, float y, float w, float h,
                                         float tl, float tr, float br, float bl,
                                         int color) {
        float maxRad = Math.min(w, h) * 0.5f;
        if (maxRad <= 0.0f) return;

        int tlPacked = packRadius(tl, maxRad);
        int trPacked = packRadius(tr, maxRad);
        int brPacked = packRadius(br, maxRad);
        int blPacked = packRadius(bl, maxRad);

        float rf = ARGB.red(color) / 255.0f;
        float gf = ARGB.green(color) / 255.0f;
        float bf = ARGB.blue(color) / 255.0f;
        float af = ARGB.alpha(color) / 255.0f;

        RenderSystem.setShaderColor(rf, gf, bf, af);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(
                DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4
        );

        BufferBuilder builder = new BufferBuilder(
                byteBuffer,
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        float x1 = toClipX(x);
        float y1 = toClipY(y);
        float x2 = toClipX(x + w);
        float y2 = toClipY(y + h);

        builder.addVertex(x1, y2, 0.0f)
                .setUv(0.0f, 1.0f)
                .setColor(0, 0, 0, blPacked);

        builder.addVertex(x2, y2, 0.0f)
                .setUv(1.0f, 1.0f)
                .setColor(0, 0, brPacked, 0);

        builder.addVertex(x2, y1, 0.0f)
                .setUv(1.0f, 0.0f)
                .setColor(0, trPacked, 0, 0);

        builder.addVertex(x1, y1, 0.0f)
                .setUv(0.0f, 0.0f)
                .setColor(tlPacked, 0, 0, 0);

        MeshData mesh = builder.buildOrThrow();
        BufferUploader.draw(ROUND_RECT_CORNERS_PIPELINE, mesh, () -> "round_rect_corners");

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void submitOutlineQuad(float x, float y, float w, float h,
                                          float radius, float thickness, int color) {
        float minSide = Math.min(w, h);
        if (minSide <= 0.0f) return;

        int thicknessPacked = Mth.clamp((int) ((thickness / minSide) * 255.0f), 0, 255);

        float rf = ARGB.red(color) / 255.0f;
        float gf = ARGB.green(color) / 255.0f;
        float bf = ARGB.blue(color) / 255.0f;
        float af = ARGB.alpha(color) / 255.0f;

        RenderSystem.setShaderColor(rf, gf, bf, af);

        ByteBufferBuilder byteBuffer = new ByteBufferBuilder(
                DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize() * 4
        );

        BufferBuilder builder = new BufferBuilder(
                byteBuffer,
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR
        );

        float x1 = toClipX(x);
        float y1 = toClipY(y);
        float x2 = toClipX(x + w);
        float y2 = toClipY(y + h);

        builder.addVertex(x1, y2, radius)
                .setUv(0.0f, 1.0f)
                .setColor(255, 255, 255, thicknessPacked);

        builder.addVertex(x2, y2, radius)
                .setUv(1.0f, 1.0f)
                .setColor(255, 255, 255, thicknessPacked);

        builder.addVertex(x2, y1, radius)
                .setUv(1.0f, 0.0f)
                .setColor(255, 255, 255, thicknessPacked);

        builder.addVertex(x1, y1, radius)
                .setUv(0.0f, 0.0f)
                .setColor(255, 255, 255, thicknessPacked);

        MeshData mesh = builder.buildOrThrow();
        BufferUploader.draw(ROUND_RECT_OUTLINE_PIPELINE, mesh, () -> "round_rect_outline");

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }


    public static void drawHorizontalGradient(float x, float y, float w, float h,
                                              float radius, int leftColor, int rightColor) {
        drawHorizontalGradient(x, y, w, h, radius, radius, radius, radius, leftColor, rightColor);
    }

    public static void drawHorizontalGradient(float x, float y, float w, float h,
                                              float tl, float tr, float br, float bl,
                                              int leftColor, int rightColor) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;

        submitSimpleQuad(
                ROUND_RECT_PIPELINE,
                x, y, w, h, Math.max(Math.max(tl, tr), Math.max(br, bl)),
                leftColor,
                rightColor,
                rightColor,
                leftColor,
                "horizontal_gradient"
        );
    }

    public static void drawRoundRectGradient(float x, float y, float w, float h,
                                             float tl, float tr, float br, float bl,
                                             int topColor, int bottomColor) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;

        submitSimpleQuad(
                ROUND_RECT_PIPELINE,
                x, y, w, h, Math.max(Math.max(tl, tr), Math.max(br, bl)),
                bottomColor,
                bottomColor,
                topColor,
                topColor,
                "vertical_gradient"
        );
    }

    public static void drawGradient4(float x, float y, float w, float h,
                                     float radius,
                                     int c00, int c10, int c11, int c01) {
        drawGradient4(x, y, w, h, radius, radius, radius, radius, c00, c10, c11, c01);
    }

    public static void drawGradient4(float x, float y, float w, float h,
                                     float tl, float tr, float br, float bl,
                                     int c00, int c10, int c11, int c01) {
        if (!initialized) init();
        if (w <= 0.0f || h <= 0.0f) return;

        submitSimpleQuad(
                ROUND_RECT_PIPELINE,
                x, y, w, h, Math.max(Math.max(tl, tr), Math.max(br, bl)),
                c01,
                c11,
                c10,
                c00,
                "gradient_4corner"
        );
    }

    private static float toClipX(float x) {
        float guiWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return x / guiWidth * 2.0f - 1.0f;
    }

    private static float toClipY(float y) {
        float guiHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return 1.0f - y / guiHeight * 2.0f;
    }

    private static int packRadius(float radius, float maxRad) {
        if (maxRad <= 0.0f) return 0;
        return Mth.clamp((int) ((radius / maxRad) * 255.0f), 0, 255);
    }

    private static boolean almostEquals(float a, float b) {
        return Math.abs(a - b) < 0.01f;
    }
}