package ru.arixcompany.ui.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.module.setting.implement.ButtonSetting;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@RequiredArgsConstructor
public final class ButtonSettingComponent implements IComponent {

    @Getter
    private final ButtonSetting setting;

    private static final float HEIGHT = 16.0F;
    private static final float BUTTON_H = 11.0F;
    private static final float PAD = 3.0F;
    private static final float RADIUS = 3.0F;

    @Override
    public float getHeight() {
        return HEIGHT;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        String buttonText = setting.getButtonName() != null && !setting.getButtonName().isEmpty()
                ? setting.getButtonName()
                : setting.getName();

        float buttonX = x + PAD;
        float buttonY = y + (HEIGHT / 2.0F) - (BUTTON_H / 2.0F);
        float buttonW = width - PAD * 2.0F;

        boolean hovered = hovered(mouseX, mouseY, buttonX, buttonY, buttonW, BUTTON_H);

        int buttonColor = hovered
                ? ColorUtil.overCol(bgColor, accentColor, 0.25F)
                : bgColor;

        int textColor = hovered ? textActive : textInactive;

        RenderUtils.fillRoundRect(guiGraphics, buttonX, buttonY, buttonW, BUTTON_H, RADIUS, buttonColor);

        float textX = buttonX + (buttonW - FontManager.get(10).getWidth(buttonText)) / 2.0F;
        float textY = buttonY + (BUTTON_H / 2.0F) - (FontManager.get(10).getHeight() / 2.0F);

        FontManager.get(10).drawString(guiGraphics, buttonText, textX, textY, textColor);
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        float buttonX = x + PAD;
        float buttonY = y + (HEIGHT / 2.0F) - (BUTTON_H / 2.0F);
        float buttonW = width - PAD * 2.0F;

        if (hovered(mouseX, mouseY, buttonX, buttonY, buttonW, BUTTON_H)) {
            if (setting.getRunnable() != null) {
                setting.getRunnable().run();
            }
            return true;
        }

        return false;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}