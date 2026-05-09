package ru.arixcompany.utils.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Реализация GuiElementRenderState для round rect шейдера.
 * Позволяет рендерить скруглённые прямоугольники через систему страт GuiGraphics,
 * гарантируя правильный порядок рендеринга относительно шрифтов и других элементов.
 */
public record RoundRectRenderState(
        RenderPipeline pipeline,
        float x, float y, float w, float h,
        float radius,
        int colorBL, int colorBR, int colorTR, int colorTL,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer buf) {
        float guiWidth  = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float guiHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float x1 = x / guiWidth  * 2.0f - 1.0f;
        float y1 = 1.0f - y / guiHeight * 2.0f;
        float x2 = (x + w) / guiWidth  * 2.0f - 1.0f;
        float y2 = 1.0f - (y + h) / guiHeight * 2.0f;

        buf.addVertex(x1, y2, radius).setUv(0.0f, 1.0f)
                .setColor(ARGB.red(colorBL), ARGB.green(colorBL), ARGB.blue(colorBL), ARGB.alpha(colorBL));
        buf.addVertex(x2, y2, radius).setUv(1.0f, 1.0f)
                .setColor(ARGB.red(colorBR), ARGB.green(colorBR), ARGB.blue(colorBR), ARGB.alpha(colorBR));
        buf.addVertex(x2, y1, radius).setUv(1.0f, 0.0f)
                .setColor(ARGB.red(colorTR), ARGB.green(colorTR), ARGB.blue(colorTR), ARGB.alpha(colorTR));
        buf.addVertex(x1, y1, radius).setUv(0.0f, 0.0f)
                .setColor(ARGB.red(colorTL), ARGB.green(colorTL), ARGB.blue(colorTL), ARGB.alpha(colorTL));
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return new ScreenRectangle((int) x, (int) y, (int) w, (int) h);
    }
}
