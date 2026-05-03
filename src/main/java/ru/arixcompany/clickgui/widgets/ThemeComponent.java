package ru.arixcompany.clickgui.widgets;

import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.Arix;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.Theme;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;

@NoArgsConstructor
public final class ThemeComponent implements IComponent {

    private static final float THEME_OFFSET = 18.0F;
    private static final float THEME_SIZE   = 9.25F;
    private static final float PANEL_PAD    = 9.0F;
    private static final float GAP_AFTER_SEARCH = 6.0F;

    private float getThemeY() {
        return 8.0F + 18.0F + GAP_AFTER_SEARCH;
    }

    private float getThemeStartX() {
        float centerX    = mc.getWindow().getScreenWidth() / 2.0F;
        float totalWidth = Gui.themes.length * THEME_OFFSET;
        return centerX - totalWidth / 2.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics,
                       int mouseX, int mouseY, float alpha) {
        float mainAnim   = (float) Gui.alphaPC.getOutput();
        int alpha255     = (int) (255.0F * mainAnim);
        int alphaShadow  = (int) (90.0F  * mainAnim);

        float y          = getThemeY();
        float startX     = getThemeStartX();
        int count        = Gui.themes.length;
        float totalWidth = count * THEME_OFFSET;

        int outlineColor = ColorUtil.replAlpha(
                ColorUtil.multDark(ColorUtil.getOutLineColor(1, 1), 1.0F),
                (int) (15.3F * mainAnim)
        );
        int bgColor = ColorUtil.replAlpha(
                ColorUtil.getBackGroundColor(1, 1),
                (int) (178.0F * mainAnim)
        );

        RenderUtils.fillRoundRect( startX - PANEL_PAD + 1.0F, y - 5.0F,
                totalWidth + PANEL_PAD - 1.0F, 21.25F,
                6.5F, 6.5F, 6.5F, 6.5F,
                bgColor);

        RenderUtils.fillRoundRect(startX - PANEL_PAD + 1.0F, y - 5.0F,
                totalWidth + PANEL_PAD - 1.0F, 21.25F,
                6.5F, outlineColor);

        float x = startX;
        for (Theme theme : Gui.themes) {
            theme.animation.setDirection(
                    theme == Gui.selectedTheme ? Direction.FORWARDS : Direction.BACKWARDS
            );

            RenderUtils.drawShadow( x + 4.5F, y + 4.76F + 0.5F,
                    0.1F, 0.1F, 10.0F, 6,
                    ColorUtil.setAlpha(
                            theme.getMain(),
                            (int) (alphaShadow * theme.animation.getOutput())
                    ).getRGB());

            RenderUtils.fillRoundRect(x, y + 0.76F, THEME_SIZE, THEME_SIZE, 10.0F,
                    ColorUtil.setAlpha(theme.getMain(), alpha255).getRGB());

            x += THEME_OFFSET;
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        float y      = getThemeY();
        float startX = getThemeStartX();
        float x      = startX;

        for (Theme theme : Gui.themes) {
            if (MathUtils.isHovered(mouseX, mouseY, x, y, 16.0F, 16.0F)) {
                Gui.animation14.reset();
                Gui.preSelectedTheme = Gui.selectedTheme;
                Gui.selectedTheme    = theme;
                Arix.getInstance().setCurrentTheme(theme);
                return true;
            }
            x += THEME_OFFSET;
        }
        return false;
    }
}