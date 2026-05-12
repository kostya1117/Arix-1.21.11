package ru.arixcompany.ui.title.button.implement;

import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.ui.title.button.AbstractButton;
import ru.arixcompany.utils.Colors;
import ru.arixcompany.utils.animation.Animation;
import ru.arixcompany.utils.animation.Direction;
import ru.arixcompany.utils.animation.impl.EaseInOutQuad;
import ru.arixcompany.utils.math.MathUtils;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

public class CustomTitleButton extends AbstractButton {

    private final Animation hoverAnimation = new EaseInOutQuad(200, 1.0);

    public CustomTitleButton(String name, Runnable action) {
        super(name, action);
        hoverAnimation.setDirection(Direction.BACKWARDS);
        hoverAnimation.timerUtil.setTime(System.currentTimeMillis() - 9999);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        boolean hovered = MathUtils.isHovered(mouseX, mouseY, x, y, width, height);

        Direction target = hovered ? Direction.FORWARDS : Direction.BACKWARDS;
        if (hoverAnimation.getDirection() != target) {
            hoverAnimation.setDirection(target);
        }

        float hoverProgress = hoverAnimation.getOutput();

        int bgColor = Colors.bgElement(100);
        int hoverColor = Colors.hoverBg(100, 1);

        int color = ColorUtil.interpolateColor(bgColor, hoverColor, hoverProgress);

        RenderUtils.fillRoundRect(context, x, y, width, height, 5, color);

        FontManager.get(14).drawCenteredString(
                context, name,
                x + width / 2f - 1,
                y + height / 2f - 6,
                -1
        );
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (MathUtils.isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            action.run();
        }
        return false;
    }
}