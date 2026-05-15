package ru.arixcompany.utils.render.shader.shaders.states;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public record CircleRenderState(
        RenderPipeline pipeline,
        float cx, float cy,
        float radius,
        float thickness,
        float startDeg, float endDeg,
        int color,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer buf) {
        float guiWidth  = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float guiHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float x1 = (cx - radius) / guiWidth  * 2.0f - 1.0f;
        float y1 = 1.0f - (cy - radius) / guiHeight * 2.0f;
        float x2 = (cx + radius) / guiWidth  * 2.0f - 1.0f;
        float y2 = 1.0f - (cy + radius) / guiHeight * 2.0f;

        float thicknessNorm = thickness / radius;

        int startPacked = Mth.clamp((int) (Mth.clamp(startDeg, 0, 360) / 360.0f * 255.0f), 0, 255);
        int endPacked   = Mth.clamp((int) (Mth.clamp(endDeg,   0, 360) / 360.0f * 255.0f), 0, 255);

        buf.addVertex(x1, y2, thicknessNorm).setUv(0.0f, 1.0f).setColor(startPacked, endPacked, 0, 255);
        buf.addVertex(x2, y2, thicknessNorm).setUv(1.0f, 1.0f).setColor(startPacked, endPacked, 0, 255);
        buf.addVertex(x2, y1, thicknessNorm).setUv(1.0f, 0.0f).setColor(startPacked, endPacked, 0, 255);
        buf.addVertex(x1, y1, thicknessNorm).setUv(0.0f, 0.0f).setColor(startPacked, endPacked, 0, 255);
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return new ScreenRectangle((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2));
    }
}
