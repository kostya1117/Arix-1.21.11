package ru.arixcompany.utils.render.shader.shaders.states;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

public record RoundBlurRenderState(
        RenderPipeline pipeline,
        float x, float y, float w, float h,
        float radius, float strength, int color,
        @Nullable ScreenRectangle scissorArea
) implements GuiElementRenderState {

    @Override
    public void buildVertices(VertexConsumer buf) {
        float sw = (float) Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float sh = (float) Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Перевод координат в Clip Space для вершинного шейдера
        float x1 = x / sw * 2.0f - 1.0f;
        float y1 = 1.0f - y / sh * 2.0f;
        float x2 = (x + w) / sw * 2.0f - 1.0f;
        float y2 = 1.0f - (y + h) / sh * 2.0f;

        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        // Сила блюра (0-10) -> Альфа (0-255)
        int a = (int) (Mth.clamp(strength, 0f, 10f) * 25.5f);

        // Z = радиус, UV = 0..1 для SDF, Color = фильтр + сила блюра
        buf.addVertex(x1, y2, radius).setUv(0.0f, 1.0f).setColor(r, g, b, a);
        buf.addVertex(x2, y2, radius).setUv(1.0f, 1.0f).setColor(r, g, b, a);
        buf.addVertex(x2, y1, radius).setUv(1.0f, 0.0f).setColor(r, g, b, a);
        buf.addVertex(x1, y1, radius).setUv(0.0f, 0.0f).setColor(r, g, b, a);
    }

    @Override
    public @NotNull TextureSetup textureSetup() {
        GpuTextureView screenView = Minecraft.getInstance().getMainRenderTarget().getColorTextureView();
        // Используем LINEAR фильтрацию для Sampler0, чтобы блюр был мягче
        return TextureSetup.singleTexture(
                screenView,
                RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
        );
    }

    @Override
    public @Nullable ScreenRectangle bounds() {
        return new ScreenRectangle((int) x, (int) y, (int) w, (int) h);
    }
}