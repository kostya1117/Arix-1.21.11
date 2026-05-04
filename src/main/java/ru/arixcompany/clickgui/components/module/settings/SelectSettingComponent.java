package ru.arixcompany.clickgui.components.module.settings;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.GuiGraphics;
import ru.arixcompany.clickgui.components.IComponent;
import ru.arixcompany.module.setting.implement.SelectSetting;
import ru.arixcompany.utils.render.ColorUtil;
import ru.arixcompany.utils.render.RenderUtils;
import ru.arixcompany.utils.render.font.FontManager;

import java.util.List;

@RequiredArgsConstructor
public final class SelectSettingComponent implements IComponent {

    @Getter private final SelectSetting setting;

    private static final float ROW_H      = 13.0F;
    private static final float BTN_H      = 10.0F;
    private static final float BTN_PAD    = 6.0F;   // горизонтальный padding внутри кнопки
    private static final float BTN_GAP    = 3.0F;   // отступ между кнопками
    private static final float LABEL_GAP  = 4.0F;   // отступ между label и кнопками

    @Override
    public float getHeight() {
        return ROW_H;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {

        // Название настройки — слева, как у BooleanSetting
        float labelY = y + (ROW_H / 2.0F) - (FontManager.get(10).getHeight() / 2.0F);
        FontManager.get(10).drawString(guiGraphics, setting.getName(), x, labelY, textInactive);

        // Кнопки — справа, идём справа налево
        List<String> list = setting.getList();
        if (list == null || list.isEmpty()) return;

        float btnX = x + width;
        for (int i = list.size() - 1; i >= 0; i--) {
            String mode = list.get(i);
            float btnW = FontManager.get(10).getWidth(mode) + BTN_PAD * 2;
            btnX -= btnW;

            boolean selected = mode.equals(setting.getSelected());
            int bg    = selected ? accentColor : bgColor;
            int text  = selected ? textActive  : textInactive;

            RenderUtils.fillRoundRect(btnX, y + (ROW_H - BTN_H) / 2.0F, btnW, BTN_H, 3.0F, bg);
            RenderUtils.drawRoundRectOutline(btnX, y + (ROW_H - BTN_H) / 2.0F, btnW, BTN_H, 3.0F, 0.5F, outlineColor);

            FontManager.get(10).drawString(guiGraphics, mode,
                    btnX + btnW / 2.0F - FontManager.get(10).getWidth(mode) / 2.0F,
                    y + (ROW_H - BTN_H) / 2.0F,
                    text);

            btnX -= BTN_GAP;
        }
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        if (button != 0) return false;

        List<String> list = setting.getList();
        if (list == null || list.isEmpty()) return false;

        float btnX = x + width;
        for (int i = list.size() - 1; i >= 0; i--) {
            String mode = list.get(i);
            float btnW = FontManager.get(10).getWidth(mode) + BTN_PAD * 2;
            btnX -= btnW;

            if (hovered(mouseX, mouseY, btnX, y + (ROW_H - BTN_H) / 2.0F, btnW, BTN_H)) {
                setting.setSelected(mode);
                return true;
            }

            btnX -= BTN_GAP;
        }
        return false;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}
