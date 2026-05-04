package ru.arixcompany.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.clickgui.Gui;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.setting.implement.BooleanSetting;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

@RequiredArgsConstructor
public final class BooleanSettingComponent implements IComponent {

    @Getter private final BooleanSetting setting;

    private static final float TOGGLE_W = 20.0F;
    private static final float TOGGLE_H = 10.0F;

    @Override
    public float getHeight() {
        return 15.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        // Анимация переключения
        Gui.animRun(setting.anim, setting.isValue() ? 1.0 : 0.0);
        float anim = setting.anim.getOutput();
        float thumb_size = 6.0F;
        // Позиция toggle
        float toggleX = x + width - TOGGLE_W - 3.0F;
        float toggleY = y + (getHeight() / 2.0F) - (TOGGLE_H / 2.0F);

        // Трек — всегда тёмный bgColor (одинаковый фон)
        RenderUtils.fillRoundRect(toggleX, toggleY, TOGGLE_W, TOGGLE_H, TOGGLE_H / 2.0F, bgColor);

        // Thumb: выкл = серый (textInactive), вкл = цвет темы (accentColor)
        float thumbX = toggleX + 2.0F + (TOGGLE_W - thumb_size - 4.0F) * anim;
        float thumbY = toggleY + (TOGGLE_H / 2.0F) - (thumb_size / 2.0F);
        int thumbColor = ColorUtil.overCol(textInactive, accentColor, anim);
        RenderUtils.fillRoundRect(thumbX, thumbY, thumb_size, thumb_size, thumb_size / 2.0F, thumbColor);

        // Название настройки
        float textY = y + (getHeight() / 2.0F) - (FontManager.get(10).getHeight() / 2.0F);
        FontManager.get(10).drawString(guiGraphics, setting.getName(), x, textY, textInactive);
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        float toggleX = x + width - TOGGLE_W - 3.0F;
        float toggleY = y + (getHeight() / 2.0F) - (TOGGLE_H / 2.0F);

        if (hovered(mouseX, mouseY, toggleX, toggleY, TOGGLE_W, TOGGLE_H)) {
            setting.setValue(!setting.isValue());
            return true;
        }
        return false;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}