package net.minecraft.client.gui.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import ru.arixcompany.Arix;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;
import ru.arixcompany.utils.render.shader.shaders.FractalFlameShader;

import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

public class LoadingOverlay extends Overlay {
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final Consumer<Optional<Throwable>> onFinish;
    private final boolean fadeIn;
    private float currentProgress;
    private long fadeOutStart = -1L;
    private long fadeInStart = -1L;

    public LoadingOverlay(Minecraft mc, ReloadInstance reload, Consumer<Optional<Throwable>> onFinish, boolean fadeIn) {
        this.minecraft = mc;
        this.reload = reload;
        this.onFinish = onFinish;
        this.fadeIn = fadeIn;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int width = g.guiWidth();
        int height = g.guiHeight();
        long now = Util.getMillis();

        if (this.fadeIn && this.fadeInStart == -1L) {
            this.fadeInStart = now;
        }

        float fadeOutAlpha = this.fadeOutStart > -1L ? (float)(now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAlpha = this.fadeInStart > -1L ? (float)(now - this.fadeInStart) / 500.0F : -1.0F;
        float alpha;

        if (fadeOutAlpha >= 1.0F) {
            this.minecraft.setOverlay(null);
            return;
        }

        if (fadeOutAlpha >= 0.0F) {
            alpha = 1.0F - Mth.clamp(fadeOutAlpha, 0.0F, 1.0F);
        } else if (fadeInAlpha >= 0.0F) {
            alpha = Mth.clamp(fadeInAlpha, 0.0F, 1.0F);
        } else {
            alpha = 1.0F;
        }

        FractalFlameShader.draw(g, 0, 0, width, height);
        g.nextStratum();

        if (fadeOutAlpha >= 0.0F && this.minecraft.screen != null) {
            this.minecraft.screen.render(g, 0, 0, delta);
        }

        RenderUtils.fillRect(0, 0, width, height, new Color(0, 0, 0, (int)(150 * alpha)).getRGB());

        float centerX = width / 2f;
        float centerY = height / 2f;

        float titleSize = 32f;
        String title = "Arix";
        float tw = FontManager.get(titleSize).getWidth(title);
        FontManager.get(titleSize).drawString(g, title, centerX - tw / 2f, centerY - 40f,
                new Color(255, 255, 255, (int)(255 * alpha)).getRGB());

        float targetProgress = this.reload.getActualProgress();
        this.currentProgress = Mth.lerp(0.05f, this.currentProgress, targetProgress);

        float barW = 180f;
        float barH = 4f;
        float barX = centerX - barW / 2f;
        float barY = centerY + 10f;

        drawCustomBar(g, barX, barY, barW, barH, alpha);

        String percentText = (int)(this.currentProgress * 100) + "%";
        float pw = FontManager.get(10).getWidth(percentText);
        FontManager.get(10).drawString(g, percentText, centerX - pw / 2f, barY + 10f,
                new Color(255, 255, 255, (int)(200 * alpha)).getRGB());

        String status = this.reload.isDone() ? "Загрузка завершена" : "Загрузка ресурсов...";
        float sw = FontManager.get(9).getWidth(status);
        FontManager.get(9).drawString(g, status, centerX - sw / 2f, barY + 22f,
                new Color(255, 255, 255, (int)(120 * alpha)).getRGB());

        if (this.fadeOutStart == -1L && this.reload.isDone() && (!this.fadeIn || now - this.fadeInStart >= 1000L)) {
            this.fadeOutStart = now;
            try {
                this.reload.checkExceptions();
                this.onFinish.accept(Optional.empty());
            } catch (Throwable throwable) {
                this.onFinish.accept(Optional.of(throwable));
            }
        }
    }

    private void drawCustomBar(GuiGraphics g, float x, float y, float w, float h, float alpha) {
        Color themeColor = Arix.getInstance().getCurrentTheme().getMain();
        int accent = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(255 * alpha)).getRGB();
        int bg = new Color(30, 30, 30, (int)(150 * alpha)).getRGB();
        int outline = new Color(255, 255, 255, (int)(40 * alpha)).getRGB();

        RenderUtils.fillRoundRect(g, x, y, w, h, 2f, bg);
        RenderUtils.drawRoundRectOutline(x, y, w, h, 2f, 0.5f, outline);

        float fillW = w * this.currentProgress;
        if (fillW > 2) {
            RenderUtils.fillRoundRect(g, x, y, fillW, h, 2f, accent);

            RenderUtils.drawShadow(x, y, fillW, h, 4f, 1, new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(100 * alpha)).getRGB());
        }
    }
}