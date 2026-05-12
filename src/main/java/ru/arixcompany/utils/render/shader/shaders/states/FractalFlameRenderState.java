package ru.arixcompany.utils.render.shader.shaders.states;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public record FractalFlameRenderState(
        RenderPipeline pipeline,
        float x, float y, float w, float h,
        float time, float alpha,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer buf) {
        float guiWidth = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float guiHeight = net.minecraft.client.Minecraft.getInstance().getWindow().getGuiScaledHeight();

        float x1 = x / guiWidth * 2.0f - 1.0f;
        float y1 = 1.0f - y / guiHeight * 2.0f;
        float x2 = (x + w) / guiWidth * 2.0f - 1.0f;
        float y2 = 1.0f - (y + h) / guiHeight * 2.0f;

        float aspectRatio = w / h;
        int aspectPacked = Mth.clamp((int) (aspectRatio * 100.0f), 0, 255);
        int alphaPacked = Mth.clamp((int) (alpha * 255.0f), 0, 255);

        buf.addVertex(x1, y2, time)
                .setUv(0.0f, 1.0f)
                .setColor(aspectPacked, 0, 0, alphaPacked);

        buf.addVertex(x2, y2, time)
                .setUv(1.0f, 1.0f)
                .setColor(aspectPacked, 0, 0, alphaPacked);

        buf.addVertex(x2, y1, time)
                .setUv(1.0f, 0.0f)
                .setColor(aspectPacked, 0, 0, alphaPacked);

        buf.addVertex(x1, y1, time)
                .setUv(0.0f, 0.0f)
                .setColor(aspectPacked, 0, 0, alphaPacked);
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        return TextureSetup.noTexture();
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return new ScreenRectangle((int) x, (int) y, (int) w, (int) h);
    }
}