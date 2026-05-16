package ru.arixcompany.utils.render.shader.shaders;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import ru.arixcompany.utils.render.shader.IShader;
import ru.arixcompany.utils.render.shader.shaders.states.RoundBlurRenderState;

public final class RoundBlurShader implements IShader {

    private static final RenderPipeline BLUR_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.GUI_TEXTURED_SNIPPET)
                    .withLocation("pipeline/round_blur")
                    .withVertexShader("core/round_blur")
                    .withFragmentShader("core/round_blur")
                    .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
                    .withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA))
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .build()
    );

    @Override public void init() {}

    public static void drawBlur(GuiGraphics ctx, float x, float y, float w, float h, float radius, float strength, int color) {
        if (w <= 0.0f || h <= 0.0f) return;

        ctx.getGuiRenderState().submitGuiElement(
                new RoundBlurRenderState(
                        BLUR_PIPELINE, x, y, w, h, radius,
                        strength, color, ctx.getCurrentScissor()
                )
        );
    }
}