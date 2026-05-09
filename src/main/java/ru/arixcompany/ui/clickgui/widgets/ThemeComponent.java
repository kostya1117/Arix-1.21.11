package ru.arixcompany.ui.clickgui.widgets;

import lombok.NoArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.Arix;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.ui.clickgui.Gui;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.features.module.Theme;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;

@NoArgsConstructor
public final class ThemeComponent implements IComponent {

    private static final float CIRCLE_SIZE = 12.0F; // Размер цветного кружка
    private static final float GAP         = 8.0F;  // Расстояние между кружками
    private static final float PADDING     = 6.0F;  // Внутренний отступ плашки
    private static final float GAP_AFTER_SEARCH = 10.0F;

    private float getThemeY() {
        // Располагаем прямо под поиском: Y поиска (15) + Высота (22) + Отступ (10)
        return 15.0F + 22.0F + GAP_AFTER_SEARCH;
    }

    private float getContainerWidth() {
        int count = Gui.themes.length;
        return (count * CIRCLE_SIZE) + ((count - 1) * GAP) + (PADDING * 2);
    }

    private float getThemeStartX() {
        // Правильное центрирование относительно ширины экрана
        float centerX = mc.getWindow().getGuiScaledWidth() / 2.0F;
        return centerX - (getContainerWidth() / 2.0F);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float alpha) {
        float x = getThemeStartX();
        float y = getThemeY();
        float w = getContainerWidth();
        float h = CIRCLE_SIZE + (PADDING * 2);

        // Фон плашки тем (закругление h / 2.0F делает идеальную форму таблетки / Pill)
        RenderUtils.fillRoundRect(guiGraphics, x, y, w, h, h / 2.0F, Colors.bgSecondary(alpha));
        RenderUtils.drawRoundRectOutline(x, y, w, h, h / 2.0F, 1.0F, Colors.outline(alpha));

        float curX = x + PADDING;
        float curY = y + PADDING;

        for (Theme theme : Gui.themes) {
            // Анимация выбора темы
            theme.animation.setDirection(theme == Gui.selectedTheme ? Direction.FORWARDS : Direction.BACKWARDS);
            float anim = (float) theme.animation.getOutput();

            int themeColor = theme.getMain().getRGB();

            // Если тема выбрана, рисуем вокруг неё красивое обводящее кольцо (Stroke)
            if (anim > 0.01F) {
                float ringSize = CIRCLE_SIZE + 4.0F;
                int ringColor = ColorUtil.replAlpha(themeColor, (int) (255.0F * alpha * anim));
                RenderUtils.drawRoundRectOutline(
                        curX - 2.0F, curY - 2.0F,
                        ringSize, ringSize,
                        ringSize / 2.0F, 1.5F, ringColor
                );
            }

            // Сам кружок темы
            int circleColor = ColorUtil.replAlpha(themeColor, (int) (255.0F * alpha));
            RenderUtils.fillRoundRect(guiGraphics, curX, curY, CIRCLE_SIZE, CIRCLE_SIZE, CIRCLE_SIZE / 2.0F, circleColor);

            curX += CIRCLE_SIZE + GAP;
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        float startX = getThemeStartX();
        float y      = getThemeY();
        float curX   = startX + PADDING;
        float curY   = y + PADDING;

        for (Theme theme : Gui.themes) {
            // Проверка клика по кружку (с небольшим запасом вокруг)
            if (MathUtils.isHovered(mouseX, mouseY, curX - 2.0F, curY - 2.0F, CIRCLE_SIZE + 4.0F, CIRCLE_SIZE + 4.0F)) {

                if (Gui.selectedTheme != theme) {
                    Gui.preSelectedTheme = Gui.selectedTheme;
                    Gui.selectedTheme    = theme;
                    Arix.getInstance().setCurrentTheme(theme);

                    // Опционально: можно добавить звук клика
                }
                return true;
            }
            curX += CIRCLE_SIZE + GAP;
        }
        return false;
    }
}