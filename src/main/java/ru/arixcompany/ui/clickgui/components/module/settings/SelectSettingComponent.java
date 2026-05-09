package ru.arixcompany.ui.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.features.module.setting.implement.SelectSetting;
import ru.arixcompany.ui.clickgui.components.IComponent;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.List;

@RequiredArgsConstructor
public final class SelectSettingComponent implements IComponent {

    @Getter
    private final SelectSetting setting;

    private static final float LABEL_H = 10f;
    private static final float BTN_H = 10f;
    private static final float BTN_PAD = 6f;
    private static final float BTN_GAP_X = 6f;
    private static final float BTN_GAP_Y = 4f;

    @Setter
    private float layoutWidth = 118f;

    @Override
    public float getHeight() {
        List<String> list = setting.getList();
        if (list == null || list.isEmpty()) return LABEL_H + 6f;

        float width = layoutWidth;

        float xCursor = 0;
        int rows = 1;

        for (String mode : list) {
            float btnW = FontManager.get(10).getWidth(mode) + BTN_PAD * 2;

            if (btnW > width) {
                rows++;
                xCursor = 0;
                continue;
            }

            if (xCursor + btnW > width) {
                rows++;
                xCursor = 0;
            }

            xCursor += btnW + BTN_GAP_X;
        }

        return LABEL_H + 6f + rows * (BTN_H + BTN_GAP_Y);
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        this.layoutWidth = width;

        FontManager.get(10).drawString(
                guiGraphics,
                setting.getName(),
                x,
                y,
                textInactive
        );

        List<String> list = setting.getList();
        if (list == null || list.isEmpty()) return;

        float xCursor = x;
        float yCursor = y + LABEL_H + 6f;

        for (String mode : list) {
            float btnW = FontManager.get(10).getWidth(mode) + BTN_PAD * 2;

            if (btnW > width) {
                xCursor = x;
                yCursor += BTN_H + BTN_GAP_Y;
            }

            if (xCursor + btnW > x + width) {
                xCursor = x;
                yCursor += BTN_H + BTN_GAP_Y;
            }

            boolean selected = mode.equals(setting.getSelected());
            int rectColor = selected ? accentColor : bgColor;

            RenderUtils.fillRoundRect(
                    xCursor,
                    yCursor,
                    btnW,
                    BTN_H,
                    3f,
                    rectColor
            );

            FontManager.get(10).drawString(
                    guiGraphics,
                    mode,
                    xCursor + BTN_PAD,
                    yCursor,
                    selected ? textActive : textInactive
            );

            xCursor += btnW + BTN_GAP_X;
        }
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {

        this.layoutWidth = width;

        if (button != 0) return false;

        List<String> list = setting.getList();
        if (list == null || list.isEmpty()) return false;

        float xCursor = x;
        float yCursor = y + LABEL_H + 6f;

        for (String mode : list) {
            float btnW = FontManager.get(10).getWidth(mode) + BTN_PAD * 2;

            if (btnW > width) {
                xCursor = x;
                yCursor += BTN_H + BTN_GAP_Y;
            }

            if (xCursor + btnW > x + width) {
                xCursor = x;
                yCursor += BTN_H + BTN_GAP_Y;
            }

            if (hovered(mouseX, mouseY, xCursor, yCursor, btnW, BTN_H)) {
                setting.setSelected(mode);
                return true;
            }

            xCursor += btnW + BTN_GAP_X;
        }

        return false;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx <= x + w && my <= y + h;
    }
}