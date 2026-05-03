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

    private static final float CHECKBOX_SIZE = 8.0F;

    @Override
    public float getHeight() {
        return 10.0F;
    }

    @Override
    public void render(GuiGraphics guiGraphics, float x, float y, float width,
                       int mouseX, int mouseY,
                       int outlineColor, int accentColor, int bgColor,
                       int textInactive, int textActive, float alpha) {
        float cbX = x + width - CHECKBOX_SIZE - 3.0F;
        float cbY = y + 2.0F;

        Gui.animRun(setting.anim, setting.isValue() ? 1.0 : 0.0);
        float anim = setting.anim.getOutput();

        RenderUtils.drawRoundRectOutline(cbX, cbY, CHECKBOX_SIZE, CHECKBOX_SIZE, 3.0F,0.1F, outlineColor);
        RenderUtils.fillRoundRect(cbX, cbY, CHECKBOX_SIZE, CHECKBOX_SIZE, 3.0F, bgColor);
        RenderUtils.fillRoundRect(cbX + 2.3F, cbY + 2.2F, 3.42F, 3.425F, 3.0F,
                ColorUtil.overCol(0, accentColor, anim));

        FontManager.get(13).drawString(guiGraphics,setting.getName(), x, y + 3.0F + 5.0F,textInactive);
    }

    @Override
    public boolean handleClick(float x, float y, float width,
                               int mouseX, int mouseY, int button) {
        float cbX = x + width - CHECKBOX_SIZE - 3.0F;
        float cbY = y + 2.0F;

        if (button == 0 && hovered(mouseX, mouseY, cbX, cbY, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            setting.setValue(!setting.isValue());
            return true;
        }
        return false;
    }

    private boolean hovered(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }
}